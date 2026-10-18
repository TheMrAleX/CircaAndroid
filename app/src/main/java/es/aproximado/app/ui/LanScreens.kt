@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package es.aproximado.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import es.aproximado.app.*
import es.aproximado.app.R
import es.aproximado.app.lan.*
import es.aproximado.core.*
import es.aproximado.core.lan.*

class LanActions(val name: (String)->Unit = {},val endpoint: (String)->Unit = {},val code: (String)->Unit = {},val host: ()->Unit = {},val join: ()->Unit = {},val close: ()->Unit = {},val refresh: ()->Unit = {},val browse: ()->Unit = {},val stopBrowse: ()->Unit = {},val configure: (Int,Int?,Set<String>)->Unit = {_,_,_->},val start: ()->Unit = {},val advance: ()->Unit = {},val lobby: ()->Unit = {},val remove: (Int)->Unit = {},val submit: (Double,String,Int)->Unit = {_,_,_->})

@Composable private fun LanTopBar(title: String,back: Boolean=false,close: Boolean=false,onAction: ()->Unit) {
    Box(Modifier.fillMaxWidth().zIndex(2f).background(Tokens.bg)) { TopBar(title,back,close,onAction) }
}

@Composable fun LanField(label: String,value: String,type: KeyboardType = KeyboardType.Text,onChange: (String)->Unit) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Label(label,size=14,color=Tokens.muted)
        BasicTextField(value,onChange,Modifier.fillMaxWidth().heightIn(min=56.dp).background(Tokens.high,RoundedCornerShape(16.dp)).padding(16.dp).semantics { contentDescription=label },
            textStyle=TextStyle(color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=type),cursorBrush=SolidColor(Tokens.players[0].color))
    }
}
fun lanError(error: LanFailure): Int = when(error) {
    LanFailure.WRONG_CODE -> R.string.lan_error_code
    LanFailure.FULL -> R.string.lan_error_full
    LanFailure.STARTED -> R.string.lan_error_started
    LanFailure.INVALID -> R.string.lan_error_invalid
    LanFailure.CLOSED -> R.string.lan_error_closed
    LanFailure.VERSION -> R.string.lan_error_version
    LanFailure.ROOM_CHANGED -> R.string.lan_error_changed
    else -> R.string.lan_error_network
}
@Composable fun LanScreen(state: LanUiState,pool: List<Question>,actions: LanActions,onBack: ()->Unit) {
    var exit by remember { mutableStateOf(false) }
    var cancel by remember { mutableStateOf(false) }
    fun leave() { if(state.active) exit=true else onBack() }
    BackHandler { leave() }
    DisposableEffect(state.active) { if(!state.active) actions.browse();onDispose { actions.stopBrowse() } }
    val room=state.snapshot
    when {
        !state.active -> LanEntry(state,actions,onBack=onBack)
        state.connection!=LanConnection.CONNECTED || room==null -> ScreenColumn {
            LanTopBar(stringResource(R.string.lan_title),close=true) { leave() }
            Column(Modifier.weight(1f).fillMaxWidth().clipToBounds().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.Center) {
                Label(stringResource(if(state.connection==LanConnection.RECONNECTING) R.string.lan_reconnecting else R.string.lan_connecting),size=28,bold=true)
                Spacer(Modifier.height(20.dp));Label(stringResource(R.string.lan_reconnect_hint),color=Tokens.muted)
            }
            BottomActions { GameButton(stringResource(R.string.lan_leave),outline=true) { leave() } }
        }
        room.stage==LanStage.LOBBY -> LanLobby(state,pool,actions) { leave() }
        room.stage==LanStage.ANSWERING -> {
            if(room.mine!=null || state.pending!=null) LanWaiting(state,actions,{ leave() },{cancel=true})
            else key(room.gameId,room.round,room.self) {
                SelectorScreen(lanGameState(room),Actions(confirm={v,_,_->actions.submit(v,room.gameId,room.round)}),playerIndex=room.self) { leave() }
            }
        }
        else -> {
            val projected=lanGameState(room)
            val footer: @Composable ()->Unit = { BottomActions {
                if(state.host) GameButton(stringResource(when(room.stage) {
                    LanStage.REVEAL -> R.string.continue_game
                    LanStage.SCORE -> if(room.round+1==room.rounds) R.string.final_result else R.string.next_round
                    else -> R.string.again
                }),color=Color.White,ink=Tokens.players[0].ink,onClick=if(room.stage==LanStage.FINISHED) actions.lobby else actions.advance)
                else Label(stringResource(R.string.lan_wait_host_next),Modifier.fillMaxWidth(),14,Tokens.muted,center=true)
                if(room.stage!=LanStage.REVEAL) GameButton(stringResource(R.string.lan_leave),outline=true) { leave() }
            } }
            when(room.stage) {
                LanStage.REVEAL -> RevealScreen(projected,Actions(),onExit={leave()},footer=footer)
                LanStage.SCORE -> ScoreScreen(projected,Actions(),footer)
                LanStage.FINISHED -> FinalScreen(projected,Actions(),footer)
                else -> Unit
            }
        }
    }
    if(exit) GameDialog(stringResource(if(state.host) R.string.lan_close_title else R.string.lan_leave_title),stringResource(if(state.host) R.string.lan_close_body else R.string.lan_leave_body),stringResource(R.string.lan_stay),stringResource(R.string.lan_leave),onPrimary={exit=false},onSecondary={exit=false;actions.close();onBack()},onDismiss={exit=false})
    if(cancel) GameDialog(stringResource(R.string.lan_cancel_title),stringResource(R.string.lan_cancel_body),stringResource(R.string.keep_playing),stringResource(R.string.lan_back_lobby),onPrimary={cancel=false},onSecondary={cancel=false;actions.lobby()},onDismiss={cancel=false})
}

@Composable fun LanEntry(state: LanUiState,actions: LanActions,initialJoin: Boolean = state.endpoint.isNotBlank(),onBack: ()->Unit) {
    val context=LocalContext.current
    var joining by remember { mutableStateOf(initialJoin) }
    ScreenColumn(Modifier.imePadding()) {
        LanTopBar(stringResource(R.string.lan_title),back=true,onAction=onBack)
        Column(Modifier.weight(1f).clipToBounds().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Label(stringResource(R.string.lan_intro),size=14,color=Tokens.muted)
            state.error?.let { Label(stringResource(lanError(it)),color=Tokens.danger) }
            LanField(stringResource(R.string.lan_name),state.name,onChange=actions.name)
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Choice(stringResource(R.string.lan_host),!joining,Modifier.weight(1f)) { joining=false }
                Choice(stringResource(R.string.lan_join_title),joining,Modifier.weight(1f)) { joining=true }
            }
            if(!joining) Panel(stringResource(R.string.lan_host)) {
                Label(stringResource(R.string.lan_create_hint),size=14,color=Tokens.muted)
                GameButton(stringResource(R.string.lan_host),enabled=state.name.isNotBlank(),onClick=actions.host)
                GameButton(stringResource(R.string.lan_wifi_settings),outline=true) { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            }
            if(joining) Panel(stringResource(R.string.lan_join_title)) {
                LanField(stringResource(R.string.lan_address),state.endpoint,KeyboardType.Ascii,actions.endpoint)
                Label(stringResource(R.string.lan_address_hint),size=13,color=Tokens.muted)
                LanField(stringResource(R.string.lan_code),state.code,KeyboardType.NumberPassword,actions.code)
                GameButton(stringResource(R.string.lan_join),color=Color.White,ink=Tokens.players[0].ink,enabled=state.name.isNotBlank() && state.endpoint.isNotBlank() && state.code.length==8,onClick=actions.join)
            }
            if(joining) Panel(stringResource(R.string.lan_nearby)) {
                state.rooms.forEach { found ->
                    Box(Modifier.fillMaxWidth().heightIn(min=56.dp).background(if(state.endpoint==found.endpoint) Tokens.players[0].color else Tokens.high,RoundedCornerShape(16.dp)).semantics { selected=state.endpoint==found.endpoint }.clickable(role=Role.Button) { actions.endpoint(found.endpoint) }.padding(16.dp)) {
                        Label(stringResource(R.string.lan_found,found.name,found.endpoint),size=14)
                    }
                }
                Label(stringResource(R.string.lan_no_rooms),size=13,color=Tokens.muted)
                GameButton(stringResource(R.string.lan_search),outline=true,onClick=actions.browse)
            }
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
@Composable fun LanMembers(room: LanSnapshot,hostControls: Boolean=false,remove: (Int)->Unit = {}) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        room.members.forEach { member ->
            Row(Modifier.fillMaxWidth().heightIn(min=64.dp).background(Tokens.surface,RoundedCornerShape(20.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                PlayerAvatar(member.name,Tokens.players[member.id],40.dp)
                Column(Modifier.weight(1f)) {
                    Label(if(member.id==room.self) stringResource(R.string.lan_you,member.name) else member.name,size=17,bold=true)
                    Label(stringResource(when { !member.online -> R.string.lan_offline;member.submitted -> R.string.lan_submitted;room.stage==LanStage.ANSWERING -> R.string.lan_not_submitted;else -> R.string.lan_online }),size=12,color=if(!member.online) Tokens.danger else Tokens.muted)
                }
                if(hostControls && member.id!=0) RoundButton("×",stringResource(R.string.lan_remove,member.name),color=Tokens.high) { remove(member.id) }
            }
        }
    }
}
@Composable fun LanLobby(state: LanUiState,pool: List<Question>,actions: LanActions,scrollState: ScrollState = rememberScrollState(),onExit: ()->Unit) {
    val room=state.snapshot ?: return
    val available=pool.count { (room.categories.isEmpty() || it.category in room.categories) && (room.difficulty==null || it.difficulty==room.difficulty) }
    ScreenColumn {
        LanTopBar(stringResource(R.string.lan_room),close=true,onAction=onExit)
        Column(Modifier.weight(1f).clipToBounds().verticalScroll(scrollState).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            state.error?.let { Label(stringResource(lanError(it)),color=Tokens.danger) }
            if(state.host) Panel(stringResource(R.string.lan_share)) {
                SelectionContainer { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Label(stringResource(R.string.lan_code),size=13,color=Tokens.muted)
                    Label(state.code,size=32,bold=true)
                    if(state.addresses.isEmpty()) Label(stringResource(R.string.lan_no_address),size=14,color=Tokens.danger)
                    state.addresses.forEach { Label(it,size=17,bold=true) }
                } }
                Label(stringResource(R.string.lan_share_hint),size=13,color=Tokens.muted)
                GameButton(stringResource(R.string.lan_refresh),outline=true,onClick=actions.refresh)
            } else Label(stringResource(R.string.lan_wait_host),color=Tokens.muted)
            Label(stringResource(R.string.lan_members,room.members.size),size=22,bold=true)
            LanMembers(room,state.host,actions.remove)
            if(state.host) {
                Panel(stringResource(R.string.rounds)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(5,10,15,20).forEach { n -> Choice(n.toString(),room.rounds==n,Modifier.weight(1f)) { actions.configure(n,room.difficulty,room.categories) } } }
                }
                Panel(stringResource(R.string.difficulty)) {
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        listOf(null,1,2,3).forEach { d -> Choice(stringResource(when(d) { 1->R.string.easy;2->R.string.medium;3->R.string.hard;else->R.string.mixed }),room.difficulty==d) { actions.configure(room.rounds,d,room.categories) } }
                    }
                }
                Panel(stringResource(R.string.categories)) {
                    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Choice(stringResource(R.string.all),room.categories.isEmpty()) { actions.configure(room.rounds,room.difficulty,emptySet()) }
                        pool.map { it.category }.distinct().sorted().forEach { cat -> Choice("${emoji(cat)} $cat",cat in room.categories) {
                            actions.configure(room.rounds,room.difficulty,if(cat in room.categories) room.categories-cat else room.categories+cat)
                        } }
                    }
                    Label(stringResource(R.string.lan_room_rules,room.rounds,available),size=13,color=if(available<room.rounds) Tokens.danger else Tokens.muted)
                }
            } else Label(stringResource(R.string.round_of,1,room.rounds),size=14,color=Tokens.muted)
            Label(stringResource(R.string.lan_need_players),size=13,color=Tokens.muted)
        }
        BottomActions {
            if(state.host) GameButton(stringResource(R.string.start),enabled=room.members.size>=2 && room.members.all { it.online } && available>=room.rounds,onClick=actions.start)
            else GameButton(stringResource(R.string.lan_leave),outline=true,onClick=onExit)
        }
    }
}
@Composable fun LanWaiting(state: LanUiState,actions: LanActions,onExit: ()->Unit,onCancel: ()->Unit) {
    val room=state.snapshot ?: return
    ScreenColumn {
        LanTopBar(stringResource(R.string.lan_room),close=true,onAction=onExit)
        Column(Modifier.weight(1f).clipToBounds().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            PlayerAvatar(room.members[room.self].name,Tokens.players[room.self],48.dp)
            Label(stringResource(R.string.lan_waiting),size=28,bold=true)
            Label(stringResource(R.string.lan_waiting_hint),color=Tokens.muted)
            Label(stringResource(R.string.lan_waiting_count,room.members.count { it.submitted },room.members.size),size=18,bold=true)
            LanMembers(room)
            if(state.host) Label(stringResource(R.string.lan_cancel_hint),size=13,color=Tokens.muted)
        }
        BottomActions {
            if(state.host) GameButton(stringResource(R.string.lan_back_lobby),outline=true,onClick=onCancel)
            else GameButton(stringResource(R.string.lan_leave),outline=true,onClick=onExit)
        }
    }
}
/** A rendering projection only. The offline match/checkpoint remains independent. */
fun lanGameState(room: LanSnapshot): AppState {
    val questions=room.results.map { it.prompt.model(it.answer,it.source) }.toMutableList()
    if(room.stage==LanStage.ANSWERING) questions.add(room.prompt!!.model())
    val config=Config(room.members.map { Player(it.id,it.name,it.id) },room.rounds,room.categories,room.difficulty)
    val guesses=room.results.map { it.guesses }+if(room.stage==LanStage.ANSWERING) listOf(emptyList()) else emptyList()
    return AppState(screen=when(room.stage) { LanStage.REVEAL->Screen.REVEAL;LanStage.SCORE->Screen.SCORE;LanStage.FINISHED->Screen.FINAL;else->Screen.SELECT },pool=questions,config=config,
        match=Match(config,List(room.rounds){"${room.gameId}:$it"},room.round,guesses),loading=false)
}
fun previewLan(n: Int,stage: LanStage = LanStage.LOBBY,host: Boolean=true): LanUiState {
    val base=previewState(n);val q=base.pool.first();val prompt=LanPrompt.of("game:0",q)
    val room=LanSnapshot("room","game",1,if(host) 0 else 1,stage,base.config.players.map { LanMember(it.id,it.name,true,stage!=LanStage.LOBBY && it.id%2==0) },10,null,emptySet(),0,if(stage==LanStage.LOBBY) null else prompt,null,emptyList())
    return LanUiState(name="Nico",code="12345678",active=true,host=host,connection=LanConnection.CONNECTED,snapshot=room,addresses=listOf("192.168.43.1:45555"))
}
@Preview(widthDp=360,heightDp=800) @Composable fun LanEntryPreview(@PreviewParameter(PreviewStates::class) s: AppState)=AproximadoTheme { LanEntry(LanUiState(name=s.config.players.first().name),LanActions()) {} }
@Preview(widthDp=360,heightDp=800) @Composable fun LanLobbyPreview(@PreviewParameter(PreviewStates::class) s: AppState)=AproximadoTheme { LanLobby(previewLan(s.config.players.size),s.pool,LanActions()) {} }
@Preview(widthDp=360,heightDp=800) @Composable fun LanWaitingPreview(@PreviewParameter(PreviewStates::class) s: AppState)=AproximadoTheme { LanWaiting(previewLan(s.config.players.size,LanStage.ANSWERING),LanActions(),{},{}) }

@Preview(widthDp=360) @Composable fun LanFieldPreview()=AproximadoTheme { LanField(stringResource(R.string.lan_address),"192.168.43.1:45555") {} }
@Preview(widthDp=360,heightDp=800) @Composable fun LanReconnectPreview(@PreviewParameter(PreviewStates::class) s: AppState)=AproximadoTheme { LanScreen(previewLan(s.config.players.size).copy(connection=LanConnection.RECONNECTING),s.pool,LanActions()) {} }
