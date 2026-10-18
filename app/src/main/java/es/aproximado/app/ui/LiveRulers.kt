package es.aproximado.app.ui

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import es.aproximado.app.R
import es.aproximado.app.lan.LanUiState
import es.aproximado.core.*
import es.aproximado.core.lan.*

@Composable fun LivePreference(enabled: Boolean,onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Label(stringResource(R.string.live_title),Modifier.weight(1f),bold=true)
        GameSwitch(enabled,stringResource(R.string.live_title)) { onChange(!enabled) }
    }
}
@Composable fun LiveRoomPreference(state: LanUiState,onChange: (Boolean)->Unit) {
    LivePreference(state.liveRuler,onChange)
    Label(stringResource(when {
        !state.liveRuler -> R.string.live_off
        state.snapshot?.liveEnabled==true -> R.string.live_active
        else -> R.string.live_wait_consent
    }),size=13,color=Tokens.muted)
}

/** One fixed column per seat. No answer-dependent rescaling or floating value labels. */
@Composable fun SharedLiveBoard(state: LanUiState,modifier: Modifier=Modifier,ownValue: Double?=null,onPlotHeight: (Int)->Unit = {}) {
    val room=state.snapshot ?: return
    if(!state.liveRuler || !room.liveEnabled || room.stage!=LanStage.ANSWERING) return
    val q=room.prompt?.model() ?: return
    val values=state.liveValues+(room.self to (ownValue ?: room.mine ?: state.pending))
    Column(modifier,verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().onSizeChanged { onPlotHeight(it.height) }.clip(RoundedCornerShape(topStart=16.dp,topEnd=16.dp))) {
            RulerCanvas(Modifier.fillMaxSize())
            Row(Modifier.fillMaxSize().padding(horizontal=8.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                room.members.forEach { member -> key(member.id) {
                    val v=values[member.id]
                    val status=v?.let { formatEstimate(q,it) } ?: stringResource(R.string.live_pending)
                    val description=stringResource(R.string.live_movement,member.name,status)+if(member.submitted) " · "+stringResource(R.string.live_confirmed) else ""
                    SharedBar(member.id,v?.let { RulerScale.position(q,it).toFloat() },member.id==room.self,Modifier.weight(1f).fillMaxHeight().semantics { contentDescription=description })
                } }
            }
        }
        val columns=if(room.members.size<=4) room.members.size else 4
        Column(Modifier.fillMaxWidth().padding(horizontal=8.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            room.members.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    row.forEach { member ->
                        val style=Tokens.players[member.id];val self=member.id==room.self;val value=values[member.id]
                        val name=if(self) stringResource(R.string.lan_you,member.name) else member.name
                        val display=value?.let { if(formatEstimate(q,it).length>9) formatNumber(it,compact=true) else formatEstimate(q,it) } ?: "—"
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(Tokens.surface)
                            .then(if(self) Modifier.border(1.dp,style.inkLight,RoundedCornerShape(8.dp)) else Modifier).padding(horizontal=6.dp,vertical=4.dp)) {
                            Text("${member.id+1} ${if(member.submitted) "✓ " else ""}$name",color=style.inkLight,fontFamily=Tokens.display,fontWeight=FontWeight.Bold,fontSize=11.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
                            FittedLiveValue(display)
                        }
                    }
                    repeat(columns-row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** The local bar has no interpolation delay. Remote bars interpolate between coalesced frames. */
@Composable private fun SharedBar(id: Int,position: Float?,self: Boolean,modifier: Modifier) {
    val animated=if(self) rememberUpdatedState(position ?: 0f) else animateFloatAsState(position ?: 0f,tween(140,easing=FastOutSlowInEasing),label="opponent_bar")
    val style=Tokens.players[id]
    val context=LocalContext.current
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=context.resources.getFont(R.font.fredoka_bold);textAlign=Paint.Align.CENTER } }
    val density=LocalDensity.current
    Canvas(modifier) {
        val stroke=2.dp.toPx();val top=12.dp.toPx().coerceAtMost(size.height/3)
        val y=top+(1-animated.value)*(size.height-top-12.dp.toPx()).coerceAtLeast(0f)
        val rect=Size((size.width-stroke).coerceAtLeast(0f),(size.height-y).coerceAtLeast(0f))
        drawRoundRect(style.color.copy(alpha=if(position==null) .22f else .95f),Offset(stroke/2,y),rect,CornerRadius(12.dp.toPx()))
        if(self) drawRoundRect(style.inkLight,Offset(stroke/2,y),rect,CornerRadius(12.dp.toPx()),style=Stroke(stroke))
        val r=9.dp.toPx().coerceAtMost(size.width/2-1)
        val center=Offset(size.width/2,(y+14.dp.toPx()).coerceIn(r,(size.height-r).coerceAtLeast(r)))
        drawCircle(if(self) Color.White else style.ink,center=center,radius=r)
        paint.textSize=with(density) { 11.sp.toPx() };paint.color=(if(self) style.ink else Color.White).toArgb()
        drawContext.canvas.nativeCanvas.drawText((id+1).toString(),center.x,center.y-(paint.ascent()+paint.descent())/2,paint)
    }
}

/** Single-line fitted numbers, with full precision retained by accessibility. */
@Composable private fun FittedLiveValue(value: String) {
    val context=LocalContext.current;val density=LocalDensity.current
    val paint=remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface=context.resources.getFont(R.font.fredoka_bold);color=android.graphics.Color.WHITE } }
    Canvas(Modifier.fillMaxWidth().height(22.dp).semantics { contentDescription=value }) {
        paint.textSize=with(density) { 14.sp.toPx() }
        val width=paint.measureText(value)
        if(width>size.width && width>0) paint.textSize*=size.width/width
        drawContext.canvas.nativeCanvas.drawText(value,0f,size.height/2-(paint.ascent()+paint.descent())/2,paint)
    }
}
fun previewLiveLan(n: Int=3): LanUiState {
    val s=previewLan(n,LanStage.ANSWERING)
    val room=s.snapshot!!.copy(liveEnabled=true,members=s.snapshot.members.map { it.copy(submitted=it.id==0) },mine=6500.0)
    val q=room.prompt!!.model()
    return s.copy(snapshot=room,liveValues=room.members.drop(1).associate { it.id to EstimateInput.round(q,q.min+(q.max-q.min)*(it.id+1)/(n+1)) })
}
@Preview(widthDp=360,heightDp=420) @Composable fun SharedBoardPreview(@PreviewParameter(PreviewStates::class) s: es.aproximado.app.AppState)=AproximadoTheme { SharedLiveBoard(previewLiveLan(s.config.players.size),modifier=Modifier.fillMaxSize()) }
@Preview(widthDp=360) @Composable fun LivePreferencePreview()=AproximadoTheme { LivePreference(true) {} }
