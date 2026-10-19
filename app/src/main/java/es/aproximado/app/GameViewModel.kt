package es.aproximado.app

import android.app.Application
import android.annotation.SuppressLint
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import es.aproximado.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

// PASS is retained only to read saved games from versions before 1.0.4.
enum class Screen { HOME, SETUP, INTRO, PASS, SELECT, REVEAL, SCORE, FINAL, SETTINGS, LAN }
data class Config(val players: List<Player> = (0..1).map { Player(it, "") },val rounds: Int = 10,val categories: Set<String> = emptySet(),val difficulty: Int? = null)
data class Match(val config: Config,val questions: List<String>,val round: Int = 0,val guesses: List<List<Double>> = listOf(emptyList()),val stage: Screen = Screen.INTRO) {
    val currentGuesses get() = guesses.getOrElse(round) { emptyList() }
    val turn get() = currentGuesses.size.coerceAtMost(config.players.lastIndex)
}
data class AppState(val screen: Screen = Screen.HOME,val pool: List<Question> = emptyList(),val config: Config = Config(),val match: Match? = null,val recent: List<String> = emptyList(),val sound: Boolean = true,val vibration: Boolean = true,val loading: Boolean = true,val error: Boolean = false,val savedQuestions: List<Question> = emptyList(),val liveRuler: Boolean = true)

class GameViewModel(app: Application): AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("aproximado",0)
    private val mutable = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val pool = withContext(Dispatchers.IO) { QuestionsDb.get(app).questions().all().map { it.model() } }
                val restored = runCatching { restore(prefs.getString("state",null)) }.getOrNull() ?: AppState()
                val archived = withContext(Dispatchers.IO) {
                    val missing = restored.match?.questions.orEmpty().filterNot { id -> pool.any { it.id==id } }
                    if(missing.isEmpty()) emptyList() else QuestionsDb.get(app).questions().findByIds(missing).map { it.model() }
                }
                val available=(pool+archived).map { it.id }.toSet()
                val match=restored.match?.takeIf { available.containsAll(it.questions) }?.let {
                    if(it.stage==Screen.PASS) it.copy(stage=Screen.SELECT) else it
                }
                val categories=restored.config.categories.intersect(pool.map { it.category }.toSet())
                val history=QuestionHistory.importLegacy(app,restored.recent)
                mutable.value = restored.copy(recent=history,pool=pool,savedQuestions=archived,match=match,config=restored.config.copy(categories=categories),loading=false,screen=Screen.HOME)
            } catch (_: Exception) { mutable.update { it.copy(loading=false,error=true) } }
        }
    }
    private fun update(block: (AppState)->AppState) { mutable.update(block); persist() }
    fun screen(screen: Screen) = update { it.copy(screen=screen) }
    fun configure(block: (Config)->Config) = update { it.copy(config=block(it.config)) }
    fun count(n: Int) = configure { c ->
        val count = n.coerceIn(2,8)
        val ps = c.players.take(count).toMutableList()
        while(ps.size < count) { val id=ps.size;ps.add(Player(id,"",(0..7).first { style -> ps.none { it.style==style } })) }
        c.copy(players=ps)
    }
    fun name(id: Int,value: String) = configure { c -> c.copy(players=c.players.map { if(it.id==id) it.copy(name=value.take(12)) else it }) }
    fun color(id: Int,style: Int) = configure { c -> if(c.players.any { it.id!=id && it.style==style }) c else c.copy(players=c.players.map { if(it.id==id) it.copy(style=style) else it }) }
    fun start(again: Boolean = false) {
        val s=mutable.value
        val raw=if(again) s.match?.config ?: s.config else s.config
        val config=raw.copy(categories=raw.categories.intersect(s.pool.map { it.category }.toSet()),players=raw.players.map { if(it.name.isBlank()) it.copy(name=getApplication<Application>().getString(R.string.player,it.id+1)) else it })
        val selected=runCatching { Selection.select(s.pool,config.rounds,config.categories,config.difficulty,QuestionHistory.read(getApplication())) }.getOrNull() ?: return
        val history=QuestionHistory.remember(getApplication(),listOf(selected.first().id,Selection.fingerprint(selected.first().text)))
        update { it.copy(config=config,match=Match(config,selected.map { q->q.id }),savedQuestions=emptyList(),screen=Screen.INTRO,recent=history) }
    }
    fun resume() {
        val m=mutable.value.match ?: return
        if(m.stage in listOf(Screen.INTRO,Screen.PASS,Screen.SELECT)) mutable.value.question()?.let { q ->
            val history=QuestionHistory.remember(getApplication(),listOf(q.id,Selection.fingerprint(q.text)))
            mutable.update { it.copy(recent=history) }
        }
        advance(if(m.stage==Screen.PASS) Screen.SELECT else m.stage)
    }
    fun advance(stage: Screen) = update {
        val next=if(stage==Screen.PASS) Screen.SELECT else stage
        it.copy(screen=next,match=it.match?.copy(stage=next))
    }
    fun confirm(value: Double,expectedRound: Int? = null,expectedTurn: Int? = null) {
        val s=mutable.value;val m=s.match ?: return
        if(s.screen!=Screen.SELECT || m.currentGuesses.size>=m.config.players.size) return
        if(expectedRound!=null && expectedRound!=m.round || expectedTurn!=null && expectedTurn!=m.turn) return
        val q=s.question() ?: return
        if(!EstimateInput.accepts(q,value)) return
        val guesses=m.currentGuesses+value
        val stage=if(guesses.size==m.config.players.size) Screen.REVEAL else Screen.SELECT
        val all=m.guesses.toMutableList().apply { this[m.round]=guesses }
        update { it.copy(match=m.copy(guesses=all,stage=stage),screen=stage) }
    }
    fun nextRound() {
        val m=mutable.value.match ?: return
        if(m.round+1>=m.config.rounds) advance(Screen.FINAL)
        else {
            val next=(mutable.value.pool+mutable.value.savedQuestions).first { it.id==m.questions[m.round+1] }
            val history=QuestionHistory.remember(getApplication(),listOf(next.id,Selection.fingerprint(next.text)))
            update { it.copy(screen=Screen.INTRO,match=m.copy(round=m.round+1,guesses=m.guesses+listOf(emptyList()),stage=Screen.INTRO),recent=history) }
        }
    }
    fun liveRuler(enabled: Boolean) = update { it.copy(liveRuler=enabled) }
    fun setting(sound: Boolean? = null,vibration: Boolean? = null) = update { it.copy(sound=sound ?: it.sound,vibration=vibration ?: it.vibration) }
    fun clear() { QuestionHistory.clear(getApplication());update { it.copy(match=null,recent=emptyList(),savedQuestions=emptyList()) } }
    private fun configJson(c: Config)=JSONObject().put("players",JSONArray().apply { c.players.forEach { put(JSONObject().put("id",it.id).put("name",it.name).put("style",it.style)) } }).put("rounds",c.rounds).put("categories",JSONArray(c.categories.toList())).put("difficulty",c.difficulty ?: 0)
    private fun parseConfig(o: JSONObject): Config {
        val players=o.getJSONArray("players").let { a -> (0 until a.length()).map { val p=a.getJSONObject(it);Player(p.getInt("id"),p.getString("name"),p.getInt("style")) } }
        require(players.size in 2..8 && players.map { it.style }.distinct().size==players.size && players.all { it.style in 0..7 })
        return Config(players,o.getInt("rounds"),o.getJSONArray("categories").strings().toSet(),o.getInt("difficulty").takeIf { it in 1..3 })
    }
    @SuppressLint("ApplySharedPref") // Synchronous checkpoint only on discrete actions, never on drag.
    private fun persist() {
        val s=mutable.value
        if(s.loading) return
        val obj=JSONObject().put("config",configJson(s.config)).put("recent",JSONArray(s.recent)).put("sound",s.sound).put("vibration",s.vibration).put("liveRuler",s.liveRuler)
        s.match?.let { m -> obj.put("match",JSONObject().put("config",configJson(m.config)).put("questions",JSONArray(m.questions)).put("round",m.round).put("stage",m.stage.name).put("guesses",JSONArray().apply { m.guesses.forEach { put(JSONArray(it)) } })) }
        // Checkpoint the current turn on discrete actions, including direct player changes.
        prefs.edit(commit=true) { putString("state",obj.toString()) }
    }
    private fun restore(raw: String?): AppState {
        if(raw==null) return AppState()
        val o=JSONObject(raw)
        val match=o.optJSONObject("match")?.let { m ->
            val a=m.getJSONArray("guesses")
            Match(parseConfig(m.getJSONObject("config")),m.getJSONArray("questions").strings(),m.getInt("round"),(0 until a.length()).map { i -> val r=a.getJSONArray(i);(0 until r.length()).map { r.getDouble(it) } },Screen.valueOf(m.getString("stage")))
        }
        if(match!=null) require(match.questions.size==match.config.rounds && match.round in match.questions.indices && match.guesses.size==match.round+1)
        return AppState(config=parseConfig(o.getJSONObject("config")),match=match,recent=o.getJSONArray("recent").strings(),sound=o.optBoolean("sound",true),vibration=o.optBoolean("vibration",true),liveRuler=o.optBoolean("liveRuler",true))
    }
}
private fun JSONArray.strings()=(0 until length()).map { getString(it) }

fun AppState.question(): Question? = match?.let { m -> (pool+savedQuestions).firstOrNull { it.id==m.questions[m.round] } }
fun AppState.scores(): List<List<RoundScore>> {
    val m=match ?: return emptyList()
    return m.guesses.mapIndexedNotNull { index,guesses -> if(guesses.size==m.config.players.size) (pool+savedQuestions).firstOrNull { it.id==m.questions[index] }?.let { Scoring.round(it,guesses) } else null }
}
