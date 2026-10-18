@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
package es.aproximado.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.*
import es.aproximado.app.*
import es.aproximado.app.R
import es.aproximado.core.*

class Actions(
    val screen: (Screen)->Unit = {}, val config: ((Config)->Config)->Unit = {},
    val count: (Int)->Unit = {}, val name: (Int,String)->Unit = {_,_->},
    val color: (Int,Int)->Unit = {_,_->}, val start: (Boolean)->Unit = {},
    val advance: (Screen)->Unit = {}, val confirm: (Double,Int,Int)->Unit = {_,_,_->},
    val next: ()->Unit = {}, val resume: ()->Unit = {},
    val setting: (Boolean?,Boolean?)->Unit = {_,_->}, val clear: ()->Unit = {}
)
@Composable fun Label(text: String, modifier: Modifier = Modifier, size: Int = 16, color: Color = Color.White, bold: Boolean = false, center: Boolean = false) {
    Text(text,modifier,color=color,fontSize=size.sp,fontWeight=if(bold) FontWeight.ExtraBold else FontWeight.Medium,
        fontFamily=if(bold) Tokens.display else null,textAlign=if(center) TextAlign.Center else TextAlign.Start)
}
@Composable fun ScreenColumn(modifier: Modifier = Modifier, background: Color = Tokens.bg, content: @Composable ColumnScope.()->Unit) {
    Column(modifier.fillMaxSize().background(background).statusBarsPadding(),content=content)
}
@Composable fun Panel(title: String,content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Tokens.surface).border(1.dp,Color.White.copy(alpha=.06f),RoundedCornerShape(28.dp)).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Label(title,size=18,bold=true);content()
    }
}
@Composable fun BottomActions(content: @Composable ColumnScope.()->Unit) {
    Column(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Tokens.bg.copy(alpha=0f),Tokens.bg),endY=24f)).padding(horizontal=20.dp).navigationBarsPadding().padding(top=16.dp,bottom=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
}
fun emoji(category: String) = when(category) {
    "Geografía"->"🌎";"Historia"->"🏛";"Ciencia"->"⚗";"Cuerpo humano"->"🫀";"Animales"->"🐾";"Deportes"->"⚽";"Cine y música"->"🎵";"Tecnología"->"💻";"Comida"->"🍉";"Espacio"->"🪐";"Cultura general"->"📚";"Vida cotidiana"->"🏠";else->"✨"
}

@Composable fun HomeScreen(state: AppState,actions: Actions) {
    val transition=rememberInfiniteTransition(label="home")
    val offset by transition.animateFloat(0f,-1200f,infiniteRepeatable(tween(20000,easing=LinearEasing),RepeatMode.Restart),label="ruler")
    val inspection=LocalInspectionMode.current
    var shown by remember { mutableStateOf(inspection) }
    LaunchedEffect(Unit) { shown=true }
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        RulerCanvas(Modifier.fillMaxSize().alpha(.22f),offset)

        Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal=20.dp,vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            AnimatedVisibility(shown,enter=fadeIn(tween(400))+slideInVertically(spring(dampingRatio=.7f)){-40}) {
                BigNumber(stringResource(R.string.app_name),Modifier.fillMaxWidth(),maxSize=44)
            }
            Label(stringResource(R.string.party_caption),Modifier.padding(bottom=8.dp),13,Tokens.players[0].inkLight,bold=true,center=true)
            Label(stringResource(R.string.tagline),Modifier.padding(horizontal=24.dp),size=16,color=Tokens.muted,center=true)
            Spacer(Modifier.height(32.dp))
            GameButton(stringResource(R.string.play)) { actions.screen(Screen.SETUP) }
            state.match?.takeIf { it.stage!=Screen.FINAL }?.let { m ->
                Spacer(Modifier.height(12.dp))
                GameButton(stringResource(R.string.resume),color=Color.White,ink=Tokens.players[0].ink,detail=stringResource(R.string.resume_detail,m.round+1,m.config.rounds,m.config.players.size),onClick=actions.resume)
            }
            Spacer(Modifier.height(12.dp))
            GameButton(stringResource(R.string.settings),outline=true) { actions.screen(Screen.SETTINGS) }
        }
    }
}

@Composable fun Choice(text: String,selected: Boolean,modifier: Modifier=Modifier,onClick:()->Unit) {
    Box(modifier.heightIn(min=48.dp).clip(RoundedCornerShape(16.dp)).background(if(selected) Tokens.players[0].color else Tokens.high)
        .clickable(role=Role.RadioButton,onClick=onClick).semantics { this.selected=selected;contentDescription=text }.padding(horizontal=8.dp,vertical=10.dp),contentAlignment=Alignment.Center) {
        Label(text,size=14,bold=selected,center=true,color=if(selected) Color.White else Tokens.muted)
    }
}
@Composable fun SetupScreen(state: AppState,actions: Actions,listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState()) {
    val c=state.config
    var picking by remember { mutableStateOf<Int?>(null) }
    val categories=remember(state.pool) { state.pool.map { it.category }.distinct().sorted() }
    val available=state.pool.count { (c.categories.isEmpty() || it.category in c.categories) && (c.difficulty==null || it.difficulty==c.difficulty) }
    ScreenColumn(Modifier.imePadding()) {
        TopBar(stringResource(R.string.new_game),back=true) { actions.screen(Screen.HOME) }
        LazyColumn(Modifier.weight(1f),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { Panel(stringResource(R.string.players)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center) {
                    RoundButton("−",stringResource(R.string.less_players),Modifier.size(56.dp),enabled=c.players.size>2) { actions.count(c.players.size-1) }
                    Label(c.players.size.toString(),Modifier.padding(horizontal=24.dp),44,bold=true)
                    RoundButton("+",stringResource(R.string.more_players),Modifier.size(56.dp),ink=Tokens.players[0].color,enabled=c.players.size<8) { actions.count(c.players.size+1) }
                }
                Label(stringResource(R.string.two_eight),Modifier.fillMaxWidth(),13,Tokens.muted,center=true)
                Column(Modifier.animateContentSize(),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    c.players.forEach { p -> key(p.id) {
                        Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            val colorDescription=stringResource(R.string.choose_color,p.name.ifBlank { stringResource(R.string.player,p.id+1) })
                            Box(Modifier.size(48.dp).clickable(role=Role.Button) { picking=p.id }.semantics { contentDescription=colorDescription },contentAlignment=Alignment.Center) { PlayerAvatar(p.name.ifBlank { (p.id+1).toString() },Tokens.players[p.style],40.dp) }
                            Column(Modifier.weight(1f)) {
                                val nameLabel=stringResource(R.string.name_label,p.id+1)
                                BasicTextField(p.name,{actions.name(p.id,it)},Modifier.fillMaxWidth().heightIn(min=48.dp).semantics { contentDescription=nameLabel },singleLine=true,
                                    textStyle=TextStyle(color=Color.White,fontSize=18.sp,fontWeight=FontWeight.SemiBold),cursorBrush=androidx.compose.ui.graphics.SolidColor(Tokens.players[p.style].color),
                                    decorationBox={ inner -> Box(Modifier.padding(vertical=12.dp)) { if(p.name.isEmpty()) Label(stringResource(R.string.player,p.id+1),color=Tokens.muted,size=18);inner() } })
                                Box(Modifier.fillMaxWidth().height(2.dp).background(Tokens.players[p.style].color))
                            }
                        }
                    } }
                }
            } }
            item {
                Panel(stringResource(R.string.rounds)) {
                    Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                        listOf(5,10,15,20).forEach { n ->
                            Choice(n.toString(),c.rounds==n,Modifier.weight(1f)) {
                                actions.config { it.copy(rounds=n) }
                            }
                        }
                    }
                }
            }
            item { Panel(stringResource(R.string.categories)) {
                FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Choice(stringResource(R.string.all),c.categories.isEmpty()) { actions.config { it.copy(categories=emptySet()) } }
                    categories.forEach { category ->
                        Choice("${emoji(category)} $category",c.categories.isEmpty() || category in c.categories) {
                            actions.config { config ->
                                val now=if(config.categories.isEmpty()) categories.toSet() else config.categories
                                val next=if(category in now) now-category else now+category
                                config.copy(categories=if(next.isEmpty()) setOf(category) else next)
                            }
                        }
                    }
                }
                Label(androidx.compose.ui.res.pluralStringResource(R.plurals.available,available,available),size=13,color=Tokens.muted)
                if(available<c.rounds) Label(stringResource(R.string.insufficient),size=13,color=Tokens.danger)
            } }
            item { Panel(stringResource(R.string.difficulty)) {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.easy,R.string.medium,R.string.hard,R.string.mixed).chunked(2).forEachIndexed { row, labels -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { labels.forEachIndexed { col,res -> val i=row*2+col; Choice(stringResource(res),c.difficulty==(i+1).takeIf { it<=3 },Modifier.weight(1f)) { actions.config { it.copy(difficulty=(i+1).takeIf { d -> d<=3 }) } } } }
                }
                }
            } }
        }
        BottomActions { GameButton(stringResource(R.string.start),enabled=available>=c.rounds) { actions.start(false) } }
    }
    picking?.let { id ->
        ModalBottomSheet(onDismissRequest={picking=null},containerColor=Tokens.surface,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp)) {
            Label(stringResource(R.string.choose_color,c.players[id].name),Modifier.padding(horizontal=24.dp),22,bold=true)
            Column(Modifier.fillMaxWidth().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                (0..1).forEach { row -> Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly) {
                    (0..3).forEach { col -> val style=row*4+col;val used=c.players.any { it.id!=id && it.style==style }
                        RoundButton(if(c.players[id].style==style) "✓" else "",stringResource(if(used) R.string.color_used else R.string.color_number,style+1),Modifier.size(56.dp),Tokens.players[style].color,Tokens.players[style].ink,!used) { actions.color(id,style);picking=null }
                    }
                } }
            }
        }
    }
}

@Composable fun IntroScreen(state: AppState,actions: Actions) {
    val m=state.match ?: return;val q=state.question() ?: return
    val inspection=LocalInspectionMode.current
    val scale=remember { Animatable(if(inspection) 1f else .96f) };LaunchedEffect(q.id) { scale.animateTo(1f,tween(400)) }
    ScreenColumn {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(28.dp).graphicsLayer { scaleX=scale.value;scaleY=scale.value },horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
            Label(stringResource(R.string.round_of,m.round+1,m.config.rounds),size=13,color=Tokens.muted,center=true)
            Spacer(Modifier.height(24.dp));ValuePill("${emoji(q.category)} ${q.category}")
            Spacer(Modifier.height(32.dp));Label(q.text,size=26,bold=true,center=true)
            q.year?.let { Spacer(Modifier.height(12.dp));Label(stringResource(R.string.year,it),size=13,color=Tokens.muted) }
        }
        BottomActions { Label(stringResource(R.string.read_together),Modifier.fillMaxWidth(),13,Tokens.muted,center=true);GameButton(stringResource(R.string.start_round),color=Color.White,ink=Tokens.players[0].ink) { actions.advance(Screen.SELECT) } }
    }
}

class PreviewStates: PreviewParameterProvider<AppState> {
    override val values=sequenceOf(previewState(2),previewState(8))
}
fun previewState(n: Int = 2): AppState {
    val config=Config((0 until n).map { Player(it,listOf("Nico","Ana","Luz","Dani","Sol","Leo","Mar","Paz")[it]) },rounds=10)
    val q=Question("demo","Geografía","¿Cuántos metros mide aproximadamente el Aconcagua sobre el nivel del mar?",6960.8,"m",100.0,20000.0,"linear",2,decimalPlaces=1)
    val guesses=listOf(6500.0,4000.0,8500.0,5000.0,7100.0,9000.0,3000.0,6200.0).take(n)
    val categories=listOf("Geografía","Historia","Ciencia","Cuerpo humano","Animales","Deportes","Cine y música","Tecnología","Comida","Espacio","Cultura general","Vida cotidiana")
    val pool=listOf(q)+(1..399).map { q.copy(id="demo$it",category=categories[it%categories.size],difficulty=it%3+1) }
    return AppState(pool=pool,config=config,match=Match(config,List(10){"demo"},guesses=listOf(guesses),stage=Screen.REVEAL),loading=false)
}
@Preview(widthDp=360,heightDp=800) @Composable fun HomePreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { HomeScreen(s,Actions()) }
@Preview(widthDp=360,heightDp=800) @Composable fun SetupPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { SetupScreen(s,Actions()) }
@Preview(widthDp=360,heightDp=800) @Composable fun IntroPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { IntroScreen(s,Actions()) }
