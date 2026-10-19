package es.aproximado.app

import android.app.Application
import android.os.Looper
import es.aproximado.app.lan.*
import es.aproximado.app.ui.lanGameState
import es.aproximado.core.Question
import es.aproximado.core.lan.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
@LooperMode(LooperMode.Mode.PAUSED)
class LanAndroidTest {
    private fun await(condition: ()->Boolean) {
        val deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(25)
        while(true) {
            shadowOf(Looper.getMainLooper()).idle()
            if(condition()) return
            if(System.nanoTime()>deadline) fail("LAN Android state did not arrive")
            Thread.sleep(10)
        }
    }
    @Test fun foregroundRoomSurvivesViewModelRecreationAndScoresMatchOnBothPhones() {
        val app=RuntimeEnvironment.getApplication()
        val offline=app.getSharedPreferences("aproximado",0)
        offline.edit().putString("lan-test-marker","unchanged").commit()
        QuestionHistory.clear(app)
        val repo=LanRepository.get(app)
        val pool=List(25) { Question("test$it","Ciencia","¿Cuántas unidades tiene la referencia $it?",12.0,null,0.0,50.0,"linear",1) }
        repo.livePreference(true);repo.name("Anfitrión");repo.host(pool)
        await { repo.state.value.snapshot!=null && repo.state.value.connection==LanConnection.CONNECTED }
        val intent=shadowOf(app).nextStartedService
        assertEquals(LanService::class.java.name,intent.component!!.className)
        val service=Robolectric.buildService(LanService::class.java,intent).create().startCommand(0,0)
        assertEquals(105,shadowOf(service.get()).lastForegroundNotificationId)
        val first=LanViewModel(app);val recreated=LanViewModel(app)
        assertSame(first.repository,recreated.repository)
        assertTrue(recreated.state.value.active)
        val port=repo.state.value.addresses.first().substringAfterLast(':').toInt()
        val guest=AtomicReference<LanSnapshot>()
        val live=AtomicReference<LanLiveFrame>()
        val client=LanClient(LanEndpoint.parse("127.0.0.1:$port",true),repo.state.value.code,UUID.randomUUID().toString(),"Invitado",onState={guest.set(it)},onConnection={_,_->},onLive={live.set(it)})
        try {
            await { repo.state.value.snapshot!!.members.size==2 }
            repo.configure(5,null,emptySet());repo.start()
            await { guest.get()?.stage==LanStage.ANSWERING && repo.state.value.snapshot?.stage==LanStage.ANSWERING }
            var s=repo.state.value.snapshot!!
            assertTrue(QuestionHistory.read(app).contains(es.aproximado.core.Selection.fingerprint(s.prompt!!.text)))
            repo.preview(10.0,s.gameId,0)
            client.preview(s.gameId,0,s.revision,15.0)
            await { repo.state.value.liveValues[1]==15.0 && live.get()?.values?.get(0)==10.0 }
            assertNull(repo.state.value.snapshot!!.mine)
            repo.livePreference(false)
            assertTrue(repo.state.value.liveValues.isEmpty())
            assertFalse(app.getSharedPreferences("aproximado-lan",0).getBoolean("liveRuler",true))
            await { guest.get()?.liveEnabled==false && repo.state.value.snapshot?.liveEnabled==false }
            repo.livePreference(true)
            await { guest.get()?.liveEnabled==true && repo.state.value.snapshot?.liveEnabled==true }
            s=repo.state.value.snapshot!!
            repo.submit(12.0,s.gameId,0);client.submit(s.gameId,0,15.0)
            await { guest.get()?.stage==LanStage.REVEAL && repo.state.value.snapshot?.stage==LanStage.REVEAL }
            val host=lanGameState(repo.state.value.snapshot!!);val remote=lanGameState(guest.get())
            assertEquals(host.scores(),remote.scores())
            assertEquals(12.0,host.question()!!.answer,0.0)
            assertEquals("unchanged",offline.getString("lan-test-marker",null))
            assertFalse(offline.contains("state"))
        } finally { client.close();repo.close();service.destroy() }
        val firstSeen=QuestionHistory.read(app).toSet()
        val target=pool.first { es.aproximado.core.Selection.fingerprint(it.text) !in firstSeen }
        val otherState=AtomicReference<LanSnapshot>()
        val otherHost=LanServer(pool,"Otro host",onState={otherState.set(it)},initialHistory=pool.filter { it.id!=target.id }.map { it.id })
        try {
            val localIp=java.net.NetworkInterface.getNetworkInterfaces().toList().flatMap { it.inetAddresses.toList() }.filterIsInstance<java.net.Inet4Address>().first { it.isSiteLocalAddress }.hostAddress!!
            repo.endpoint("$localIp:${otherHost.port}");repo.code(otherHost.code);repo.join()
            await { repo.state.value.connection==LanConnection.CONNECTED && otherState.get()?.members?.size==2 }
            otherHost.configure(5,null,emptySet());otherHost.start()
            await { repo.state.value.snapshot?.stage==LanStage.ANSWERING }
            assertEquals(target.text,repo.state.value.snapshot!!.prompt!!.text)
            assertTrue(QuestionHistory.read(app).contains(es.aproximado.core.Selection.fingerprint(target.text)))
        } finally { repo.close();otherHost.close() }
        repo.host(pool)
        await { repo.state.value.connection==LanConnection.CONNECTED && repo.state.value.addresses.isNotEmpty() }
        val nextGuest=AtomicReference<LanSnapshot>()
        val secondPort=repo.state.value.addresses.first().substringAfterLast(':').toInt()
        val secondClient=LanClient(LanEndpoint.parse("127.0.0.1:$secondPort",true),repo.state.value.code,UUID.randomUUID().toString(),"Invitado",onState={nextGuest.set(it)},onConnection={_,_->})
        try {
            await { repo.state.value.snapshot?.members?.size==2 };repo.configure(5,null,emptySet());repo.start()
            await { nextGuest.get()?.stage==LanStage.ANSWERING }
            val fingerprint=es.aproximado.core.Selection.fingerprint(nextGuest.get().prompt!!.text)
            assertFalse(fingerprint in firstSeen);assertNotEquals(es.aproximado.core.Selection.fingerprint(target.text),fingerprint)
        } finally { secondClient.close();repo.close() }
        assertFalse(repo.state.value.active)
        assertNull(repo.state.value.snapshot)
        assertTrue(repo.state.value.liveValues.isEmpty())
    }
}
