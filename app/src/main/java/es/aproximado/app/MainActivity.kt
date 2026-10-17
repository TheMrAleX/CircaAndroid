package es.aproximado.app

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import es.aproximado.app.ui.*

class MainActivity : ComponentActivity() {
    private var pool: SoundPool? = null
    private var click: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pool=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        click=pool!!.load(this,R.raw.tick,1)
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val haptics=LocalHapticFeedback.current
            val feedback: (Boolean)->Unit = { strong ->
                if(state.vibration) haptics.performHapticFeedback(if(strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                if(state.sound) pool?.play(click,if(strong) .25f else .12f,if(strong) .25f else .12f,1,0,if(strong) .8f else 1f)
            }
            SideEffect {
                if(state.screen==Screen.SELECT) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
            val actions=remember(vm) { Actions(vm::screen,vm::configure,vm::count,vm::name,vm::color,vm::start,vm::advance,vm::confirm,vm::nextRound,vm::resume,vm::setting,vm::clear) }
            CompositionLocalProvider(LocalFeedback provides feedback) { AproximadoTheme { AproximadoApp(state,actions) } }
        }
    }
    override fun onDestroy() { pool?.release();pool=null;super.onDestroy() }
}

@Composable fun AproximadoApp(state: AppState,actions: Actions) {
    var exitDialog by remember { mutableStateOf(false) }
    val inMatch=state.screen in listOf(Screen.INTRO,Screen.PASS,Screen.SELECT,Screen.REVEAL,Screen.SCORE,Screen.FINAL)
    BackHandler(state.screen!=Screen.HOME) {
        if(inMatch) exitDialog=true else actions.screen(Screen.HOME)
    }
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Label(stringResource(R.string.loading),Modifier.padding(24.dp),center=true) }
            state.error -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Label(stringResource(R.string.error_load),Modifier.padding(24.dp),center=true) }
            state.screen==Screen.SELECT || state.screen==Screen.PASS -> key(state.screen,state.match?.round,state.match?.turn) {
                // No outgoing private screen is retained for a crossfade or handoff.
                if(state.screen==Screen.SELECT) SelectorScreen(state,actions) { exitDialog=true } else PassScreen(state,actions)
            }
            else -> AnimatedContent(state.screen,transitionSpec={ (fadeIn(tween(300))+slideInVertically(tween(300)){24}) togetherWith (fadeOut(tween(150))+slideOutVertically(tween(300)){-24}) },label="screen") { screen ->
                when(screen) {
                    Screen.HOME -> HomeScreen(state,actions)
                    Screen.SETUP -> SetupScreen(state,actions)
                    Screen.INTRO -> IntroScreen(state,actions)
                    Screen.REVEAL -> RevealScreen(state,actions) { exitDialog=true }
                    Screen.SCORE -> ScoreScreen(state,actions)
                    Screen.FINAL -> FinalScreen(state,actions)
                    Screen.SETTINGS -> SettingsScreen(state,actions)
                    else -> Unit
                }
            }
        }
    }
    if(exitDialog) GameDialog(stringResource(R.string.exit_title),stringResource(R.string.saved_notice),stringResource(R.string.keep_playing),stringResource(R.string.exit),
        onPrimary={exitDialog=false},onSecondary={exitDialog=false;actions.screen(Screen.HOME)})
}
