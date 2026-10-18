package es.aproximado.app

import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import es.aproximado.app.lan.*
import es.aproximado.core.lan.*
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
    private var openLan by mutableStateOf(false)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        openLan=intent.getBooleanExtra("lan",false)
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(android.graphics.Color.TRANSPARENT))
        pool=SoundPool.Builder().setMaxStreams(2).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        click=pool!!.load(this,R.raw.tick,1)
        setContent {
            val vm: GameViewModel = viewModel()
            val state by vm.state.collectAsStateWithLifecycle()
            val lanVm: LanViewModel = viewModel()
            val lan by lanVm.state.collectAsStateWithLifecycle()
            val repo=lanVm.repository
            val notificationPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { /* Optional: LAN works when denied. */ }
            LaunchedEffect(lan.active) {
                val preferences=getSharedPreferences("aproximado-lan",0)
                if(lan.active && Build.VERSION.SDK_INT>=33 && !preferences.getBoolean("asked_notifications",false) && ContextCompat.checkSelfPermission(this@MainActivity,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
                    preferences.edit { putBoolean("asked_notifications",true) }
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            LaunchedEffect(openLan,state.loading) { if(openLan && !state.loading) { vm.screen(Screen.LAN);openLan=false } }
            LaunchedEffect(state.loading,state.liveRuler) { if(!state.loading) repo.livePreference(state.liveRuler) }
            val lanActions=remember(repo,state.pool) { LanActions(repo::name,repo::endpoint,repo::code,{repo.host(state.pool)},repo::join,repo::close,repo::refresh,repo::browse,repo::stopBrowsing,repo::configure,repo::start,repo::advance,repo::lobby,repo::remove,repo::submit,repo::preview,{ enabled -> vm.liveRuler(enabled);repo.livePreference(enabled) }) }
            val haptics=LocalHapticFeedback.current
            val feedback: (Boolean)->Unit = { strong ->
                if(state.vibration) haptics.performHapticFeedback(if(strong) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove)
                if(state.sound) pool?.play(click,if(strong) .25f else .12f,if(strong) .25f else .12f,1,0,if(strong) .8f else 1f)
            }
            SideEffect {
                if(state.screen==Screen.SELECT || state.screen==Screen.PASS || state.screen==Screen.LAN && lan.snapshot?.stage==LanStage.ANSWERING) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                if(lan.active && state.screen==Screen.LAN) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            val actions=remember(vm) { Actions(vm::screen,vm::configure,vm::count,vm::name,vm::color,vm::start,vm::advance,{ value,round,turn -> vm.confirm(value,round,turn) },vm::nextRound,vm::resume,vm::setting,vm::clear,{ enabled -> vm.liveRuler(enabled);repo.livePreference(enabled) }) }
            CompositionLocalProvider(LocalFeedback provides feedback) { AproximadoTheme { AproximadoApp(state,actions,lan,lanActions) } }
        }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent);if(intent.getBooleanExtra("lan",false)) openLan=true }
    override fun onDestroy() { pool?.release();pool=null;super.onDestroy() }
}

@Composable fun AproximadoApp(state: AppState,actions: Actions,lan: LanUiState = LanUiState(),lanActions: LanActions = LanActions()) {
    var exitDialog by remember { mutableStateOf(false) }
    val inMatch=state.screen in listOf(Screen.INTRO,Screen.PASS,Screen.SELECT,Screen.REVEAL,Screen.SCORE,Screen.FINAL)
    BackHandler(state.screen!=Screen.HOME && state.screen!=Screen.LAN) {
        if(inMatch) exitDialog=true else actions.screen(Screen.HOME)
    }
    Box(Modifier.fillMaxSize().background(Tokens.bg)) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Label(stringResource(R.string.loading),Modifier.padding(24.dp),center=true) }
            state.error -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { Label(stringResource(R.string.error_load),Modifier.padding(24.dp),center=true) }
            state.screen==Screen.LAN -> LanScreen(lan,state.pool,lanActions) { actions.screen(Screen.HOME) }
            state.screen==Screen.SELECT || state.screen==Screen.PASS -> key(state.screen,state.match?.round,state.match?.turn) {
                // No outgoing private screen is retained for a crossfade or handoff.
                SelectorScreen(state,actions) { exitDialog=true }
            }
            else -> AnimatedContent(state.screen,transitionSpec={ (fadeIn(tween(300))+slideInVertically(tween(300)){24}) togetherWith (fadeOut(tween(150))+slideOutVertically(tween(300)){-24}) },label="screen") { screen ->
                when(screen) {
                    Screen.HOME -> HomeScreen(state,actions)
                    Screen.SETUP -> SetupScreen(state,actions)
                    Screen.INTRO -> IntroScreen(state,actions)
                    Screen.REVEAL -> RevealScreen(state,actions,onExit={ exitDialog=true })
                    Screen.SCORE -> ScoreScreen(state,actions)
                    Screen.FINAL -> FinalScreen(state,actions)
                    Screen.SETTINGS -> SettingsScreen(state,actions)
                    else -> Unit
                }
            }
        }
    }
    if(exitDialog) GameDialog(stringResource(R.string.exit_title),stringResource(R.string.saved_notice),stringResource(R.string.keep_playing),stringResource(R.string.exit),
        onPrimary={exitDialog=false},onSecondary={exitDialog=false;actions.screen(Screen.HOME)},onDismiss={exitDialog=false})
}
