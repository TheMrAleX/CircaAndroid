package es.aproximado.core.lan

import es.aproximado.core.*
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicReference
import java.io.*
import java.net.*

class LanTest {
    private fun token()=UUID.randomUUID().toString()
    private val pool=(0..29).map { Question("secret-$it","Ciencia","¿Cuántas unidades tiene esta referencia?",9.80665,"m",0.0,30.0,"linear",2,source="private-source",decimalPlaces=5) }
    private fun waitFor(condition: ()->Boolean) {
        val end=System.nanoTime()+TimeUnit.SECONDS.toNanos(25)
        while(!condition()) { if(System.nanoTime()>end) fail("Timed out waiting for LAN state");Thread.sleep(10) }
    }
    @Test fun roomKeepsAnswersPrivateAndRejectsStaleDuplicateAndInvalidSubmissions() {
        val host=token();val guest=token();val room=LanRoom(pool,host,"Ana")
        room.join(guest,"Luis");room.configure(5,null,emptySet());room.start()
        var s=room.snapshot(host);val game=s.gameId
        assertEquals(0.0,s.prompt!!.model().answer,0.0)
        assertFalse(s.prompt!!.id.contains("secret"));assertTrue(s.results.isEmpty())
        room.submit(host,game,0,9.0);room.submit(host,game,0,10.0)
        assertEquals(9.0,room.snapshot(host).mine!!,0.0)
        s=room.snapshot(guest);assertNull(s.mine);assertTrue(s.results.isEmpty());assertTrue(s.members[0].submitted)
        for(value in listOf(Double.NaN,-1.0,31.0,9.123456)) assertThrows(LanException::class.java) { room.submit(guest,game,0,value) }
        assertThrows(LanException::class.java) { room.submit(guest,"old-game",0,9.0) }
        room.submit(guest,game,0,9.80665)
        s=room.snapshot(guest);assertEquals(LanStage.REVEAL,s.stage);assertEquals(listOf(9.0,9.80665),s.results.single().guesses)
        assertEquals(9.80665,s.results.single().answer,0.0)
        room.advance();room.advance()
        assertThrows(LanException::class.java) { room.submit(guest,game,0,9.0) }
    }
    @Test fun reconnectPreservesSeatAndResponseWithoutAllowingLateNewPlayers() {
        val h=token();val g=token();val r=LanRoom(pool,h,"Ana");r.join(g,"Luis");r.start()
        val s=r.snapshot(g);r.submit(g,s.gameId,s.round,9.0);r.offline(g)
        assertFalse(r.snapshot(h).members[1].online)
        assertEquals(1,r.join(g,"Otro nombre"));assertEquals("Luis",r.snapshot(g).members[1].name)
        assertEquals(9.0,r.snapshot(g).mine!!,0.0)
        assertThrows(LanException::class.java) { r.join(token(),"Nuevo") }
        assertThrows(LanException::class.java) { r.remove(1) }
    }
    @Test fun completeTwoAndEightPlayerGamesAndHostOnlyAdvanceSequence() {
        for(n in listOf(2,8)) {
            val tokens=List(n){token()};val r=LanRoom(pool,tokens[0],"Host")
            tokens.drop(1).forEachIndexed { i,t -> r.join(t,"Jugador$i") };r.configure(5,null,emptySet());r.start()
            val ids=mutableSetOf<String>()
            repeat(5) { round ->
                val s=r.snapshot(tokens[0]);assertEquals(round,s.round);assertTrue(ids.add(s.prompt!!.id))
                tokens.forEachIndexed { i,t -> r.submit(t,s.gameId,round,9.0+i) }
                assertEquals(LanStage.REVEAL,r.snapshot(tokens[0]).stage)
                r.advance();assertEquals(LanStage.SCORE,r.snapshot(tokens[0]).stage);r.advance()
            }
            val end=r.snapshot(tokens[0]);assertEquals(LanStage.FINISHED,end.stage);assertEquals(5,end.results.size)
            r.lobby();assertTrue(r.snapshot(tokens[0]).results.isEmpty())
        }
    }
    @Test fun lobbyCapacityAndOfflineRemovalAreExplicit() {
        val h=token();val r=LanRoom(pool,h,"Ana")
        val tokens=List(7){token()};tokens.forEach { r.join(it,"Otro") }
        assertThrows(LanException::class.java) { r.join(token(),"Nuevo") }
        r.offline(tokens[0]);assertThrows(LanException::class.java) { r.start() }
        r.remove(1);r.join(token(),"Nuevo")
        val s=r.snapshot(h);assertEquals((0..7).toList(),s.members.map { it.id });r.start()
    }
    @Test fun boundedWireRoundTripsAndRejectsMalformedFrames() {
        val h=token();val r=LanRoom(pool,h,"Ana");r.join(token(),"Luis")
        val m=LanMessage.State(r.snapshot(h));assertEquals(m,LanWire.decode(LanWire.encode(m)))
        r.start();val secret=LanWire.encode(LanMessage.State(r.snapshot(h)))
        assertFalse(String(secret).contains("private-source"));assertFalse(String(secret).contains("secret-"))
        assertThrows(Exception::class.java) { LanWire.decode(byteArrayOf(1,127,127,127,127)) }
        assertThrows(Exception::class.java) { LanWire.decode(ByteArray(LanWire.MAX_FRAME+1)) }
        assertThrows(Exception::class.java) { LanWire.decode(LanWire.encode(LanMessage.Ping)+byteArrayOf(0)) }
    }
    @Test fun endpointsNeverResolveNamesOrAllowPublicInternet() {
        assertEquals("192.168.43.1:45555",LanEndpoint.parse("192.168.43.1:45555").toString())
        for(s in listOf("example.com:4000","8.8.8.8:4000","127.0.0.1:4000","192.168.1.1:80","192.168.1.999:4000","192.168.1.1:65536")) assertThrows(Exception::class.java) { LanEndpoint.parse(s) }
    }
    @Test fun authenticatedRealSocketsSynchronizeEightPlayersAndReconnect() {
        val state=AtomicReference<LanSnapshot>();val server=LanServer(pool,"Host",onState={state.set(it)})
        val guests=mutableListOf<LanClient>();val states=List(7){AtomicReference<LanSnapshot>()};val tokens=List(7){token()}
        try {
            val endpoint=LanEndpoint.parse("127.0.0.1:${server.port}",true)
            repeat(7) { i -> guests.add(LanClient(endpoint,server.code,tokens[i],"Jugador$i",onState={states[i].set(it)},onConnection={_,_->})) }
            waitFor { state.get()?.members?.size==8 && states.all { it.get()?.members?.size==8 } }
            server.configure(5,null,emptySet());server.start()
            waitFor { states.all { it.get()?.stage==LanStage.ANSWERING } }
            val s=state.get();guests[0].submit(s.gameId,0,9.0)
            waitFor { states[0].get()?.mine==9.0 }
            guests[0].close();waitFor { state.get().members.any { !it.online } }
            guests[0]=LanClient(endpoint,server.code,tokens[0],"Jugador0",onState={states[0].set(it)},onConnection={_,_->})
            waitFor { state.get().members.all { it.online } && states[0].get()?.mine==9.0 }
            guests.drop(1).forEach { it.submit(s.gameId,0,10.0) };server.submit(s.gameId,0,9.80665)
            waitFor { states.all { it.get()?.stage==LanStage.REVEAL } }
            assertEquals(8,state.get().results.single().guesses.size)
            assertTrue(states.all { it.get().results==state.get().results })
            repeat(4) { index ->
                server.advance();server.advance()
                waitFor { states.all { it.get()?.stage==LanStage.ANSWERING && it.get()?.round==index+1 } }
                val next=state.get()
                guests.forEach { it.submit(next.gameId,next.round,10.0) };server.submit(next.gameId,next.round,9.80665)
                waitFor { states.all { it.get()?.stage==LanStage.REVEAL && it.get()?.round==index+1 } }
            }
            server.advance();server.advance()
            waitFor { states.all { it.get()?.stage==LanStage.FINISHED } }
            assertEquals(5,state.get().results.size)
        } finally { guests.forEach { it.close() };server.close() }
    }
    @Test fun wrongRoomCodeCannotJoinOrObserveState() {
        val host=AtomicReference<LanSnapshot>();val failure=AtomicReference<LanFailure>();val received=AtomicReference<LanSnapshot>()
        val server=LanServer(pool,"Host",onState={host.set(it)})
        val wrong=if(server.code=="00000000") "11111111" else "00000000"
        val client=LanClient(LanEndpoint.parse("127.0.0.1:${server.port}",true),wrong,token(),"Intruso",onState={received.set(it)},onConnection={s,e->if(s==LanConnection.FAILED) failure.set(e)})
        try { waitFor { failure.get()!=null };assertEquals(LanFailure.WRONG_CODE,failure.get());assertNull(received.get());assertEquals(1,host.get().members.size) } finally { client.close();server.close() }
    }
    @Test fun cipherRejectsTamperingAndReplay() {
        for(replay in listOf(false,true)) {
            val listener=ServerSocket(0)
            val clientSocket=Socket("127.0.0.1",listener.localPort).apply { soTimeout=5000 }
            val serverSocket=listener.accept().apply { soTimeout=5000 };listener.close()
            val recorded=ByteArrayOutputStream()
            val output=object: OutputStream() {
                override fun write(b: Int) { recorded.write(b);serverSocket.getOutputStream().write(b) }
                override fun write(b: ByteArray,off: Int,len: Int) { recorded.write(b,off,len);serverSocket.getOutputStream().write(b,off,len) }
                override fun flush()=serverSocket.getOutputStream().flush()
            }
            val executor=Executors.newSingleThreadExecutor()
            try {
                val future=executor.submit<LanChannel> { LanChannel.open(serverSocket.getInputStream(),output,"12345678",false) }
                val client=LanChannel.open(clientSocket.getInputStream(),clientSocket.getOutputStream(),"12345678",true)
                val server=future.get(10,TimeUnit.SECONDS)
                recorded.reset();server.send(LanMessage.Ping)
                val frame=recorded.toByteArray()
                if(replay) { assertEquals(LanMessage.Ping,client.receive());serverSocket.getOutputStream().write(frame) }
                else {
                    // First consume a valid frame. Modify the next authenticated frame in transit.
                    assertEquals(LanMessage.Ping,client.receive())
                    recorded.reset();val sink=ByteArrayOutputStream()
                    // A copied earlier frame with one altered tag must also fail, never decode as a command.
                    frame[frame.lastIndex]=(frame.last().toInt() xor 1).toByte()
                    sink.write(frame);serverSocket.getOutputStream().write(sink.toByteArray())
                }
                assertThrows(Exception::class.java) { client.receive() }
            } finally { clientSocket.close();serverSocket.close();executor.shutdownNow() }
        }
    }
    @Test fun automaticReconnectResendsAnUnacknowledgedGuessExactlyOnce() {
        val host=AtomicReference<LanSnapshot>();val server=LanServer(pool,"Host",onState={host.set(it)})
        val proxy=ServerSocket(0);val sockets=java.util.Collections.synchronizedList(mutableListOf<Socket>())
        val relay=Executors.newCachedThreadPool { task -> Thread(task,"test-proxy").apply { isDaemon=true } }
        relay.execute {
            while(!proxy.isClosed) try {
                val incoming=proxy.accept();val outgoing=Socket("127.0.0.1",server.port)
                sockets.add(incoming);sockets.add(outgoing)
                relay.execute { runCatching { incoming.getInputStream().copyTo(outgoing.getOutputStream()) };runCatching { outgoing.close() } }
                relay.execute { runCatching { outgoing.getInputStream().copyTo(incoming.getOutputStream()) };runCatching { incoming.close() } }
            } catch (_: Exception) { }
        }
        val state=AtomicReference<LanSnapshot>();val reconnects=java.util.concurrent.atomic.AtomicInteger()
        val client=LanClient(LanEndpoint.parse("127.0.0.1:${proxy.localPort}",true),server.code,token(),"Ana",onState={state.set(it)},onConnection={s,_->if(s==LanConnection.RECONNECTING) reconnects.incrementAndGet()})
        try {
            waitFor { host.get()?.members?.size==2 };server.start();waitFor { state.get()?.stage==LanStage.ANSWERING }
            val before=state.get()
            synchronized(sockets) { sockets.forEach { it.close() };sockets.clear() }
            client.submit(before.gameId,0,9.12345)
            waitFor { reconnects.get()>0 && state.get()?.mine==9.12345 }
            server.submit(before.gameId,0,9.80665)
            waitFor { state.get()?.stage==LanStage.REVEAL }
            assertEquals(listOf(9.80665,9.12345),state.get().results.single().guesses)
        } finally { client.close();server.close();proxy.close();synchronized(sockets) { sockets.forEach { runCatching { it.close() } } };relay.shutdownNow() }
    }
}
