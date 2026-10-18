@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
package es.aproximado.app

import android.view.MotionEvent
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.*
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.*
import es.aproximado.app.ui.*
import es.aproximado.core.lan.LanPrompt
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/** Drive the real selector while attached, using its actual accessibility actions and draft effect. */
class LiveSelectorTest {
    @get:Rule val paparazzi=Paparazzi(deviceConfig=DeviceConfig.PIXEL_5.copy(screenWidth=360,screenHeight=800,density=com.android.resources.Density.MEDIUM),theme="android:style/Theme.Material.NoActionBar",maxPercentDifference=0.1)
    private fun root(view: View): RootForTest? = (view as? RootForTest) ?: (view as? ViewGroup)?.let { group -> (0 until group.childCount).firstNotNullOfOrNull { root(group.getChildAt(it)) } }
    private fun descendants(node: SemanticsNode): List<SemanticsNode> = listOf(node)+node.children.flatMap(::descendants)
    @Test fun movementsPublishFineAdjustmentsRemainSelectedAndOptOutStopsSharing() {
        val enabled=mutableStateOf(true);val epoch=mutableLongStateOf(1);val opponent=mutableDoubleStateOf(15.0)
        val published=mutableListOf<Double>();val confirmed=mutableListOf<Double>()
        var completed=false
        var failure: Throwable?=null
        val base=previewState(8);val q=base.pool.first().copy(min=0.0,max=50.0,answer=12.0,decimalPlaces=0,unit=null)
        val state=base.copy(pool=listOf(q),match=base.match!!.copy(guesses=listOf(emptyList())))
        suspend fun advance(millis: Long) {
            val start=withFrameNanos { it }
            do { val elapsed=withFrameNanos { it }-start } while(elapsed<millis*1_000_000)
        }
        val view=ComposeView(paparazzi.context)
        fun nodes()=descendants(root(view)!!.semanticsOwner.unmergedRootSemanticsNode)
        fun progress(value: Float) { assertTrue(nodes().first { it.config.contains(SemanticsActions.SetProgress) }.config[SemanticsActions.SetProgress].action!!.invoke(value)) }
        fun click(label: String) {
            val node=nodes().first { n -> n.config.contains(SemanticsActions.OnClick) && descendants(n).any { c -> c.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true || c.config.getOrNull(SemanticsProperties.Text)?.any { it.text==label }==true } }
            assertTrue(node.config[SemanticsActions.OnClick].action!!.invoke())
        }
        suspend fun dragOverLastOpponent() {
            val bounds=nodes().first { it.config.contains(SemanticsActions.SetProgress) }.boundsInRoot
            val x=bounds.right-24f;val y=bounds.top+70f;val down=SystemClock.uptimeMillis()
            fun event(action: Int,offset: Float) {
                val e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y+offset,0)
                try { val handled=view.dispatchTouchEvent(e);if(action!=MotionEvent.ACTION_CANCEL) assertTrue(handled) } finally { e.recycle() }
            }
            event(MotionEvent.ACTION_DOWN,0f);advance(50)
            event(MotionEvent.ACTION_MOVE,-32f);advance(50)
            event(MotionEvent.ACTION_MOVE,-60f);advance(50)
            event(MotionEvent.ACTION_CANCEL,-60f);advance(150)
        }
        view.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { AproximadoTheme {
                SelectorScreen(state,Actions(confirm={v,_,_->confirmed.add(v)}),playerIndex=0,
                    onPreview=if(enabled.value) { { published.add(it) } } else null,previewEpoch=epoch.longValue,
                    liveRoom=previewLiveLan(8).let { s -> s.copy(liveRuler=enabled.value,snapshot=s.snapshot!!.copy(prompt=LanPrompt.of(q.id,q),mine=null,members=s.snapshot.members.map { it.copy(submitted=false) }),liveValues=(1..7).associateWith { opponent.doubleValue+it }) },onLiveSettings={})
                LaunchedEffect(Unit) { try {
                    advance(100);assertTrue(published.isNotEmpty())
                    assertEquals(8,nodes().count { it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { d -> d.startsWith("Regla de ") }==true })
                    progress(.2f);advance(150);assertEquals(10.0,published.last(),0.0)
                    click(paparazzi.context.getString(R.string.increase));advance(150);assertEquals(11.0,published.last(),0.0)
                    dragOverLastOpponent();assertTrue("Dragging the last opponent column changes the local estimate",published.last()>11.0)
                    assertTrue(confirmed.isEmpty())
                    val before=published.toList();opponent.doubleValue=42.0
                    advance(150);assertEquals(before,published)
                    enabled.value=false;advance(100);assertEquals(0,nodes().count { it.config.getOrNull(SemanticsProperties.ContentDescription)?.any { d -> d.startsWith("Regla de ") }==true });progress(.4f);advance(150);assertEquals(before,published)
                    enabled.value=true;epoch.longValue++
                    advance(150);assertEquals(20.0,published.last(),0.0)
                    click(paparazzi.context.getString(R.string.confirm));advance(400)
                    assertEquals(listOf(20.0),confirmed)
                    val after=published.toList();progress(.8f);advance(100);assertEquals(after,published)
                    completed=true
                } catch(t: Throwable) { failure=t } }
            } }
        }
        try { paparazzi.gif(view,"live_selector",start=0,end=4000,fps=20);failure?.let { throw it };assertTrue("All interaction assertions executed",completed) }
        finally { view.disposeComposition() }
    }
}
