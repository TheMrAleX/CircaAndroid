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

@Composable fun RevealScreen(state: AppState,actions: Actions,onExit: ()->Unit = {}) {
    val m=state.match ?: return;val q=state.question() ?: return
    val scores=remember(m.currentGuesses,q) { Scoring.round(q,m.currentGuesses) }
    val ordered=remember(scores) { scores.sortedBy { it.guess } }
    val inspection=LocalInspectionMode.current
    val timeline=remember { Animatable(if(inspection) 3400f else 0f) };val scope=rememberCoroutineScope();val feedback=LocalFeedback.current
    LaunchedEffect(q.id) { timeline.animateTo(3400f,tween(3400,easing=LinearEasing)) }
    val time=timeline.value
    LaunchedEffect(time>=1400) { if(time>=1400) feedback(true) }
    ScreenColumn {
        TopBar(stringResource(R.string.answer_title,m.round+1,m.config.rounds),close=true,onAction=onExit)
        // Long questions remain readable through vertical scrolling rather than truncation.
        Box(Modifier.fillMaxWidth().heightIn(min=44.dp,max=76.dp).verticalScroll(rememberScrollState()).padding(horizontal=32.dp,vertical=4.dp)) { Label(q.text,Modifier.fillMaxWidth(),13,Tokens.muted,center=true) }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().clickable(onClickLabel=stringResource(R.string.skip_animation)) { scope.launch { timeline.snapTo(3400f) } }) {
            val density=LocalDensity.current
            val height=with(density) { maxHeight.toPx() };val width=with(density) { maxWidth.toPx() }
            val values=ordered.map { if(q.scale=="log") ln(it.guess) else it.guess }+if(q.scale=="log") ln(q.answer) else q.answer
            val minValue=values.min();val maxValue=values.max();val spread=(maxValue-minValue).coerceAtLeast(if(q.scale=="log") .1 else max(abs(q.answer)*.1,1.0))
            val lo=minValue-spread*.15;val hi=maxValue+spread*.15
            val top=48.dp;val plotHeight=(maxHeight-200.dp).coerceAtLeast(40.dp)
            fun y(value: Double): Dp { val v=if(q.scale=="log") ln(value) else value;return top+plotHeight*(1-((v-lo)/(hi-lo)).toFloat()).coerceIn(0f,1f) }
            val slot=(maxWidth-24.dp)/ordered.size
            val narrow=slot<72.dp
            RulerCanvas(Modifier.fillMaxSize())
            // Major labels use the same transformed scale as the bars.
            Column(Modifier.fillMaxWidth().padding(top=top),verticalArrangement=Arrangement.SpaceBetween) {
                (0..4).forEach { index ->
                    val raw=hi-(hi-lo)*index/4
                    val number=if(q.scale=="log") exp(raw) else raw
                    Label(formatNumber(number,true),Modifier.height(plotHeight/4),11,Color.White.copy(alpha=.5f))
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                ordered.reversed().forEach { score ->
                    val index=ordered.indexOf(score)
                    val raw=((time-200-score.playerId*120)/900).coerceIn(0f,1f)
                    val progress=FastOutSlowInEasing.transform(raw)
                    val target=with(density) { y(score.guess).toPx() }
                    val current=height+(target-height)*progress
                    val x=with(density) { (12.dp+slot*index).toPx() }
                    val barWidth=with(density) { (slot+12.dp).toPx() }
                    val style=Tokens.players[m.config.players[score.playerId].style]
                    drawRoundRect(style.color,Offset(x,current),Size(barWidth,height-current+40.dp.toPx()),CornerRadius(20.dp.toPx()))
                    if(score.rank==1 && time>=2800) drawRoundRect(Tokens.gold,Offset(x,current),Size(barWidth,height-current+40.dp.toPx()),CornerRadius(20.dp.toPx()),style=Stroke(3.dp.toPx()))
                    if(time>=2200) {
                        val center=x+barWidth/2
                        drawLine(style.inkLight,Offset(center,target),Offset(center,with(density) { y(q.answer).toPx() }),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(),4.dp.toPx())))
                    }
                }
            }
            // Stagger adjacent narrow labels if their cards intersect; all labels stay in the chart.
            data class Placement(val x: Dp,val y: Dp,val width: Dp,val height: Dp)
            val placements=mutableListOf<Placement>()
            ordered.forEachIndexed { index,score ->
                val labelWidth=if(narrow) 60.dp else (slot+8.dp).coerceAtMost(140.dp)
                val cardHeight=if(score.rank==1) 108.dp else 80.dp
                val x=(12.dp+slot*index+(slot-labelWidth)/2).coerceIn(2.dp,maxWidth-labelWidth-2.dp)
                val preferred=(y(score.guess)+8.dp).coerceAtMost(maxHeight-cardHeight)
                val answerY=y(q.answer)
                val candidates=(0..((maxHeight-cardHeight).value.toInt().coerceAtLeast(0)) step 4).map { it.dp }
                fun clearCards(candidate: Dp)=placements.none { old ->
                    x<old.x+old.width+2.dp && x+labelWidth+2.dp>old.x && candidate<old.y+old.height+4.dp && candidate+cardHeight+4.dp>old.y
                }
                val calculatedY=candidates.filter { clearCards(it) && (it+cardHeight<answerY-48.dp || it>answerY+48.dp) }
                    .minByOrNull { abs((it-preferred).value) }
                    ?: candidates.filter { clearCards(it) }.minByOrNull { abs((it-preferred).value) }
                    ?: preferred.coerceAtLeast(0.dp)
                val winningIndex=ordered.indexOfFirst { it.rank==1 }
                val topTier=(answerY+60.dp).coerceAtMost((maxHeight-188.dp).coerceAtLeast(0.dp))
                val labelY=if(narrow) topTier+if((index-winningIndex)%2==0) 0.dp else 108.dp else calculatedY
                placements.add(Placement(x,labelY,labelWidth,cardHeight))
                val progress=((time-200-score.playerId*120)/900).coerceIn(0f,1f)
                val player=m.config.players[score.playerId];val style=Tokens.players[player.style]
                if(progress>0) Column(Modifier.offset(x=x,y=labelY).width(labelWidth).alpha(progress),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(2.dp)) {
                    if(time>=2800 && score.rank==1) Crown()
                    ValuePill(formatNumber(score.guess*progress,narrow),small=narrow)
                    if(narrow) PlayerAvatar(player.name,style,32.dp) else Label(player.name,size=12,color=style.ink,bold=true,center=true)
                }
            }
            if(time>=1400) {
                val lineProgress=((time-1400)/500).coerceIn(0f,1f)
                Canvas(Modifier.fillMaxSize()) {
                    val lineY=with(density) { y(q.answer).toPx() }
                    drawLine(Color.White,Offset(0f,lineY),Offset(width*lineProgress,lineY),2.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(12.dp.toPx(),8.dp.toPx())))
                }
                Box(Modifier.offset(y=y(q.answer)-16.dp).clip(CircleShape).background(Color.White).padding(horizontal=8.dp,vertical=4.dp)) { Label(stringResource(R.string.answer),size=13,color=Tokens.bg,bold=true) }
                BigNumber(formatNumber(q.answer),Modifier.offset(y=y(q.answer)-60.dp).padding(horizontal=24.dp).alpha(lineProgress),maxSize=88,glass=true)
            }
            if(time>=2800) Confetti(Modifier.fillMaxSize())
        }
        // Differences and points use a compact scrollable strip to preserve legibility for eight players.
        if(time>=2200) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            scores.forEach { r -> val p=m.config.players[r.playerId];val diff=r.guess-q.answer
                Column(Modifier.clip(RoundedCornerShape(12.dp)).background(Tokens.players[p.style].color).padding(8.dp)) {
                    Label(p.name,size=12,color=Tokens.players[p.style].ink,bold=true)
                    Label("${if(diff>0) "+" else ""}${formatNumber(diff,true)} · +${r.points}",size=12,color=Tokens.players[p.style].ink,bold=true)
                    if(r.bonus>0) Label(stringResource(R.string.nailed),size=12,color=Tokens.players[p.style].ink,bold=true)
                }
            }
        }
        BottomActions {
            if(time>=3400) GameButton(stringResource(R.string.continue_game),color=Color.White,ink=Tokens.players[0].ink) { actions.advance(Screen.SCORE) }
            else Label(stringResource(R.string.skip_animation),Modifier.fillMaxWidth().height(64.dp),13,Tokens.muted,center=true)
        }
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
                                Label(standing.player.name,Modifier.padding(vertical=8.dp),16,bold=true,center=true)
                                Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(topStart=20.dp,topEnd=20.dp)).background(style.color),contentAlignment=Alignment.Center) {
                                    Column(horizontalAlignment=Alignment.CenterHorizontally) { Label(standing.rank.toString(),size=48,color=style.ink,bold=true);Label(androidx.compose.ui.res.pluralStringResource(R.plurals.points,standing.total,standing.total),size=16,color=style.ink,bold=true,center=true) }
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
