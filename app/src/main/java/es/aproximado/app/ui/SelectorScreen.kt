@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package es.aproximado.app.ui

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import es.aproximado.app.*
import es.aproximado.app.R
import es.aproximado.core.*
import kotlinx.coroutines.launch
import kotlin.math.*

val LocalFeedback = staticCompositionLocalOf<(Boolean)->Unit> { {} }

@Composable fun SelectorScreen(state: AppState,actions: Actions,onExit: ()->Unit = {}) {
    val m=state.match ?: return;val q=state.question() ?: return
    val p=m.config.players[m.turn];val style=Tokens.players[p.style]
    val scope=rememberCoroutineScope()
    val density=LocalDensity.current
    val feedback=LocalFeedback.current
    val distance=with(density) { 12000.dp.toPx() }
    var position by remember(q.id,m.turn) { mutableFloatStateOf(RulerScale.initial(q).toFloat()) }
    var exact by remember(q.id,m.turn) { mutableStateOf<Double?>(null) }
    var keyboard by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var lastTick by remember { mutableLongStateOf(0L) }
    var lastMajor by remember { mutableIntStateOf(-1) }
    val fling=remember { Animatable(0f) }
    val leaving=remember { Animatable(0f) }
    val decay=rememberSplineBasedDecay<Float>()
    val value=exact ?: RulerScale.snapped(q,position.toDouble())
    fun setPosition(next: Float) {
        position=next.coerceIn(0f,1f);exact=null
        val major=(position*100).toInt()
        if(major!=lastMajor && SystemClock.uptimeMillis()-lastTick>=40) {
            lastMajor=major;lastTick=SystemClock.uptimeMillis();feedback(false)
        }
    }
    fun fine(direction: Int) {
        val raw=if(q.scale=="log") max(value*(exp((ln(q.max)-ln(q.min))/1000)-1),.001) else (q.max-q.min)/1000
        val step=RulerScale.niceStep(raw).coerceAtLeast(if(q.answer==floor(q.answer)) 1.0 else .01)
        val next=(value+direction*step).coerceIn(q.min,q.max)
        position=RulerScale.position(q,next).toFloat();exact=next;feedback(false)
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(Tokens.bg)) {
        val sheetHeight=maxOf(if(density.fontScale>1.15) 390.dp else 320.dp,maxHeight*.42f).coerceAtMost(maxHeight-180.dp)
        val sheetTop=maxHeight-sheetHeight
        val description=stringResource(R.string.estimate_accessibility,formatNumber(value))
        Column(Modifier.fillMaxWidth().statusBarsPadding()) {
            TopBar(stringResource(R.string.selector_title,p.name,m.round+1,m.config.rounds),close=true,onAction=onExit)
            Box(Modifier.fillMaxWidth().height((sheetTop-56.dp).coerceAtLeast(80.dp))
                .semantics { contentDescription=description;setProgress { progress -> setPosition(progress);true };progressBarRangeInfo=ProgressBarRangeInfo(position,0f..1f) }
                .draggable(rememberDraggableState { delta -> setPosition(position+delta/distance) },Orientation.Vertical,
                    onDragStarted={ fling.stop() },onDragStopped={ velocity ->
                        scope.launch {
                            fling.snapTo(position);fling.updateBounds(0f,1f)
                            fling.animateDecay(velocity/distance,decay) { setPosition(this.value) }
                        }
                    })) {
                RulerCanvas(Modifier.fillMaxSize(),position*distance)
                Label(stringResource(R.string.drag_hint),Modifier.align(Alignment.TopCenter).padding(top=24.dp,start=60.dp,end=60.dp),13,Tokens.muted,center=true)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
            .graphicsLayer { translationY=leaving.value*size.height }.clip(RoundedCornerShape(topStart=40.dp,topEnd=40.dp))
            .background(style.color).navigationBarsPadding().padding(start=24.dp,end=24.dp,top=56.dp,bottom=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            q.unit?.let { Label(it,size=16,color=style.ink.copy(alpha=.7f),center=true) }
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical=8.dp),contentAlignment=Alignment.Center) {
                Label(q.text,size=20,color=style.ink,bold=true,center=true)
            }
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                RoundButton("−",stringResource(R.string.decrease),color=style.ink.copy(alpha=.15f),ink=style.ink) { fine(-1) }
                RoundButton("⌨",stringResource(R.string.keyboard),color=style.ink.copy(alpha=.15f),ink=style.ink) { input=formatNumber(value).replace(".","");keyboard=true }
                RoundButton("+",stringResource(R.string.increase),color=style.ink.copy(alpha=.15f),ink=style.ink) { fine(1) }
            }
            GameButton(stringResource(R.string.confirm),color=Color.White,ink=style.ink,enabled=!submitted) {
                if(!submitted) { submitted=true;feedback(true);scope.launch { leaving.animateTo(1f,tween(250));actions.confirm(value) } }
            }
        }
        BigNumber(formatNumber(value),Modifier.offset(y=sheetTop-60.dp).padding(horizontal=24.dp).graphicsLayer { translationY=leaving.value*size.height },style)
    }
    if(keyboard) {
        val parsed=input.trim().replace(" ","").replace(",",".").toDoubleOrNull()
        val valid=parsed!=null && parsed.isFinite() && parsed in q.min..q.max
        ModalBottomSheet(onDismissRequest={keyboard=false},containerColor=Tokens.surface,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Label(stringResource(R.string.numeric_title),size=22,bold=true)
                val estimateLabel=stringResource(R.string.number_hint)
                BasicTextField(input,{input=it.take(24)},Modifier.fillMaxWidth().heightIn(min=64.dp).semantics { contentDescription=estimateLabel },singleLine=true,
                    textStyle=TextStyle(color=Color.White,fontSize=44.sp,fontWeight=FontWeight.Black,fontFamily=Tokens.display),
                    keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),cursorBrush=SolidColor(style.color))
                Label(stringResource(R.string.number_range,formatNumber(q.min),formatNumber(q.max)),size=13,color=Tokens.muted)
                if(!valid) Label(stringResource(R.string.invalid_number),size=13,color=Tokens.danger)
                GameButton(stringResource(R.string.accept),enabled=valid,color=style.color,ink=style.ink) {
                    if(valid) { exact=parsed;position=RulerScale.position(q,parsed!!).toFloat();keyboard=false }
                }
            }
        }
    }
}

@Preview(widthDp=360,heightDp=800) @Composable fun SelectorPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { SelectorScreen(s.copy(match=s.match?.copy(guesses=listOf(emptyList()))),Actions()) }
