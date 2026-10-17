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

/** Each result owns its labels and track: no floating text or overlapping columns. */
@Composable fun RevealScreen(state: AppState, actions: Actions, listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(), onExit: () -> Unit = {}) {
    val m = state.match ?: return
    val q = state.question() ?: return
    val scores = remember(m.currentGuesses, q) { Scoring.round(q, m.currentGuesses) }
    val scale = remember(q, m.currentGuesses) { ComparisonScale(q, m.currentGuesses) }
    val feedback = LocalFeedback.current
    LaunchedEffect(q.id) { feedback(true) }
    ScreenColumn {
        TopBar(stringResource(R.string.answer_title, m.round + 1, m.config.rounds), close = true, onAction = onExit)
        LazyColumn(Modifier.weight(1f), state = listState, contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Tokens.surface).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Label(q.text, size = 18, bold = true, center = true)
                    Spacer(Modifier.height(12.dp))
                    Label(stringResource(R.string.correct_answer), size = 13, color = Tokens.muted)
                    BigNumber(formatNumber(q.answer), Modifier.height(88.dp), maxSize = 60, glass = true)
                    q.unit?.let { Label(it, size = 16, color = Tokens.muted, center = true) }
                    q.year?.let { Label(stringResource(R.string.year, it), size = 13, color = Tokens.muted) }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Label(stringResource(R.string.round_results), size = 22, bold = true)
                    Label(stringResource(if (q.scale == "log") R.string.log_comparison else R.string.linear_comparison), size = 13, color = Tokens.muted)
                }
            }
            items(scores, key = { it.playerId }) { score ->
                val player = m.config.players[score.playerId]
                val style = Tokens.players[player.style]
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Tokens.surface)
                    .border(1.dp, if (score.rank == 1) Tokens.gold.copy(alpha = .75f) else Color.White.copy(alpha = .08f), RoundedCornerShape(20.dp))
                    .padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PlayerAvatar(player.name, style, 40.dp)
                        Column(Modifier.weight(1f)) {
                            Label(player.name, size = 18, bold = true)
                            if (score.rank == 1) Label(stringResource(R.string.closest), size = 13, color = Tokens.gold, bold = true)
                        }
                        Label(androidx.compose.ui.res.pluralStringResource(R.plurals.earned_points, score.points, score.points), size = 16, color = style.inkLight, bold = true)
                    }
                    // Stacked values can wrap at large font sizes without stealing width from names.
                    Label(stringResource(R.string.guess_value, formatNumber(score.guess)), size = 20, bold = true)
                    ComparisonTrack(scale.position(score.guess), scale.position(q.answer), style.color)
                    Label(stringResource(R.string.distance_value, formatNumber(abs(score.guess - q.answer))), size = 14, color = Tokens.muted)
                    if (score.bonus > 0) Label(stringResource(R.string.bonus_points, score.bonus), size = 13, color = Tokens.gold, bold = true)
                }
            }
        }
        BottomActions { GameButton(stringResource(R.string.continue_game), color = Color.White, ink = Tokens.players[0].ink) { actions.advance(Screen.SCORE) } }
    }
}

/** Same linear/log domain in every row. White marker = answer; colored dot = guess. */
@Composable private fun ComparisonTrack(guess: Float, answer: Float, color: Color) {
    Canvas(Modifier.fillMaxWidth().height(20.dp)) {
        val inset = 8.dp.toPx()
        val length = (size.width - 2 * inset).coerceAtLeast(0f)
        val y = size.height / 2
        val x = inset + length * guess
        val target = inset + length * answer
        drawLine(Tokens.high, Offset(inset, y), Offset(size.width - inset, y), 6.dp.toPx(), StrokeCap.Round)
        drawLine(color.copy(alpha = .6f), Offset(min(x,target), y), Offset(max(x,target), y), 6.dp.toPx(), StrokeCap.Round)
        drawLine(Color.White, Offset(target, 2.dp.toPx()), Offset(target, size.height - 2.dp.toPx()), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(Tokens.surface, 8.dp.toPx(), Offset(x,y))
        drawCircle(color, 5.dp.toPx(), Offset(x,y))
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
