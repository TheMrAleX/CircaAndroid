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
        val repo=LanRepository.get(app)
        val pool=List(25) { Question("test$it","Ciencia","¿Cuántas unidades?",12.0,null,0.0,50.0,"linear",1) }
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
        assertFalse(repo.state.value.active)
        assertNull(repo.state.value.snapshot)
        assertTrue(repo.state.value.liveValues.isEmpty())
    }
}
