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
        assertEquals(430,vm.state.value.pool.size)
        return vm
    }
    @Test fun actualRoomAssetAndCompleteTwoAndEightPlayerMatches()=runBlocking {
        for(n in listOf(2,8)) {
            val vm=loaded();vm.clear();vm.count(n);vm.configure { it.copy(rounds=5) };vm.start()
            assertEquals(Screen.INTRO,vm.state.value.screen)
            val ids=vm.state.value.match!!.questions
            assertEquals(5,ids.toSet().size)
            repeat(5) { round ->
                vm.advance(Screen.PASS)
                repeat(n) { turn ->
                    assertEquals(Screen.PASS,vm.state.value.screen)
                    assertEquals(turn,vm.state.value.match!!.turn)
                    vm.advance(Screen.SELECT)
                    val q=vm.state.value.question()!!
                    vm.confirm((q.answer+turn).coerceIn(q.min,q.max))
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
    @Test fun processDeathRestoresConfirmedGuessesBehindPrivateHandoff()=runBlocking {
        val first=loaded();first.start();first.advance(Screen.PASS);first.advance(Screen.SELECT)
        val answer=first.state.value.question()!!.answer
        first.confirm(answer);first.advance(Screen.SELECT)
        val restored=loaded()
        assertEquals(Screen.HOME,restored.state.value.screen)
        restored.resume()
        assertEquals(Screen.PASS,restored.state.value.screen)
        assertEquals(listOf(answer),restored.state.value.match!!.currentGuesses)
        assertEquals(1,restored.state.value.match!!.turn)
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
}
