package es.aproximado.core.lan

import es.aproximado.core.*
import java.util.UUID

enum class LanStage { LOBBY, ANSWERING, REVEAL, SCORE, FINISHED }
enum class LanFailure { WRONG_CODE, FULL, STARTED, INVALID, DISCONNECTED, CLOSED, VERSION, NETWORK, TIMEOUT, ROOM_CHANGED }
class LanException(val reason: LanFailure): Exception(reason.name)
data class LanMember(val id: Int,val name: String,val online: Boolean,val submitted: Boolean = false)
/** No answer, source, catalog ID or future question is included in a prompt. */
data class LanPrompt(val id: String,val category: String,val text: String,val unit: String?,val min: Double,val max: Double,val scale: String,val difficulty: Int,val year: Int?,val decimals: Int) {
    fun model(answer: Double = 0.0,source: String = "") = Question(id,category,text,answer,unit,min,max,scale,difficulty,year,source,decimals)
    companion object { fun of(id: String,q: Question) = LanPrompt(id,q.category,q.text,q.unit,q.min,q.max,q.scale,q.difficulty,q.year,q.decimalPlaces) }
}
data class LanRound(val prompt: LanPrompt,val answer: Double,val source: String,val guesses: List<Double>)
data class LanSnapshot(val roomId: String,val gameId: String,val revision: Long,val self: Int,val stage: LanStage,val members: List<LanMember>,val rounds: Int,val difficulty: Int?,val categories: Set<String>,val round: Int,val prompt: LanPrompt?,val mine: Double?,val results: List<LanRound>,val liveEnabled: Boolean = false)

/** Transient estimates only. Never persisted or used for scoring. */
data class LanLiveFrame(val game: String,val round: Int,val revision: Long,val values: Map<Int,Double>) {
    fun accepts(s: LanSnapshot): Boolean = s.liveEnabled && s.stage==LanStage.ANSWERING && game==s.gameId && round==s.round && revision==s.revision && values.size<=2 && values.all { (id,v) ->
        id!=s.self && s.members.any { it.id==id && it.online && !it.submitted } && s.prompt?.let { EstimateInput.accepts(it.model(),v) }==true
    }
}

/** Host-owned state. Tokens are never included in snapshots. All changes are serialized by the server. */
class LanRoom(private val pool: List<Question>,hostToken: String,hostName: String,private val history: MutableList<String> = mutableListOf(),hostLiveRuler: Boolean = true) {
    private data class Seat(val token: String,val name: String,var online: Boolean,var live: Boolean)
    val id: String = UUID.randomUUID().toString()
    private var gameId = ""
    private val seats=mutableListOf(Seat(hostToken,cleanName(hostName),true,hostLiveRuler))
    private var revision=0L
    var stage=LanStage.LOBBY; private set
    private var rounds=10
    private var difficulty: Int?=null
    private var categories=emptySet<String>()
    private var questions=emptyList<Question>()
    private var round=0
    private val guesses=mutableMapOf<String,Double>()
    private val results=mutableListOf<LanRound>()
    private val drafts=mutableMapOf<String,Double>()
    private fun liveEnabled()=seats.size in 2..3 && seats.all { it.online && it.live }
    @Synchronized fun join(token: String,name: String,liveRuler: Boolean = true): Int {
        requireToken(token)
        val old=seats.indexOfFirst { it.token==token }
        if(old>=0) { seats[old].online=true;seats[old].live=liveRuler;drafts.clear();revision++;return old }
        if(stage!=LanStage.LOBBY) throw LanException(LanFailure.STARTED)
        if(seats.size>=8) throw LanException(LanFailure.FULL)
        seats.add(Seat(token,cleanName(name),true,liveRuler));drafts.clear();revision++;return seats.lastIndex
    }
    @Synchronized fun offline(token: String) { seats.firstOrNull { it.token==token }?.let { if(it.online) { it.online=false;drafts.clear();revision++ } } }
    @Synchronized fun remove(index: Int) {
        if(stage!=LanStage.LOBBY || index !in 1..seats.lastIndex) throw LanException(LanFailure.INVALID)
        seats.removeAt(index);drafts.clear();revision++
    }
    @Synchronized fun configure(rounds: Int,difficulty: Int?,categories: Set<String>) {
        if(stage!=LanStage.LOBBY || rounds !in listOf(5,10,15,20) || difficulty!=null && difficulty !in 1..3 || !pool.map { it.category }.containsAll(categories)) throw LanException(LanFailure.INVALID)
        this.rounds=rounds;this.difficulty=difficulty;this.categories=categories.toSet();revision++
    }
    @Synchronized fun start() {
        if(stage!=LanStage.LOBBY || seats.size !in 2..8 || seats.any { !it.online }) throw LanException(LanFailure.INVALID)
        questions=try { Selection.select(pool,rounds,categories,difficulty,history) } catch (_: IllegalArgumentException) { throw LanException(LanFailure.INVALID) }
        gameId=UUID.randomUUID().toString();round=0;guesses.clear();drafts.clear();results.clear();stage=LanStage.ANSWERING;revision++
    }
    @Synchronized fun submit(token: String,game: String,round: Int,value: Double) {
        if(stage!=LanStage.ANSWERING || game!=gameId || round!=this.round || seats.none { it.token==token } || !EstimateInput.accepts(questions[round],value)) throw LanException(LanFailure.INVALID)
        // Retried commands are idempotent. A confirmed value cannot be replaced.
        if(guesses.containsKey(token)) return
        drafts.remove(token);guesses[token]=value;revision++
        if(guesses.size==seats.size) {
            val q=questions[round]
            results.add(LanRound(LanPrompt.of("$gameId:$round",q),q.answer,q.source,seats.map { guesses.getValue(it.token) }))
            val remembered=Selection.remember(history,listOf(q.id));history.clear();history.addAll(remembered)
            stage=LanStage.REVEAL
        }
    }
    @Synchronized fun advance() {
        when(stage) {
            LanStage.REVEAL -> stage=LanStage.SCORE
            LanStage.SCORE -> if(round+1==rounds) stage=LanStage.FINISHED else { round++;guesses.clear();stage=LanStage.ANSWERING }
            else -> throw LanException(LanFailure.INVALID)
        };drafts.clear();revision++
    }
    @Synchronized fun lobby() { questions=emptyList();results.clear();guesses.clear();drafts.clear();round=0;gameId="";stage=LanStage.LOBBY;revision++ }
    @Synchronized fun snapshot(token: String): LanSnapshot {
        val self=seats.indexOfFirst { it.token==token };if(self<0) throw LanException(LanFailure.CLOSED)
        return LanSnapshot(id,gameId,revision,self,stage,seats.mapIndexed { i,s -> LanMember(i,s.name,s.online,s.token in guesses) },rounds,difficulty,categories,round,
            questions.getOrNull(round)?.let { LanPrompt.of("$gameId:$round",it) },guesses[token],results.toList(),liveEnabled())
    }
    @Synchronized fun preference(token: String,enabled: Boolean) {
        val seat=seats.firstOrNull { it.token==token } ?: throw LanException(LanFailure.INVALID)
        if(seat.live!=enabled) { seat.live=enabled;drafts.clear();revision++ }
    }
    @Synchronized fun preview(token: String,game: String,round: Int,revision: Long,value: Double): Boolean {
        if(!liveEnabled() || stage!=LanStage.ANSWERING || game!=gameId || round!=this.round || revision!=this.revision || token in guesses || seats.none { it.token==token } || !EstimateInput.accepts(questions[round],value)) return false
        if(drafts[token]==value) return false
        drafts[token]=value;return true
    }
    @Synchronized fun live(token: String): LanLiveFrame {
        val values=if(liveEnabled() && stage==LanStage.ANSWERING) seats.mapIndexedNotNull { i,s -> drafts[s.token]?.takeIf { s.token!=token && s.token !in guesses }?.let { i to it } }.toMap() else emptyMap()
        return LanLiveFrame(gameId,round,revision,values)
    }
    companion object {
        fun cleanName(name: String): String = name.filterNot { it.isISOControl() }.trim().take(12).also { if(it.isBlank()) throw LanException(LanFailure.INVALID) }
        fun requireToken(token: String) { if(!Regex("[a-zA-Z0-9-]{32,64}").matches(token)) throw LanException(LanFailure.INVALID) }
    }
}
