package es.aproximado.core.lan

import es.aproximado.core.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class LiveRulerTest {
    private val pool=List(25) { Question("secret$it","Ciencia","¿Cuántas unidades?",12.0,null,0.0,50.0,"linear",1) }
    private fun token()=UUID.randomUUID().toString()
    private fun await(condition: ()->Boolean) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(20)
        while(!condition()) { if(System.nanoTime()>deadline) fail("Live update did not arrive");Thread.sleep(10) }
    }
    @Test fun livePolicyRequiresTwoOrThreeOnlinePlayersAndUnanimousConsent() {
        for(n in listOf(2,3,4,8)) {
            val tokens=List(n) { token() };val room=LanRoom(pool,tokens[0],"Host")
            tokens.drop(1).forEach { room.join(it,"Invitado") };room.start()
            val s=room.snapshot(tokens[0])
            assertEquals(n<4,s.liveEnabled)
            assertEquals(n<4,room.preview(tokens[0],s.gameId,0,s.revision,10.0))
            room.preference(tokens.last(),false)
            assertFalse(room.snapshot(tokens[0]).liveEnabled);assertTrue(room.live(tokens.last()).values.isEmpty())
            room.preference(tokens.last(),true)
            assertEquals(n<4,room.snapshot(tokens[0]).liveEnabled)
            room.offline(tokens.last());assertFalse(room.snapshot(tokens[0]).liveEnabled)
            room.join(tokens.last(),"Otro",false);assertFalse(room.snapshot(tokens[0]).liveEnabled)
            room.preference(tokens.last(),true);assertEquals(n<4,room.snapshot(tokens[0]).liveEnabled)
        }
        val h=token();val r=LanRoom(pool,h,"Host",hostLiveRuler=false);r.join(token(),"Otro");assertFalse(r.snapshot(h).liveEnabled)
    }
    @Test fun draftsNeverConfirmScoreOrLeakRealAnswersAndSubmittedValuesAreImmutable() {
        val h=token();val g=token();val room=LanRoom(pool,h,"Host");room.join(g,"Invitado");room.start()
        var s=room.snapshot(h)
        assertTrue(room.preview(h,s.gameId,0,s.revision,9.0))
        assertEquals(mapOf(0 to 9.0),room.live(g).values);assertTrue(room.live(h).values.isEmpty())
        assertNull(room.snapshot(h).mine);assertFalse(room.snapshot(g).members[0].submitted)
        assertTrue(room.snapshot(g).results.isEmpty());assertEquals(0.0,s.prompt!!.model().answer,0.0)
        room.submit(h,s.gameId,0,11.0);s=room.snapshot(g)
        assertTrue(room.live(g).values.isEmpty())
        assertFalse(room.preview(h,s.gameId,0,s.revision,49.0))
        room.submit(h,s.gameId,0,48.0);room.submit(g,s.gameId,0,13.0)
        assertEquals(listOf(11.0,13.0),room.snapshot(h).results.single().guesses)
        assertEquals(12.0,room.snapshot(h).results.single().answer,0.0)
        assertTrue(room.live(g).values.isEmpty())
    }
    @Test fun invalidAndDelayedDraftsCannotReviveAfterConsentOrRoundChanges() {
        val h=token();val g=token();val room=LanRoom(pool,h,"Host");room.join(g,"Otro");room.start()
        val s=room.snapshot(h)
        for(v in listOf(Double.NaN,Double.POSITIVE_INFINITY,-1.0,51.0,3.5)) assertFalse(room.preview(h,s.gameId,0,s.revision,v))
        assertFalse(room.preview(h,"old",0,s.revision,9.0));assertFalse(room.preview(h,s.gameId,1,s.revision,9.0))
        assertFalse(room.preview(token(),s.gameId,0,s.revision,9.0))
        room.preview(h,s.gameId,0,s.revision,9.0);room.preference(g,false);room.preference(g,true)
        assertFalse(room.preview(h,s.gameId,0,s.revision,9.0));assertTrue(room.live(g).values.isEmpty())
        room.submit(h,s.gameId,0,9.0);room.submit(g,s.gameId,0,8.0);room.advance();room.advance()
        assertTrue(room.live(g).values.isEmpty());assertFalse(room.preview(h,s.gameId,0,room.snapshot(h).revision,10.0))
        room.lobby();assertTrue(room.live(g).values.isEmpty())
    }
    @Test fun wireFramesAreBoundedAndBoundToRecipientRevisionAndPromptPrecision() {
        val h=token();val g=token();val r=LanRoom(pool,h,"Host");r.join(g,"Otro");r.start()
        val s=r.snapshot(g);r.preview(h,s.gameId,0,s.revision,9.0);val frame=r.live(g)
        assertTrue(frame.accepts(s))
        for(m in listOf(LanMessage.Hello(g,"Otro",false),LanMessage.Preview(s.gameId,0,s.revision,9.0),LanMessage.Preference(false),LanMessage.Live(frame),LanMessage.State(s))) assertEquals(m,LanWire.decode(LanWire.encode(m)))
        assertTrue(LanWire.encode(LanMessage.Live(frame)).size<128)
        assertFalse(frame.accepts(s.copy(liveEnabled=false)))
        assertFalse(frame.accepts(s.copy(revision=s.revision+1)))
        assertFalse(frame.copy(values=mapOf(1 to 9.0)).accepts(s))
        assertFalse(frame.copy(values=mapOf(0 to 9.1)).accepts(s))
        assertFalse(frame.accepts(s.copy(members=s.members.map { it.copy(submitted=true) })))
        for(values in listOf(mapOf(0 to 1.0,1 to 2.0,2 to 3.0),mapOf(8 to 1.0),mapOf(0 to Double.NaN))) assertThrows(Exception::class.java) { LanWire.decode(LanWire.encode(LanMessage.Live(frame.copy(values=values)))) }
    }
    @Test fun realThreePhoneSocketsCoalesceDraftsAndStopWhenAnyPlayerOptsOut() {
        val host=AtomicReference<LanSnapshot>();val hostLive=AtomicReference<LanLiveFrame>()
        val server=LanServer(pool,"Host",onState={host.set(it)},onLive={hostLive.set(it)})
        val states=List(2) { AtomicReference<LanSnapshot>() };val frames=List(2) { CopyOnWriteArrayList<LanLiveFrame>() }
        val clients=List(2) { i -> LanClient(LanEndpoint.parse("127.0.0.1:${server.port}",true),server.code,token(),"Invitado$i",onState={states[i].set(it)},onConnection={_,_->},onLive={frames[i].add(it)}) }
        try {
            await { host.get()?.members?.size==3 && states.all { it.get()?.members?.size==3 } }
            server.start();await { states.all { it.get()?.stage==LanStage.ANSWERING } }
            var s=host.get();val revision=s.revision
            repeat(1000) { i -> server.preview(s.gameId,0,revision,(i%40).toDouble());clients[0].preview(s.gameId,0,revision,(i%30).toDouble()) }
            await { hostLive.get()?.values?.get(states[0].get().self)==9.0 && frames[1].lastOrNull()?.values?.get(0)==39.0 }
            assertEquals(revision,host.get().revision);assertTrue(host.get().members.none { it.submitted });assertTrue(frames[1].size<10)
            assertEquals(mapOf(0 to 39.0,states[0].get().self to 9.0),frames[1].last().values)
            clients[1].preference(false);await { !host.get().liveEnabled && states.all { it.get()?.liveEnabled==false } }
            assertFalse(frames[1].last().accepts(states[1].get()))
            clients[1].preference(true);await { host.get().liveEnabled && states.all { it.get()?.liveEnabled==true } }
            s=host.get();server.preview(s.gameId,0,revision,48.0)
            clients[0].preview(s.gameId,0,s.revision,7.0)
            await { hostLive.get()?.revision==s.revision && hostLive.get()?.values?.get(states[0].get().self)==7.0 }
            await { frames[1].lastOrNull()?.let { it.revision==s.revision && it.values[0]==null }==true }
            // A submission takes priority over the pending draft and keeps its exact value.
            clients[0].submit(s.gameId,0,14.0);await { states[0].get().mine==14.0 }
            await { hostLive.get()?.revision==host.get().revision && hostLive.get()?.values?.containsKey(states[0].get().self)==false }
            clients[1].submit(s.gameId,0,13.0);server.submit(s.gameId,0,12.0)
            await { states.all { it.get()?.stage==LanStage.REVEAL } }
            val results=host.get().results.single()
            assertEquals(12.0,results.guesses[0],0.0);assertEquals(14.0,results.guesses[states[0].get().self],0.0);assertEquals(13.0,results.guesses[states[1].get().self],0.0)
        } finally { clients.forEach { it.close() };server.close() }
    }
    @Test fun fourthSeatDisablesLiveEvenIfItDisconnectsAndReenablesOnlyAfterRemoval() {
        val h=token();val r=LanRoom(pool,h,"Host");val guests=List(3) { token() };guests.forEach { r.join(it,"Otro") }
        assertFalse(r.snapshot(h).liveEnabled);r.offline(guests.last());assertFalse(r.snapshot(h).liveEnabled)
        r.remove(3);assertTrue(r.snapshot(h).liveEnabled)
        r.preference(h,false);r.remove(2);assertFalse(r.snapshot(h).liveEnabled)
    }
}
