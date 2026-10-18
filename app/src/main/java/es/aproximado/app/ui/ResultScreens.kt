package es.aproximado.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import es.aproximado.app.*
import es.aproximado.app.R
import es.aproximado.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/** A shared vertical chart remains the main result view for two to eight players. */
@Composable fun RevealScreen(state: AppState, actions: Actions, onExit: () -> Unit = {}) {
    val m=state.match ?: return
    val q=state.question() ?: return
    val scores=remember(m.currentGuesses,q) { Scoring.round(q,m.currentGuesses).sortedBy { it.guess } }
    val scale=remember(q,m.currentGuesses) { ComparisonScale(q,m.currentGuesses) }
    val inspection=LocalInspectionMode.current
    val legendWidth=if(LocalDensity.current.fontScale>1.15f) 192.dp else 148.dp
    val progress=remember(q.id) { Animatable(if(inspection) 1f else 0f) }
    val scope=rememberCoroutineScope()
    val feedback=LocalFeedback.current
    LaunchedEffect(q.id) { progress.animateTo(1f,tween(850,easing=FastOutSlowInEasing));feedback(true) }
    ScreenColumn {
        TopBar(stringResource(R.string.answer_title,m.round+1,m.config.rounds),close=true,onAction=onExit)
        Box(Modifier.fillMaxWidth().heightIn(max=144.dp).verticalScroll(rememberScrollState()).padding(horizontal=24.dp,vertical=4.dp)) {
            Label(q.text,Modifier.fillMaxWidth(),16,bold=true,center=true)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Label(stringResource(R.string.correct_answer),size=13,color=Tokens.muted)
                q.unit?.let { Label(it,size=13,color=Tokens.muted) }
                if(q.scale=="log") Label(stringResource(R.string.log_scale),size=12,color=Tokens.muted)
            }
            Column(Modifier.weight(1.5f),horizontalAlignment=Alignment.CenterHorizontally) {
                BigNumber(formatNumber(q.answer),Modifier.height(64.dp),maxSize=52,glass=true)
                if(formatNumber(q.answer).length>8) Label(formatNumber(q.answer),size=12,center=true)
            }
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal=12.dp).clipToBounds()
            .clickable(onClickLabel=stringResource(R.string.show_result)) { scope.launch { progress.snapTo(1f) } }) {
            val density=LocalDensity.current
            val axisWidth=56.dp
            val slot=(maxWidth-axisWidth)/scores.size
            val top=32.dp
            val bottom=(maxHeight-32.dp).coerceAtLeast(top+1.dp)
            val plotHeight=bottom-top
            fun y(value: Double)=top+plotHeight*(1-scale.position(value))
            RulerCanvas(Modifier.fillMaxSize().alpha(.3f))
            Canvas(Modifier.fillMaxSize()) {
                val baseline=with(density) { bottom.toPx() }
                scale.ticks.forEach { tick ->
                    val tickY=with(density) { y(tick).toPx() }
                    drawLine(Color.White.copy(alpha=.12f),Offset(with(density) { axisWidth.toPx() },tickY),Offset(size.width,tickY),1.dp.toPx())
                }
                scores.forEachIndexed { index,score ->
                    val style=Tokens.players[m.config.players[score.playerId].style]
                    val x=with(density) { (axisWidth+slot*index+3.dp).toPx() }
                    val width=with(density) { (slot-6.dp).toPx() }
                    val target=with(density) { y(score.guess).toPx() }
                    val barTop=baseline+(target-baseline)*progress.value
                    val height=(baseline-barTop).coerceAtLeast(0f)
                    drawRoundRect(style.color,Offset(x,barTop),Size(width,height),CornerRadius(14.dp.toPx()))
                    if(score.rank==1) drawRoundRect(Tokens.gold,Offset(x,barTop),Size(width,height),CornerRadius(14.dp.toPx()),style=Stroke(2.dp.toPx()))
                }
                val answerY=with(density) { y(q.answer).toPx() }
                drawLine(Color.White,Offset(with(density) { axisWidth.toPx() },answerY),Offset(size.width,answerY),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(),8.dp.toPx())))
            }
            scale.ticks.drop(1).forEach { tick ->
                Label(formatNumber(tick,true).replace(" ",""),Modifier.offset(y=(y(tick)-8.dp).coerceAtLeast(0.dp)).width(axisWidth).background(Tokens.bg).padding(end=4.dp),11,Tokens.muted)
            }
            Label(formatNumber(scale.minimum,true).replace(" ",""),Modifier.offset(y=bottom+4.dp).width(axisWidth).background(Tokens.bg),13,Tokens.muted)
            scores.forEachIndexed { index,score ->
                if(score.rank==1) Box(Modifier.offset(x=axisWidth+slot*index,y=(y(score.guess)-30.dp).coerceAtLeast(0.dp)).width(slot),contentAlignment=Alignment.Center) { Crown(Modifier.size(24.dp)) }
                if(scores.size<=4 && progress.value>=.95f) {
                    val labelY=(y(score.guess)+8.dp).coerceAtMost((bottom-44.dp).coerceAtLeast(0.dp))
                    Box(Modifier.offset(x=axisWidth+slot*index,y=labelY).width(slot).padding(horizontal=4.dp),contentAlignment=Alignment.Center) {
                        ValuePill(formatNumber(score.guess,true),small=scores.size>2)
                    }
                }
                Box(Modifier.offset(x=axisWidth+slot*index,y=bottom+4.dp).width(slot),contentAlignment=Alignment.Center) {
                    Label((score.playerId+1).toString(),size=13,color=Tokens.players[m.config.players[score.playerId].style].inkLight,bold=true)
                }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=8.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            scores.forEach { score ->
                val player=m.config.players[score.playerId]
                val style=Tokens.players[player.style]
                Column(Modifier.width(legendWidth).clip(RoundedCornerShape(16.dp)).background(Tokens.surface)
                    .border(1.dp,if(score.rank==1) Tokens.gold else style.color.copy(alpha=.4f),RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Label(stringResource(R.string.bar_player,player.id+1,player.name),size=14,color=style.inkLight,bold=true)
                    Label(formatNumber(score.guess,true),size=20,bold=true)
                    Label(androidx.compose.ui.res.pluralStringResource(R.plurals.earned_points,score.points,score.points),size=14,color=style.inkLight,bold=true)
                    if(score.bonus>0) Label(stringResource(R.string.bonus_points,score.bonus),size=12,color=Tokens.gold)
                }
            }
        }
        BottomActions { GameButton(stringResource(R.string.continue_game),color=Color.White,ink=Tokens.players[0].ink) { actions.advance(Screen.SCORE) } }
    }
}

@Composable fun ScoreRow(standing: Standing,modifier: Modifier=Modifier,bonus: Int = 0,compact: Boolean = false) {
    val p=standing.player;val style=Tokens.players[p.style]
    val inspection=LocalInspectionMode.current
    val target=remember { Animatable((if(inspection) standing.total else standing.total-bonus).toFloat()) }
    LaunchedEffect(standing.total) { target.animateTo(standing.total.toFloat(),tween(600)) }
    Row(modifier.fillMaxWidth().heightIn(min=if(compact) 64.dp else 72.dp).clip(RoundedCornerShape(20.dp)).background(Tokens.surface),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.width(6.dp).height(if(compact) 64.dp else 72.dp).background(style.color))
        Label(standing.rank.toString(),Modifier.padding(horizontal=8.dp),24,if(standing.rank==1) Tokens.gold else Color.White,bold=true)
        PlayerAvatar(p.name,style,if(compact) 40.dp else 48.dp)
        Column(Modifier.weight(1f).padding(horizontal=8.dp)) {
            Text(p.name,color=Color.White,fontSize=if(compact) 16.sp else 18.sp,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Clip)
            if(bonus>0) Label(stringResource(R.string.round_points,bonus),size=13,color=style.inkLight,bold=true)
        }
        Label(target.value.roundToInt().toString(),Modifier.padding(end=12.dp),28,bold=true)
    }
}
@Composable fun ScoreScreen(state: AppState,actions: Actions) {
    val m=state.match ?: return;val rounds=state.scores()
    val standings=Scoring.standings(m.config.players,rounds)
    ScreenColumn {
        TopBar(stringResource(R.string.score_title,m.round+1,m.config.rounds))
        Label(stringResource(R.string.total_points_hint), Modifier.padding(horizontal=20.dp,vertical=8.dp), 14, Tokens.muted)
        LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(standings,key={it.player.id}) { standing -> ScoreRow(standing,Modifier.animateItem(),bonus=rounds.lastOrNull()?.firstOrNull { it.playerId==standing.player.id }?.points ?: 0,compact=m.config.players.size>6) }
        }
        BottomActions { GameButton(stringResource(if(m.round+1==m.config.rounds) R.string.final_result else R.string.next_round),color=Color.White,ink=Tokens.players[0].ink,onClick=actions.next) }
    }
}

@Composable fun FinalScreen(state: AppState,actions: Actions) {
    val m=state.match ?: return
    val standings=Scoring.standings(m.config.players,state.scores())
    val first=standings.take(3)
    val ordered=if(first.size==2) listOf(first[1],first[0]) else listOf(first[1],first[0],first[2])
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        Confetti(Modifier.fillMaxSize(),continuous=true)
        ScreenColumn(background=Color.Transparent) {
            TopBar(stringResource(R.string.final_title))
            Label(stringResource(R.string.final_subtitle), Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp), 16, Tokens.muted, center=true)
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top=24.dp,bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.Bottom) {
                        ordered.forEach { standing ->
                            val style=Tokens.players[standing.player.style]
                            val height=when(standing.rank) { 1->160.dp;2->120.dp;else->90.dp }
                            val inspection=LocalInspectionMode.current
                            val progress=remember { Animatable(if(inspection) 1f else 0f) }
                            LaunchedEffect(Unit) { delay((3-standing.rank).coerceAtLeast(0)*500L);progress.animateTo(1f,spring(dampingRatio=.7f)) }
                            Column(Modifier.weight(1f).graphicsLayer { translationY=(1-progress.value)*200 },horizontalAlignment=Alignment.CenterHorizontally) {
                                if(standing.rank==1) Crown() else Spacer(Modifier.height(28.dp))
                                PlayerAvatar(standing.player.name,style,56.dp)
                                Label(standing.player.name,Modifier.fillMaxWidth().padding(vertical=12.dp),14,bold=true,center=true)
                                Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(topStart=20.dp,topEnd=20.dp)).background(style.color),contentAlignment=Alignment.Center) {
                                    Column(horizontalAlignment=Alignment.CenterHorizontally) { Label(standing.rank.toString(),size=48,color=style.ink,bold=true);Label(androidx.compose.ui.res.pluralStringResource(R.plurals.points,standing.total,standing.total),size=14,color=style.ink,bold=true,center=true) }
                                }
                            }
                        }
                    }
                }
                items(standings.drop(3),key={it.player.id}) { ScoreRow(it,compact=true) }
                item { Label(stringResource(R.string.rules),size=13,color=Tokens.muted,center=true) }
            }
            BottomActions { GameButton(stringResource(R.string.again)) { actions.start(true) };GameButton(stringResource(R.string.menu),outline=true) { actions.screen(Screen.HOME) } }
        }
    }
}

@Preview(widthDp=360,heightDp=800) @Composable fun RevealPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { RevealScreen(s,Actions()) }
@Preview(widthDp=360,heightDp=800) @Composable fun ScorePreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { ScoreScreen(s,Actions()) }
@Preview(widthDp=360,heightDp=800) @Composable fun FinalPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { FinalScreen(s,Actions()) }
