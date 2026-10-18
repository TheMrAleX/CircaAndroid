package es.aproximado.core.lan

import es.aproximado.core.Question
import java.io.Closeable
import java.net.*
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import java.security.SecureRandom

private fun daemonThreads(prefix: String)=ThreadFactory { task -> Thread(task,prefix).apply { isDaemon=true } }
private class Peer(val socket: Socket,val channel: LanChannel,workers: ExecutorService) : Closeable {
    private val queue=ArrayBlockingQueue<LanMessage>(8)
    private val closed=AtomicBoolean(false)
    init { workers.execute { try { while(!closed.get()) { val m=queue.take();channel.send(m);if(m==LanMessage.Bye) break } } catch (_: Exception) { } finally { close() } } }
    fun send(message: LanMessage) {
        if(closed.get()) return
        // Snapshots are complete: retain only the newest queued state for a slow connection.
        if(message is LanMessage.State) queue.removeIf { it is LanMessage.State }
        if(!queue.offer(message)) close()
    }
    fun read()=channel.receive()
    override fun close() { if(closed.compareAndSet(false,true)) { runCatching { socket.close() };queue.clear();queue.offer(LanMessage.Bye) } }
}

/** Bounded connections, no network IO in the state actor, no traffic for slider movements. */
class LanServer(pool: List<Question>,hostName: String,val code: String = "%08d".format(java.util.Locale.ROOT,SecureRandom().nextInt(100000000)),
    private val onState: (LanSnapshot)->Unit,private val onFailure: (LanFailure)->Unit = {}) : Closeable {
    private val hostToken=UUID.randomUUID().toString()
    private val room=LanRoom(pool,hostToken,hostName)
    private val actor=Executors.newSingleThreadExecutor(daemonThreads("lan-room"))
    private val workers=Executors.newFixedThreadPool(24,daemonThreads("lan-io"))
    private val timer=Executors.newSingleThreadScheduledExecutor(daemonThreads("lan-timeout"))
    private val permits=Semaphore(12)
    private val sockets=ConcurrentHashMap.newKeySet<Socket>()
    private val peers=mutableMapOf<String,Peer>()
    private val server=ServerSocket().apply { reuseAddress=true;bind(InetSocketAddress(0)) }
    val port: Int get()=server.localPort
    @Volatile private var closed=false
    init {
        actor.execute { onState(room.snapshot(hostToken)) }
        Thread({ acceptLoop() },"lan-listen").apply { isDaemon=true;start() }
    }
    private fun acceptLoop() {
        val attempts=linkedMapOf<String,ArrayDeque<Long>>()
        while(!closed) {
            try {
                val socket=server.accept()
                val ip=socket.inetAddress
                val now=System.nanoTime()/1000000
                val times=attempts.getOrPut(ip.hostAddress!!) { ArrayDeque() }
                while(times.isNotEmpty() && now-times.first()>30000) times.removeFirst()
                if(attempts.size>64) attempts.remove(attempts.keys.first())
                if(!(ip.isSiteLocalAddress || ip.isLinkLocalAddress || ip.isLoopbackAddress) || times.size>=12 || !permits.tryAcquire()) { socket.close();continue }
                times.addLast(now);sockets.add(socket)
                workers.execute { acceptPeer(socket) }
            } catch (_: Exception) { if(!closed) onFailure(LanFailure.NETWORK) }
        }
    }
    private fun acceptPeer(socket: Socket) {
        var peer: Peer?=null
        var token: String?=null
        try {
            socket.tcpNoDelay=true;socket.soTimeout=8000
            val channel=LanChannel.open(socket.getInputStream(),socket.getOutputStream(),code,false)
            val hello=channel.receive() as? LanMessage.Hello ?: throw LanException(LanFailure.INVALID)
            peer=Peer(socket,channel,workers);val active=peer;token=hello.token
            val accepted=actor.submit<Boolean> {
                if(closed) return@submit false
                try {
                    room.join(hello.token,hello.name)
                    peers.put(hello.token,active)?.close()
                    broadcast();true
                } catch(e: LanException) { active.send(LanMessage.Error(e.reason));timer.schedule({ active.close() },300,TimeUnit.MILLISECONDS);false }
            }.get(3,TimeUnit.SECONDS)
            if(!accepted) { Thread.sleep(350);return }
            socket.soTimeout=15000
            var window=System.nanoTime();var commands=0
            while(!closed) {
                val message=active.read()
                val now=System.nanoTime();if(now-window>TimeUnit.SECONDS.toNanos(1)) { window=now;commands=0 }
                if(++commands>12) throw LanException(LanFailure.INVALID)
                actor.execute {
                    if(peers[hello.token]!==active || closed) return@execute
                    when(message) {
                        LanMessage.Ping -> active.send(LanMessage.Pong)
                        is LanMessage.Submit -> try { room.submit(hello.token,message.game,message.round,message.value);broadcast() } catch(e: LanException) { active.send(LanMessage.Error(e.reason)) }
                        else -> active.close()
                    }
                }
            }
        } catch (_: Exception) { } finally {
            peer?.close();runCatching { socket.close() };sockets.remove(socket);permits.release()
            val ended=peer;val who=token
            if(!closed && who!=null) runCatching { actor.execute { if(peers[who]===ended) { peers.remove(who);room.offline(who);broadcast() } } }
        }
    }
    private fun broadcast() {
        onState(room.snapshot(hostToken))
        peers.forEach { (token,peer) -> runCatching { peer.send(LanMessage.State(room.snapshot(token))) } }
    }
    private fun change(action: ()->Unit) { if(!closed) runCatching { actor.execute { try { action();broadcast() } catch(e: LanException) { onFailure(e.reason) } } } }
    fun configure(rounds: Int,difficulty: Int?,categories: Set<String>)=change { room.configure(rounds,difficulty,categories) }
    fun start()=change { room.start() }
    fun submit(game: String,round: Int,value: Double)=change { room.submit(hostToken,game,round,value) }
    fun advance()=change { room.advance() }
    fun lobby()=change { room.lobby() }
    fun remove(index: Int)=change {
        val before=peers.keys.toList()
        room.remove(index)
        before.forEach { token -> if(runCatching { room.snapshot(token) }.isFailure) { peers.remove(token)?.let { it.send(LanMessage.Bye);timer.schedule({ it.close() },200,TimeUnit.MILLISECONDS) } } }
    }
    override fun close() {
        if(closed) return
        closed=true;runCatching { server.close() }
        actor.execute {
            peers.values.forEach { it.send(LanMessage.Bye) }
            timer.schedule({ sockets.forEach { runCatching { it.close() } };workers.shutdownNow();actor.shutdownNow();timer.shutdown() },300,TimeUnit.MILLISECONDS)
        }
    }
}

enum class LanConnection { CONNECTING, CONNECTED, RECONNECTING, FAILED, CLOSED }
class LanClient(private val endpoint: LanEndpoint,private val code: String,private val token: String,private val name: String,
    private val expectedRoom: String? = null,initialPending: LanMessage.Submit? = null,private val socketFactory: ()->Socket = { Socket() },
    private val onState: (LanSnapshot)->Unit,private val onConnection: (LanConnection,LanFailure?)->Unit) : Closeable {
    private val workers=Executors.newFixedThreadPool(3,daemonThreads("lan-client"))
    private val timer=Executors.newSingleThreadScheduledExecutor(daemonThreads("lan-ping"))
    private var task: Future<*>?=null
    @Volatile private var peer: Peer?=null
    @Volatile private var connecting: Socket?=null
    @Volatile private var closed=false
    @Volatile private var pending: LanMessage.Submit?=initialPending
    private var roomId: String?=expectedRoom
    init {
        task=workers.submit { connectLoop() }
        timer.scheduleAtFixedRate({ peer?.send(LanMessage.Ping) },4,4,TimeUnit.SECONDS)
    }
    fun submit(game: String,round: Int,value: Double) { val message=LanMessage.Submit(game,round,value);pending=message;peer?.send(message) }
    private fun connectLoop() {
        var failures=0;var everConnected=false
        while(!closed && failures<6) {
            var welcomed=false
            try {
                onConnection(if(everConnected) LanConnection.RECONNECTING else LanConnection.CONNECTING,null)
                val socket=socketFactory();connecting=socket
                socket.tcpNoDelay=true;socket.soTimeout=8000;socket.connect(endpoint.address,5000)
                val channel=LanChannel.open(socket.getInputStream(),socket.getOutputStream(),code,true)
                val active=Peer(socket,channel,workers);peer=active
                active.send(LanMessage.Hello(token,name))
                socket.soTimeout=15000
                while(!closed) {
                    when(val message=active.read()) {
                        is LanMessage.State -> {
                            val s=message.snapshot
                            if(roomId!=null && roomId!=s.roomId) throw LanException(LanFailure.ROOM_CHANGED)
                            val firstSnapshot=!welcomed
                            roomId=s.roomId;welcomed=true;everConnected=true;failures=0
                            onConnection(LanConnection.CONNECTED,null);onState(s)
                            pending?.let { p ->
                                if(s.gameId!=p.game || s.round!=p.round || s.stage!=LanStage.ANSWERING || s.mine!=null) pending=null
                                else if(firstSnapshot) active.send(p)
                            }
                        }
                        is LanMessage.Error -> {
                            if(message.reason==LanFailure.INVALID) { pending=null;onConnection(LanConnection.CONNECTED,LanFailure.INVALID) }
                            else throw LanException(message.reason)
                        }
                        LanMessage.Pong -> Unit
                        LanMessage.Bye -> { onConnection(LanConnection.CLOSED,LanFailure.CLOSED);return }
                        else -> throw LanException(LanFailure.INVALID)
                    }
                }
            } catch(e: Exception) {
                if(closed) return
                val terminal=(e as? LanException)?.reason
                if(terminal!=null || e is javax.crypto.AEADBadTagException || !welcomed && !everConnected && e is java.io.EOFException) {
                    onConnection(LanConnection.FAILED,terminal ?: LanFailure.WRONG_CODE);return
                }
                failures++
                onConnection(LanConnection.RECONNECTING,LanFailure.DISCONNECTED)
            } finally { peer?.close();peer=null;runCatching { connecting?.close() };connecting=null }
            if(!closed && failures<6) try { Thread.sleep((1000L shl (failures-1).coerceIn(0,3))) } catch (_: InterruptedException) { return }
        }
        if(!closed) onConnection(LanConnection.FAILED,LanFailure.DISCONNECTED)
    }
    override fun close() {
        closed=true;peer?.close();runCatching { connecting?.close() };task?.cancel(true);workers.shutdownNow();timer.shutdownNow()
    }
}
