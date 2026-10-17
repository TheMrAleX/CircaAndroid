package es.aproximado.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import es.aproximado.app.R

data class PlayerStyle(val color: Color, val ink: Color, val inkLight: Color)
object Tokens {
    val bg = Color(0xFF262A35)
    val surface = Color(0xFF313644)
    val high = Color(0xFF3B4152)
    val muted = Color.White.copy(alpha = .65f)
    val gold = Color(0xFFFFC83D)
    val danger = Color(0xFFFF5A4D)
    val display = FontFamily(Font(R.font.fredoka, FontWeight.Bold), Font(R.font.fredoka, FontWeight.SemiBold))
    val players = listOf(
        PlayerStyle(Color(0xFF2F7CF6),Color(0xFF0B2A66),Color(0xFF8DBBFF)),
        PlayerStyle(Color(0xFFF5A623),Color(0xFF5A3600),Color(0xFFFFD08A)),
        PlayerStyle(Color(0xFFFF5C8A),Color(0xFF5E0F2A),Color(0xFFFFA3BD)),
        PlayerStyle(Color(0xFF2ED47A),Color(0xFF0B4A28),Color(0xFF92F0BC)),
        PlayerStyle(Color(0xFF9B6BFF),Color(0xFF2E1470),Color(0xFFCDB5FF)),
        PlayerStyle(Color(0xFFFF5A4D),Color(0xFF5E140D),Color(0xFFFFA59E)),
        PlayerStyle(Color(0xFF22C7D6),Color(0xFF07454C),Color(0xFF8FEAF2)),
        PlayerStyle(Color(0xFFFFD93D),Color(0xFF4D3F00),Color(0xFFFFEC99))
    )
}
