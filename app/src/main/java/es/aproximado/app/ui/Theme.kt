package es.aproximado.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable fun AproximadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(primary = Tokens.players[0].color, background = Tokens.bg,
            surface = Tokens.surface, onBackground = Color.White, onSurface = Color.White),
        typography = Typography(bodyLarge = TextStyle(fontSize = 16.sp,fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontFamily = Tokens.display,fontSize = 22.sp,fontWeight = FontWeight.ExtraBold)),
        content = content
    )
}
