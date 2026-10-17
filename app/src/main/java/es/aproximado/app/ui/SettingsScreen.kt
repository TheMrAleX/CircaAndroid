package es.aproximado.app.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import es.aproximado.app.*
import es.aproximado.app.R

@Composable fun GameDialog(title: String,body: String? = null,primary: String,secondary: String,onPrimary: ()->Unit,onSecondary: ()->Unit,onDismiss: ()->Unit = onSecondary) {
    Dialog(onDismissRequest=onDismiss) {
        Column(Modifier.fillMaxWidth().clipCompat().background(Tokens.surface).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Label(title,size=22,bold=true,center=true)
            body?.let { Label(it,color=Tokens.muted,center=true) }
            GameButton(primary,onClick=onPrimary)
            GameButton(secondary,outline=true,onClick=onSecondary)
        }
    }
}
private fun Modifier.clipCompat()=clip(RoundedCornerShape(28.dp)).border(1.dp,Color.White.copy(alpha=.08f),RoundedCornerShape(28.dp))

@Composable fun SettingsScreen(state: AppState,actions: Actions) {
    var clearing by remember { mutableStateOf(false) }
    ScreenColumn {
        TopBar(stringResource(R.string.settings),back=true) { actions.screen(Screen.HOME) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Panel(stringResource(R.string.experience)) {
                Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
                    Label(stringResource(R.string.sound),Modifier.weight(1f));GameSwitch(state.sound,stringResource(R.string.sound)) { actions.setting(!state.sound,null) }
                }
                Row(Modifier.fillMaxWidth().heightIn(min=64.dp),verticalAlignment=Alignment.CenterVertically) {
                    Label(stringResource(R.string.vibration),Modifier.weight(1f));GameSwitch(state.vibration,stringResource(R.string.vibration)) { actions.setting(null,!state.vibration) }
                }
                }
            Panel(stringResource(R.string.saved_data)) {
                Label(stringResource(R.string.history_hint),size=14,color=Tokens.muted)
                Box(Modifier.fillMaxWidth().heightIn(min=64.dp).clickable(role=Role.Button) { clearing=true }.padding(vertical=12.dp)) { Label(stringResource(R.string.clear_history),color=Tokens.danger) }
            }
            Panel(stringResource(R.string.about)) { Label(stringResource(R.string.about_text),color=Tokens.muted);Label(stringResource(R.string.rules),size=13,color=Tokens.muted) }
        }
    }
    if(clearing) GameDialog(stringResource(R.string.clear_title),primary=stringResource(R.string.clear_confirm),secondary=stringResource(R.string.cancel),onPrimary={actions.clear();clearing=false},onSecondary={clearing=false})
}
@Preview(widthDp=360,heightDp=800) @Composable fun SettingsPreview(@PreviewParameter(PreviewStates::class) s: AppState) = AproximadoTheme { SettingsScreen(s,Actions()) }
