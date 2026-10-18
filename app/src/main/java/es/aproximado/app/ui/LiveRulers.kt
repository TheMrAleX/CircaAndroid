package es.aproximado.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.tooling.preview.Preview
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
    val room=state.snapshot
    Label(stringResource(when {
        !state.liveRuler -> R.string.live_off
        room!=null && room.members.size>=4 -> R.string.live_large_room
        room?.liveEnabled==true -> R.string.live_active
        else -> R.string.live_wait_consent
    }),size=13,color=Tokens.muted)
}

/** Read-only drafts. Names/values remain outside the moving plot, so they never collide. */
@Composable fun LiveRules(state: LanUiState,compact: Boolean=false,onExpand: ()->Unit = {}) {
    val room=state.snapshot ?: return
    if(!state.liveRuler || !room.liveEnabled || room.stage!=LanStage.ANSWERING || room.members.size !in 2..3) return
    val q=room.prompt?.model() ?: return
    Row(Modifier.fillMaxWidth().then(if(compact) Modifier.padding(horizontal=20.dp,vertical=4.dp) else Modifier),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        room.members.filter { it.id!=room.self }.forEach { member ->
            val style=Tokens.players[member.id]
            val value=state.liveValues[member.id].takeIf { !member.submitted }
            val status=when { member.submitted -> stringResource(R.string.live_confirmed);value==null -> stringResource(R.string.live_pending);else -> if(formatEstimate(q,value).length>10) formatNumber(value,compact=true) else formatEstimate(q,value) }
            val description=stringResource(R.string.live_movement,member.name,status)
            Column(Modifier.weight(1f).clip(RoundedCornerShape(if(compact) 12.dp else 20.dp)).background(Tokens.surface)
                .then(if(compact) Modifier.clickable(role=Role.Button,onClickLabel=stringResource(R.string.live_view),onClick=onExpand) else Modifier)
                .semantics(mergeDescendants=true) { contentDescription=description }
                .padding(if(compact) 8.dp else 12.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    if(!compact) PlayerAvatar(member.name,style,32.dp)
                    Label(member.name,Modifier.weight(1f),if(compact) 12 else 14,style.inkLight,bold=true)
                }
                if(compact) {
                    Row(Modifier.heightIn(min=26.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        LiveRulePlot(q,value,style,Modifier.width(24.dp).height(26.dp))
                        Label(status,Modifier.weight(1f),14,bold=true)
                    }
                } else {
                    LiveRulePlot(q,value,style,Modifier.fillMaxWidth().height(164.dp))
                    Label(status,Modifier.fillMaxWidth(),18,bold=true,center=true)
                    q.unit?.let { Label(it,Modifier.fillMaxWidth(),12,Tokens.muted,center=true) }
                }
            }
        }
    }
}

/** Fixed ticks; only the colored boundary moves. No network or composition work per animation frame. */
@Composable fun LiveRulePlot(q: Question,value: Double?,style: PlayerStyle,modifier: Modifier = Modifier) {
    val target=if(value==null) .5f else RulerScale.position(q,value).toFloat()
    val position=animateFloatAsState(target,tween(140,easing=FastOutSlowInEasing),label="opponent_rule")
    Canvas(modifier.clip(RoundedCornerShape(8.dp)).background(Tokens.bg)) {
        val edge=8.dp.toPx().coerceAtMost(size.height/4)
        val y=edge+(1-position.value)*(size.height-2*edge)
        val tint=if(value==null) Tokens.high else style.color
        drawRect(tint,Offset(0f,y),Size(size.width,size.height-y))
        val gap=12.dp.toPx()
        for(i in 0..(size.height/gap).toInt()) {
            val tickY=i*gap
            val length=(if(i%5==0) 12 else 6).dp.toPx().coerceAtMost(size.width/3)
            drawLine(Color.White.copy(alpha=.5f),Offset(0f,tickY),Offset(length,tickY),1.dp.toPx())
            drawLine(Color.White.copy(alpha=.5f),Offset(size.width-length,tickY),Offset(size.width,tickY),1.dp.toPx())
        }
        drawLine(if(value==null) Tokens.muted else style.inkLight,Offset(0f,y),Offset(size.width,y),2.dp.toPx())
    }
}
fun previewLiveLan(n: Int=3): LanUiState {
    val s=previewLan(n,LanStage.ANSWERING)
    val room=s.snapshot!!.copy(liveEnabled=n in 2..3,members=s.snapshot.members.map { it.copy(submitted=it.id==0) },mine=6500.0)
    val q=room.prompt!!.model()
    return s.copy(snapshot=room,liveValues=if(room.liveEnabled) room.members.drop(1).associate { it.id to EstimateInput.round(q,q.min+(q.max-q.min)*if(it.id==1) .32 else .74) } else emptyMap())
}
@Preview(widthDp=320,fontScale=1.3f) @Composable fun LiveRulesPreview()=AproximadoTheme { LiveRules(previewLiveLan()) }
@Preview(widthDp=320,fontScale=1.3f) @Composable fun LiveStripPreview()=AproximadoTheme { LiveRules(previewLiveLan(),compact=true) }
@Preview(widthDp=360) @Composable fun LivePreferencePreview()=AproximadoTheme { LivePreference(true) {} }
