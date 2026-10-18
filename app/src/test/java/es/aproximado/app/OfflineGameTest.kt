@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package es.aproximado.app

import android.app.Application
import es.aproximado.core.Scoring
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class OfflineGameTest {
    private lateinit var app: Application
    @Before fun prepare() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        app=RuntimeEnvironment.getApplication()
        app.getSharedPreferences("aproximado",0).edit().clear().commit()
    }
    @After fun cleanup() { Dispatchers.resetMain() }
    private suspend fun loaded(): GameViewModel {
        val vm=GameViewModel(app)
        withTimeout(20000) { vm.state.first { !it.loading } }
        assertFalse("Room asset failed to load",vm.state.value.error)
        assertEquals(400,vm.state.value.pool.size)
        return vm
    }
    @Test fun actualRoomAssetAndCompleteTwoAndEightPlayerMatches()=runBlocking {
        for(n in listOf(2,8)) {
            val vm=loaded();vm.clear();vm.count(n);vm.configure { it.copy(rounds=5) };vm.start()
            assertEquals(Screen.INTRO,vm.state.value.screen)
            val ids=vm.state.value.match!!.questions
            assertEquals(5,ids.toSet().size)
            repeat(5) { round ->
                vm.advance(Screen.SELECT)
                repeat(n) { turn ->
                    assertEquals(Screen.SELECT,vm.state.value.screen)
                    assertEquals(turn,vm.state.value.match!!.turn)
                    vm.advance(Screen.SELECT)
                    val q=vm.state.value.question()!!
                    vm.confirm(es.aproximado.core.EstimateInput.round(q,(q.answer+turn).coerceIn(q.min,q.max)))
                }
                assertEquals(Screen.REVEAL,vm.state.value.screen)
                assertEquals(n,vm.state.value.scores().last().size)
                vm.advance(Screen.SCORE);vm.nextRound()
                assertEquals(if(round==4) Screen.FINAL else Screen.INTRO,vm.state.value.screen)
            }
            assertEquals(5,vm.state.value.scores().size)
            assertEquals(n,Scoring.standings(vm.state.value.match!!.config.players,vm.state.value.scores()).size)
            assertTrue(vm.state.value.recent.containsAll(ids))
        }
    }
    @Test fun processDeathRestoresTheNextPlayerDirectly()=runBlocking {
        val first=loaded();first.start();first.advance(Screen.SELECT);first.advance(Screen.SELECT)
        val answer=first.state.value.question()!!.answer
        first.confirm(answer);first.advance(Screen.SELECT)
        val restored=loaded()
        assertEquals(Screen.HOME,restored.state.value.screen)
        restored.resume()
        assertEquals(Screen.SELECT,restored.state.value.screen)
        assertEquals(listOf(answer),restored.state.value.match!!.currentGuesses)
        assertEquals(1,restored.state.value.match!!.turn)
    }
    @Test fun liveRulerConsentSurvivesProcessDeathAndDefaultsOnForOldSaves()=runBlocking {
        val vm=loaded();assertTrue(vm.state.value.liveRuler)
        vm.liveRuler(false);assertFalse(loaded().state.value.liveRuler)
        vm.liveRuler(true);assertTrue(loaded().state.value.liveRuler)
        val prefs=app.getSharedPreferences("aproximado",0)
        val saved=org.json.JSONObject(prefs.getString("state",null)!!).apply { remove("liveRuler") }
        prefs.edit().putString("state",saved.toString()).commit()
        assertTrue(loaded().state.value.liveRuler)
    }
    @Test fun configBoundsAndUniqueColors()=runBlocking {
        val vm=loaded();vm.count(99)
        assertEquals(8,vm.state.value.config.players.size)
        assertEquals(8,vm.state.value.config.players.map { it.style }.distinct().size)
        vm.name(0,"Nombre demasiado largo")
        assertEquals(12,vm.state.value.config.players[0].name.length)
        vm.color(0,1)
        assertEquals(0,vm.state.value.config.players[0].style)
        vm.count(1);assertEquals(2,vm.state.value.config.players.size)
    }
    @Test fun corruptedSavedMatchDoesNotCrashStartup()=runBlocking {
        app.getSharedPreferences("aproximado",0).edit().putString("state","{invalid").commit()
        assertNull(loaded().state.value.match)
    }
    @Test fun precisionMetadataAndIntegerKeyboardAreEnforcedByTheViewModel()=runBlocking {
        val initial=loaded()
        val zodiac=initial.state.value.pool.first { it.id=="v104_cultura_001" }
        assertEquals(0,zodiac.decimalPlaces)
        assertEquals(350,initial.state.value.pool.count { it.decimalPlaces==0 })
        assertEquals(50,initial.state.value.pool.count { it.decimalPlaces>0 })
        initial.configure { it.copy(rounds=5) };initial.start()
        val prefs=app.getSharedPreferences("aproximado",0)
        val state=org.json.JSONObject(prefs.getString("state",null)!!)
        state.getJSONObject("match").put("questions",org.json.JSONArray(listOf(zodiac.id)+initial.state.value.pool.filter { it.id!=zodiac.id }.take(4).map { it.id })).put("stage","PASS")
        prefs.edit().putString("state",state.toString()).commit()
        val restored=loaded();restored.resume();restored.advance(Screen.SELECT)
        assertEquals(zodiac,restored.state.value.question())
        restored.confirm(12.5)
        assertEquals(Screen.SELECT,restored.state.value.screen)
        assertTrue(restored.state.value.match!!.currentGuesses.isEmpty())
        restored.confirm(12.0)
        assertEquals(Screen.SELECT,restored.state.value.screen)
        assertEquals(listOf(12.0),restored.state.value.match!!.currentGuesses)
    }
    @Test fun installedDatabasesKeepArchivedQuestionsButSelectOnlyTheNewCatalog()=runBlocking {
        val fresh=loaded().state.value.pool
        for(version in listOf(1,2)) {
            val name="questions-migration-$version.db"
            app.deleteDatabase(name)
            val file=app.getDatabasePath(name);file.parentFile!!.mkdirs()
            javaClass.getResourceAsStream("/questions-v$version.db")!!.use { input -> file.outputStream().use { input.copyTo(it) } }
            val db=androidx.room.Room.databaseBuilder(app,QuestionsDb::class.java,name)
                .addMigrations(QuestionsDb.precisionMigration(app),QuestionsDb.catalogMigration(app)).build()
            try {
                assertEquals(fresh,db.questions().all().map { it.model() })
                val old=db.questions().findByIds(listOf("cultura_035","deportes_004","curiosidades_013")).map { it.model() }
                assertEquals(3,old.size)
                assertEquals(0,old.first { it.id=="cultura_035" }.decimalPlaces)
                assertEquals(2,old.first { it.id=="deportes_004" }.decimalPlaces)
                assertEquals(3,old.first { it.id=="curiosidades_013" }.decimalPlaces)
                val saved=AppState(pool=fresh,savedQuestions=old,match=Match(Config(rounds=5),List(5){"cultura_035"},guesses=listOf(listOf(12.0,13.0))))
                assertEquals(12.0,saved.question()!!.answer,0.0)
                assertEquals(2,saved.scores().single().size)
                val cursor=db.openHelper.readableDatabase.query("SELECT count(*) FROM questions")
                cursor.use { assertTrue(it.moveToFirst());assertEquals(830,it.getInt(0)) }
            } finally { db.close();app.deleteDatabase(name) }
        }
    }
    @Test fun staleConfirmationCannotSubmitForTheNextPlayer()=runBlocking {
        val vm=loaded();vm.count(8);vm.start();vm.advance(Screen.SELECT)
        val q=vm.state.value.question()!!
        vm.confirm(q.answer,0,0)
        assertEquals(Screen.SELECT,vm.state.value.screen)
        assertEquals(1,vm.state.value.match!!.turn)
        vm.confirm(q.answer,0,0)
        assertEquals(listOf(q.answer),vm.state.value.match!!.currentGuesses)
        vm.confirm(q.answer,0,1)
        assertEquals(2,vm.state.value.match!!.turn)
    }
    @Test fun referenceFiguresKeepPublishedPrecisionAndLegacyPassRestoresAsSelector()=runBlocking {
        val vm=loaded();val pool=vm.state.value.pool
        assertEquals(8848.86,pool.first { it.id=="v104_geografia_001" }.answer,0.0)
        assertEquals(3682.0,pool.first { it.id=="v104_geografia_002" }.answer,0.0)
        val gravity=pool.first { it.id=="v104_ciencia_018" }
        assertEquals(5,gravity.decimalPlaces)
        assertTrue(es.aproximado.core.EstimateInput.accepts(gravity,9.80665))
        vm.start()
        val prefs=app.getSharedPreferences("aproximado",0)
        val json=org.json.JSONObject(prefs.getString("state",null)!!)
        json.getJSONObject("match").put("stage","PASS")
        prefs.edit().putString("state",json.toString()).commit()
        val restored=loaded();restored.resume()
        assertEquals(Screen.SELECT,restored.state.value.screen)
        assertTrue(restored.state.value.match!!.currentGuesses.isEmpty())
    }
}
