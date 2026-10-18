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

enum class Screen { HOME, SETUP, INTRO, PASS, SELECT, REVEAL, SCORE, FINAL, SETTINGS }
data class Config(val players: List<Player> = (0..1).map { Player(it, "") },val rounds: Int = 10,val categories: Set<String> = emptySet(),val difficulty: Int? = null)
data class Match(val config: Config,val questions: List<String>,val round: Int = 0,val guesses: List<List<Double>> = listOf(emptyList()),val stage: Screen = Screen.INTRO) {
    val currentGuesses get() = guesses.getOrElse(round) { emptyList() }
    val turn get() = currentGuesses.size.coerceAtMost(config.players.lastIndex)
}
data class AppState(val screen: Screen = Screen.HOME,val pool: List<Question> = emptyList(),val config: Config = Config(),val match: Match? = null,val recent: List<String> = emptyList(),val sound: Boolean = true,val vibration: Boolean = true,val loading: Boolean = true,val error: Boolean = false)

class GameViewModel(app: Application): AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("aproximado",0)
    private val mutable = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = mutable.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val pool = withContext(Dispatchers.IO) { QuestionsDb.get(app).questions().all().map { it.model() } }
                val restored = runCatching { restore(prefs.getString("state",null)) }.getOrNull() ?: AppState()
                mutable.value = restored.copy(pool=pool,loading=false,screen=Screen.HOME)
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
        val config=raw.copy(players=raw.players.map { if(it.name.isBlank()) it.copy(name=getApplication<Application>().getString(R.string.player,it.id+1)) else it })
        val selected=runCatching { Selection.select(s.pool,config.rounds,config.categories,config.difficulty,s.recent) }.getOrNull() ?: return
        update { it.copy(config=config,match=Match(config,selected.map { q->q.id }),screen=Screen.INTRO) }
    }
    fun resume() { val m=mutable.value.match ?: return;screen(if(m.stage==Screen.SELECT) Screen.PASS else m.stage) }
    fun advance(stage: Screen) = update { it.copy(screen=stage,match=it.match?.copy(stage=stage)) }
    fun confirm(value: Double) {
        val s=mutable.value;val m=s.match ?: return
        if(s.screen!=Screen.SELECT || m.currentGuesses.size>=m.config.players.size) return
        val q=s.pool.first { it.id==m.questions[m.round] }
        if(!EstimateInput.accepts(q,value)) return
        val guesses=m.currentGuesses+value
        val stage=if(guesses.size==m.config.players.size) Screen.REVEAL else Screen.PASS
        val all=m.guesses.toMutableList().apply { this[m.round]=guesses }
        update { it.copy(match=m.copy(guesses=all,stage=stage),screen=stage,recent=if(stage==Screen.REVEAL) Selection.remember(it.recent,listOf(q.id)) else it.recent) }
    }
    fun nextRound() {
        val m=mutable.value.match ?: return
        if(m.round+1>=m.config.rounds) advance(Screen.FINAL)
        else update { it.copy(screen=Screen.INTRO,match=m.copy(round=m.round+1,guesses=m.guesses+listOf(emptyList()),stage=Screen.INTRO)) }
    }
    fun setting(sound: Boolean? = null,vibration: Boolean? = null) = update { it.copy(sound=sound ?: it.sound,vibration=vibration ?: it.vibration) }
    fun clear() = update { it.copy(match=null,recent=emptyList()) }
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
        val obj=JSONObject().put("config",configJson(s.config)).put("recent",JSONArray(s.recent)).put("sound",s.sound).put("vibration",s.vibration)
        s.match?.let { m -> obj.put("match",JSONObject().put("config",configJson(m.config)).put("questions",JSONArray(m.questions)).put("round",m.round).put("stage",(if(m.stage==Screen.SELECT) Screen.PASS else m.stage).name).put("guesses",JSONArray().apply { m.guesses.forEach { put(JSONArray(it)) } })) }
        // commit ensures the secret handoff survives immediate process termination; payload is only a few KB.
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
        return AppState(config=parseConfig(o.getJSONObject("config")),match=match,recent=o.getJSONArray("recent").strings(),sound=o.optBoolean("sound",true),vibration=o.optBoolean("vibration",true))
    }
}
private fun JSONArray.strings()=(0 until length()).map { getString(it) }

fun AppState.question(): Question? = match?.let { m -> pool.firstOrNull { it.id==m.questions[m.round] } }
fun AppState.scores(): List<List<RoundScore>> {
    val m=match ?: return emptyList()
    return m.guesses.mapIndexedNotNull { index,guesses -> if(guesses.size==m.config.players.size) pool.firstOrNull { it.id==m.questions[index] }?.let { Scoring.round(it,guesses) } else null }
}
