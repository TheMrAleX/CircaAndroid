package es.aproximado.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import es.aproximado.app.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ScreensTest(private val width: Int,private val height: Int,private val scale: Float,private val players: Int) {
    @get:Rule val paparazzi=Paparazzi(deviceConfig=DeviceConfig.PIXEL_5.copy(screenWidth=width,screenHeight=height,density=com.android.resources.Density.MEDIUM,fontScale=scale),theme="android:style/Theme.Material.NoActionBar",maxPercentDifference=0.1)
    private val state=previewState(players)
    private fun capture(name: String,content: @Composable ()->Unit) {
        paparazzi.snapshot("${name}_${width}x${height}_${scale}_${players}p") { CompositionLocalProvider(LocalInspectionMode provides true) { AproximadoTheme { Box(Modifier.background(Tokens.bg)) { content() } } } }
    }
    @Test fun home()=capture("home") { HomeScreen(state,Actions()) }
    @Test fun setup()=capture("setup") { SetupScreen(state,Actions()) }
    @Test fun intro()=capture("intro") { IntroScreen(state,Actions()) }
    @Test fun pass()=capture("pass") { PassScreen(state,Actions()) }
    @Test fun selector()=capture("selector") { SelectorScreen(state.copy(match=state.match!!.copy(guesses=listOf(emptyList()))),Actions()) }
    @Test fun reveal()=capture("reveal") { RevealScreen(state,Actions()) }
    @Test fun score()=capture("score") { ScoreScreen(state,Actions()) }
    @Test fun final()=capture("final") { FinalScreen(state,Actions()) }
    @Test fun settings()=capture("settings") { SettingsScreen(state,Actions()) }
    @Test fun hugeNumber()=capture("number_huge") { BigNumber("1.400.000.000",Modifier.padding(horizontal=24.dp)) }
    @Test fun decimalNumber()=capture("number_decimal") { BigNumber("0,5",Modifier.padding(horizontal=24.dp)) }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}x{1}@{2}-{3}p") fun params()=listOf(
            arrayOf<Any>(360,800,1f,2),arrayOf<Any>(360,800,1f,8),
            arrayOf<Any>(320,640,1.3f,8),arrayOf<Any>(411,914,1.3f,2)
        )
    }
}
