package es.aproximado.app.lan

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import es.aproximado.app.QuestionHistory
import es.aproximado.core.Question
import es.aproximado.core.Selection
import es.aproximado.core.EstimateInput
import es.aproximado.core.lan.*
import kotlinx.coroutines.flow.*
import java.net.*
import java.util.UUID
import java.util.concurrent.Executors

data class FoundRoom(val name: String,val endpoint: String)
data class LanUiState(val name: String="",val endpoint: String="",val code: String="",val active: Boolean=false,val host: Boolean=false,val connection: LanConnection=LanConnection.CLOSED,val snapshot: LanSnapshot?=null,val addresses: List<String> = emptyList(),val rooms: List<FoundRoom> = emptyList(),val error: LanFailure?=null,val pending: Double?=null,val browsing: Boolean=false,val liveRuler: Boolean=true,val liveValues: Map<Int,Double> = emptyMap())
private data class PendingGuess(val game: String,val round: Int,val value: Double)

/** Process-scoped session: Activity recreation never owns or closes sockets. */
class LanRepository private constructor(private val app: Application) {
    private val prefs=app.getSharedPreferences("aproximado-lan",Context.MODE_PRIVATE)
    private val serviceIntent=Intent(app,LanService::class.java)
    private val main=Handler(Looper.getMainLooper())
    private val worker=Executors.newSingleThreadExecutor { r -> Thread(r,"lan-setup").apply { isDaemon=true } }
    private val token=prefs.getString("token",null) ?: UUID.randomUUID().toString().also { prefs.edit { putString("token",it) } }
    private val mutable=MutableStateFlow(LanUiState(name=prefs.getString("name","")!!,endpoint=prefs.getString("endpoint","")!!,code=prefs.getString("code","")!!,liveRuler=prefs.getBoolean("liveRuler",true)))
    val state: StateFlow<LanUiState> = mutable.asStateFlow()
    private var generation=0
    private var server: LanServer?=null
    private var client: LanClient?=null
    private var pending: PendingGuess?=null
    private var rememberedPrompt: String?=null
    private val nsd=app.getSystemService(NsdManager::class.java)
    private var registration: NsdManager.RegistrationListener?=null
    private var discovery: NsdManager.DiscoveryListener?=null
    private var multicast: WifiManager.MulticastLock?=null
    private val resolveQueue=ArrayDeque<NsdServiceInfo>()
    private var resolving=false
    fun name(value: String) { mutable.update { it.copy(name=value.filterNot(Char::isISOControl).take(12),error=null) } }
    fun endpoint(value: String) { mutable.update { it.copy(endpoint=value.take(32),error=null) } }
    fun code(value: String) { mutable.update { it.copy(code=value.filter(Char::isDigit).take(8),error=null) } }
    private fun foreground(): Boolean = try {
        ContextCompat.startForegroundService(app,serviceIntent.putExtra("generation",generation));true
    } catch (_: Exception) { mutable.update { it.copy(active=false,error=LanFailure.NETWORK) };false }
    fun host(pool: List<Question>) {
        if(mutable.value.active || pool.isEmpty()) return
        val name=runCatching { LanRoom.cleanName(mutable.value.name) }.getOrElse { mutable.update { it.copy(error=LanFailure.INVALID) };return }
        stopBrowsing();val gen=++generation
        mutable.update { it.copy(active=true,host=true,snapshot=null,addresses=emptyList(),pending=null,error=null,connection=LanConnection.CONNECTING) }
        if(!foreground()) return
        prefs.edit { putString("name",name) }
        val liveRuler=mutable.value.liveRuler
        worker.execute {
            try {
                val room=LanServer(pool,name,onState={ s -> main.post { if(gen==generation) receive(s) } },onFailure={ e -> main.post { if(gen==generation) mutable.update { it.copy(error=e,pending=null) } } },hostLiveRuler=liveRuler,onLive={ f -> main.post { if(gen==generation) receiveLive(f) } },initialHistory=QuestionHistory.read(app),onQuestionShown={ id -> pool.firstOrNull { it.id==id }?.let { QuestionHistory.remember(app,listOf(id,Selection.fingerprint(it.text))) };Unit })
                main.post {
                    if(gen!=generation) { room.close();return@post }
                    server=room;mutable.update { it.copy(code=room.code,connection=LanConnection.CONNECTED,addresses=addresses(room.port)) };register(room.port,name)
                }
            } catch (_: Exception) { main.post { if(gen==generation) { mutable.update { it.copy(error=LanFailure.NETWORK) };close() } } }
        }
    }
    fun join() {
        if(mutable.value.active) return
        val s=mutable.value
        val endpoint=runCatching { LanEndpoint.parse(s.endpoint) }.getOrElse { mutable.update { it.copy(error=LanFailure.INVALID) };return }
        val name=runCatching { LanRoom.cleanName(s.name) }.getOrElse { mutable.update { it.copy(error=LanFailure.INVALID) };return }
        if(!s.code.matches(Regex("[0-9]{8}"))) { mutable.update { it.copy(error=LanFailure.INVALID) };return }
        stopBrowsing();val gen=++generation
        pending=if(prefs.contains("pending_value")) PendingGuess(prefs.getString("pending_game","")!!,prefs.getInt("pending_round",0),java.lang.Double.longBitsToDouble(prefs.getLong("pending_value",0))) else null
        val expected=if(prefs.getString("endpoint",null)==endpoint.toString() && prefs.getString("code",null)==s.code) prefs.getString("room",null) else null
        prefs.edit { putString("name",name);putString("endpoint",endpoint.toString());putString("code",s.code);if(expected==null) remove("room") }
        mutable.update { it.copy(active=true,host=false,snapshot=null,pending=null,error=null,connection=LanConnection.CONNECTING) }
        if(!foreground()) return
        client=LanClient(endpoint,s.code,token,name,expectedRoom=expected,initialPending=pending?.let { LanMessage.Submit(it.game,it.round,it.value) },socketFactory={ localSocket(endpoint) },
            onState={ room -> main.post { if(gen==generation) { prefs.edit { putString("room",room.roomId) };receive(room) } } },
            onConnection={ connection,error -> main.post { if(gen==generation) {
                if(error==LanFailure.INVALID) { pending=null;clearPending() }
                mutable.update { it.copy(connection=connection,error=error,pending=if(error==LanFailure.INVALID) null else it.pending) }
                if(connection!=LanConnection.CONNECTED) mutable.update { it.copy(liveValues=emptyMap()) }
                if(connection==LanConnection.FAILED || connection==LanConnection.CLOSED) close()
            } } },liveRuler=s.liveRuler,onLive={ f -> main.post { if(gen==generation) receiveLive(f) } })
    }
    private fun receive(s: LanSnapshot) {
        val old=mutable.value.snapshot
        if(old!=null && old.roomId==s.roomId && s.revision<old.revision) return
        val prompt=s.prompt
        if(!mutable.value.host && s.stage==LanStage.ANSWERING && prompt!=null && rememberedPrompt!=prompt.id) {
            QuestionHistory.remember(app,listOf(Selection.fingerprint(prompt.text)));rememberedPrompt=prompt.id
        }
        val p=pending
        if(p!=null && (p.game!=s.gameId || p.round!=s.round || s.stage!=LanStage.ANSWERING || s.mine!=null)) { pending=null;clearPending() }
        mutable.update { it.copy(snapshot=s,error=null,pending=pending?.value,liveValues=if(old?.revision==s.revision && s.liveEnabled && it.liveRuler) it.liveValues else emptyMap()) }
    }
    private fun receiveLive(frame: LanLiveFrame) {
        mutable.update { s -> if(s.liveRuler && s.connection==LanConnection.CONNECTED && s.snapshot?.let { frame.accepts(it) }==true) s.copy(liveValues=frame.values) else s }
    }
    fun livePreference(enabled: Boolean) {
        if(mutable.value.liveRuler==enabled) return
        prefs.edit { putBoolean("liveRuler",enabled) }
        mutable.update { it.copy(liveRuler=enabled,liveValues=emptyMap()) }
        if(mutable.value.host) server?.preference(enabled) else client?.preference(enabled)
    }
    fun preview(value: Double,game: String,round: Int) {
        val s=mutable.value;val room=s.snapshot ?: return
        if(!s.liveRuler || !room.liveEnabled || room.gameId!=game || room.round!=round || room.stage!=LanStage.ANSWERING || room.mine!=null || s.pending!=null || s.connection!=LanConnection.CONNECTED || !EstimateInput.accepts(room.prompt!!.model(),value)) return
        if(s.host) server?.preview(game,round,room.revision,value) else client?.preview(game,round,room.revision,value)
    }
    private fun clearPending() { prefs.edit { remove("pending_value");remove("pending_game");remove("pending_round") } }
    fun submit(value: Double,game: String,round: Int) {
        val s=mutable.value;val room=s.snapshot ?: return
        if(room.gameId!=game || room.round!=round || room.stage!=LanStage.ANSWERING || room.mine!=null || s.pending!=null || s.connection!=LanConnection.CONNECTED || !EstimateInput.accepts(room.prompt!!.model(),value)) return
        pending=PendingGuess(game,round,value);mutable.update { it.copy(pending=value) }
        prefs.edit { putString("pending_game",game);putInt("pending_round",round);putLong("pending_value",java.lang.Double.doubleToLongBits(value)) }
        if(s.host) server?.submit(game,round,value) else client?.submit(game,round,value)
    }
    fun configure(rounds: Int,difficulty: Int?,categories: Set<String>) { server?.configure(rounds,difficulty,categories) }
    fun start() { server?.start() }
    fun advance() { server?.advance() }
    fun lobby() { server?.lobby() }
    fun remove(id: Int) { server?.remove(id) }
    fun refresh() { server?.let { server -> mutable.update { it.copy(addresses=addresses(server.port)) } } }
    fun serviceStopped(session: Int) { if(session==generation && mutable.value.active) close() }
    fun close() {
        generation++;server?.close();server=null;client?.close();client=null;pending=null
        registration?.let { runCatching { nsd.unregisterService(it) } };registration=null
        stopBrowsing();mutable.update { it.copy(active=false,snapshot=null,pending=null,liveValues=emptyMap(),connection=LanConnection.CLOSED,addresses=emptyList()) }
        app.stopService(serviceIntent)
    }
    private fun addresses(port: Int): List<String> = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback && !Regex("^(rmnet|ccmni|pdp|tun|clat|v4-).*",RegexOption.IGNORE_CASE).matches(it.name) }
            .sortedBy { if(Regex("^(wlan|ap|swlan|wifi|eth).*",RegexOption.IGNORE_CASE).matches(it.name)) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
            .filter { it.isSiteLocalAddress || it.isLinkLocalAddress }.map { "${it.hostAddress}:$port" }.distinct()
    }.getOrDefault(emptyList())
    @Suppress("DEPRECATION") // One-shot route lookup on connect/reconnect, compatible with API 26.
    private fun localSocket(endpoint: LanEndpoint): Socket {
        val socket=Socket()
        try {
            val cm=app.getSystemService(ConnectivityManager::class.java)
            val network=cm.allNetworks.firstOrNull { n ->
                val caps=cm.getNetworkCapabilities(n)
                (caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)==true || caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)==true) && cm.getLinkProperties(n)?.routes?.any { !it.isDefaultRoute && it.destination.contains(endpoint.address.address) }==true
            }
            network?.bindSocket(socket)
            return socket
        } catch(e: Exception) { socket.close();throw e }
    }
    private fun register(port: Int,name: String) {
        val gen=generation
        val listener=object: NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) { if(gen!=generation) runCatching { nsd.unregisterService(this) } }
            override fun onRegistrationFailed(info: NsdServiceInfo,errorCode: Int) { /* Manual IP join remains available. */ }
            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit
            override fun onUnregistrationFailed(info: NsdServiceInfo,errorCode: Int) = Unit
        }
        registration=listener
        runCatching { nsd.registerService(NsdServiceInfo().apply { serviceName="Aproximado - $name";serviceType=TYPE;this.port=port;setAttribute("v",LanWire.VERSION.toString()) },NsdManager.PROTOCOL_DNS_SD,listener) }
    }
    fun browse() {
        if(discovery!=null || mutable.value.active) return
        val listener=object: NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String)=Unit
            override fun onDiscoveryStopped(type: String)=Unit
            override fun onStartDiscoveryFailed(type: String,errorCode: Int) { main.post { stopBrowsing() } }
            override fun onStopDiscoveryFailed(type: String,errorCode: Int)=Unit
            override fun onServiceFound(info: NsdServiceInfo) { main.post { if(discovery===this && info.serviceType==TYPE && resolveQueue.size<20) { resolveQueue.addLast(info);resolveNext() } } }
            override fun onServiceLost(info: NsdServiceInfo) { main.post { mutable.update { it.copy(rooms=it.rooms.filterNot { r -> r.name==info.serviceName }) } } }
        }
        discovery=listener;mutable.update { it.copy(rooms=emptyList(),browsing=true) }
        runCatching {
            multicast=(app.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).createMulticastLock("aproximado-discovery").apply { setReferenceCounted(false);acquire() }
            nsd.discoverServices(TYPE,NsdManager.PROTOCOL_DNS_SD,listener)
        }.onFailure { stopBrowsing() }
    }
    @Suppress("DEPRECATION") // Resolver overload available on all supported Android versions.
    private fun resolveNext() {
        if(resolving || discovery==null || resolveQueue.isEmpty()) return
        val info=resolveQueue.removeFirst();resolving=true;val owner=discovery
        runCatching { nsd.resolveService(info,object: NsdManager.ResolveListener {
            override fun onResolveFailed(service: NsdServiceInfo,errorCode: Int) { main.post { resolving=false;resolveNext() } }
            override fun onServiceResolved(service: NsdServiceInfo) { main.post {
                if(discovery===owner) {
                    val address=service.host
                    val endpoint=runCatching { LanEndpoint.parse("${address?.hostAddress}:${service.port}").toString() }.getOrNull()
                    if(address is Inet4Address && endpoint!=null) mutable.update { s -> s.copy(rooms=(s.rooms.filterNot { it.name==service.serviceName }+FoundRoom(service.serviceName.take(64),endpoint)).take(20)) }
                }
                resolving=false;resolveNext()
            } }
        }) }.onFailure { resolving=false;resolveNext() }
    }
    fun stopBrowsing() {
        val old=discovery;discovery=null
        old?.let { runCatching { nsd.stopServiceDiscovery(it) } };resolveQueue.clear()
        multicast?.let { if(it.isHeld) it.release() };multicast=null
        mutable.update { it.copy(browsing=false) }
    }
    companion object {
        private const val TYPE="_aproximado._tcp."
        @Volatile private var instance: LanRepository?=null
        fun get(app: Context)=instance ?: synchronized(this) { instance ?: LanRepository(app.applicationContext as Application).also { instance=it } }
    }
}
class LanViewModel(app: Application): AndroidViewModel(app) {
    val repository=LanRepository.get(app)
    val state=repository.state
}
