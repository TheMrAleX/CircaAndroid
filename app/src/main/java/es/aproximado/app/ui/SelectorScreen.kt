@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package es.aproximado.app.ui

import android.os.SystemClock
import androidx.compose.animation.core.*
import androidx.compose.animation.rememberSplineBasedDecay
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
import androidx.compose.ui.layout.onSizeChanged
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
import kotlinx.coroutines.Job
import kotlin.math.*

val LocalFeedback = staticCompositionLocalOf<(Boolean)->Unit> { {} }

@Composable fun SelectorScreen(state: AppState,actions: Actions,initialPosition: Double? = null,onExit: ()->Unit = {}) {
    val m=state.match ?: return;val q=state.question() ?: return
    val p=m.config.players[m.turn];val style=Tokens.players[p.style]
    val scope=rememberCoroutineScope()
    val density=LocalDensity.current
    val feedback=LocalFeedback.current
    var position by remember(q.id,m.turn) { mutableFloatStateOf((initialPosition ?: RulerScale.initial(q)).toFloat()) }
    var exact by remember(q.id,m.turn) { mutableStateOf<Double?>(null) }
    var keyboard by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }
    var lastTick by remember { mutableLongStateOf(0L) }
    var lastMajor by remember { mutableIntStateOf(-1) }
    val fling=remember { Animatable(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }
    val leaving=remember { Animatable(0f) }
    val decay=rememberSplineBasedDecay<Float>()
    val value=exact ?: RulerScale.snapped(q,position.toDouble())
    val display=formatEstimate(q,value)
    val reference=remember(q) { formatEstimate(q,q.max).replace(Regex("[0-9]"),"8") }
    var controlsHeight by remember { mutableIntStateOf(0) }
    fun setPosition(next: Float) {
        if(submitted) return
        position=next.coerceIn(0f,1f);exact=null
        val major=(position*100).toInt()
        if(major!=lastMajor && SystemClock.uptimeMillis()-lastTick>=40) {
            lastMajor=major;lastTick=SystemClock.uptimeMillis();feedback(false)
        }
    }
    fun fine(direction: Int) {
        flingJob?.cancel()
        val step=EstimateInput.fineStep(q)
        val next=EstimateInput.round(q,(value+direction*step).coerceIn(q.min,q.max))
        position=RulerScale.position(q,next).toFloat();exact=next;feedback(false)
    }
    ScreenColumn {
        TopBar(stringResource(R.string.selector_title,p.name,m.round+1,m.config.rounds),close=true,onAction=onExit)
        Box(Modifier.fillMaxWidth().heightIn(max=144.dp).verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=8.dp)) {
            Label(q.text,Modifier.fillMaxWidth(),16,bold=true,center=true)
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            val controlSize=if(controlsHeight>0) with(density) { controlsHeight.toDp() } else 212.dp
            val upper=60.dp
            val lower=(maxHeight-controlSize-68.dp).coerceAtLeast(upper+1.dp)
            val travel=lower-upper
            val travelPx=with(density) { travel.toPx() }
            val panelTop=lower-travel*position
            val description=stringResource(R.string.estimate_accessibility,display)
            Box(Modifier.fillMaxSize()
                .semantics { contentDescription=description;setProgress { progress -> flingJob?.cancel();setPosition(progress);true };progressBarRangeInfo=ProgressBarRangeInfo(position,0f..1f) }
                .draggable(rememberDraggableState { delta -> setPosition(RulerScale.drag(position.toDouble(),delta.toDouble(),travelPx.toDouble()).toFloat()) },Orientation.Vertical,
                    enabled=!submitted,onDragStarted={ flingJob?.cancel();fling.stop() },onDragStopped={ velocity ->
                        flingJob=scope.launch {
                            fling.snapTo(position);fling.updateBounds(0f,1f)
                            fling.animateDecay(-velocity/travelPx,decay) { setPosition(this.value) }
                        }
                    })) {
                // The ticks never scroll. The colored boundary tracks the selected position.
                RulerCanvas(Modifier.fillMaxSize().alpha(.65f))
                Box(Modifier.offset { IntOffset(0,panelTop.roundToPx()) }.fillMaxSize()
                    .graphicsLayer { translationY=leaving.value*size.height }
                    .clip(RoundedCornerShape(topStart=32.dp,topEnd=32.dp)).background(style.color))
                BigNumber(display,Modifier.offset { IntOffset(0,(panelTop-60.dp).roundToPx()) }
                    .padding(horizontal=24.dp).graphicsLayer { translationY=leaving.value*size.height },
                    style,maxSize=80,referenceText=reference)
                if(q.unit!=null || display.length>10) Column(Modifier.offset { IntOffset(0,(panelTop+48.dp).roundToPx()) }.fillMaxWidth().padding(horizontal=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    if(display.length>10) Label(formatNumber(value),size=13,color=style.ink,center=true)
                    q.unit?.let { Label(it,size=14,color=style.ink,center=true) }
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { controlsHeight=it.height }
                    .graphicsLayer { translationY=leaving.value*size.height }.navigationBarsPadding().padding(20.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Label(stringResource(R.string.drag_hint),Modifier.fillMaxWidth(),13,style.ink,center=true)
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        RoundButton("−",stringResource(R.string.decrease),color=style.ink.copy(alpha=.15f),ink=style.ink,enabled=!submitted) { fine(-1) }
                        Box(Modifier.weight(1f).heightIn(min=48.dp).clip(RoundedCornerShape(16.dp)).background(style.ink.copy(alpha=.12f))
                            .clickable(enabled=!submitted,role=Role.Button) { flingJob?.cancel();input=formatNumber(value).replace(".","");keyboard=true }
                            .padding(horizontal=8.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
                            Label(stringResource(R.string.keyboard),size=14,color=style.ink,bold=true,center=true)
                        }
                        RoundButton("+",stringResource(R.string.increase),color=style.ink.copy(alpha=.15f),ink=style.ink,enabled=!submitted) { fine(1) }
                    }
                    GameButton(stringResource(R.string.confirm),color=Color.White,ink=style.ink,enabled=!submitted) {
                        if(!submitted) {
                            flingJob?.cancel()
                            val confirmed=value
                            submitted=true;feedback(true)
                            scope.launch { leaving.animateTo(1f,tween(250));actions.confirm(confirmed) }
                        }
                    }
                }
            }
        }
    }
    if(keyboard) {
        val parsed=input.trim().replace(" ","").replace(",",".").toDoubleOrNull()
        val valid=parsed!=null && EstimateInput.accepts(q,parsed)
        ModalBottomSheet(onDismissRequest={keyboard=false},containerColor=Tokens.surface,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
            Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                Label(stringResource(R.string.numeric_title),size=22,bold=true)
                val estimateLabel=stringResource(R.string.number_hint)
                BasicTextField(input,{input=it.take(24)},Modifier.fillMaxWidth().heightIn(min=64.dp).semantics { contentDescription=estimateLabel },singleLine=true,
                    textStyle=TextStyle(color=Color.White,fontSize=44.sp,fontWeight=FontWeight.Black,fontFamily=Tokens.display),
                    keyboardOptions=KeyboardOptions(keyboardType=if(q.decimalPlaces==0) KeyboardType.Number else KeyboardType.Decimal),cursorBrush=SolidColor(style.color))
                Label(stringResource(R.string.number_range,formatNumber(q.min),formatNumber(q.max)),size=13,color=Tokens.muted)
                Label(if(q.decimalPlaces==0) stringResource(R.string.integer_only) else androidx.compose.ui.res.pluralStringResource(R.plurals.decimal_precision,q.decimalPlaces,q.decimalPlaces),size=13,color=Tokens.muted)
                if(!valid) Label(stringResource(R.string.invalid_number),size=13,color=Tokens.danger)
                GameButton(stringResource(R.string.accept),enabled=valid,color=style.color,ink=style.ink) {
                    if(valid) { exact=parsed;position=RulerScale.position(q,parsed!!).toFloat();keyboard=false }
                }
            }
        }
    }
}

@Preview(widthDp=360,heightDp=800) @Composable fun SelectorPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { SelectorScreen(s.copy(match=s.match?.copy(guesses=listOf(emptyList()))),Actions()) }
