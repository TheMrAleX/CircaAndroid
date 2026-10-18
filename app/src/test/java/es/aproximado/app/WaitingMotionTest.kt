package es.aproximado.app

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalInspectionMode
import es.aproximado.app.ui.*
import org.junit.Rule
import org.junit.Test

/** Exercise the live animation clock, separately from the fixed-phase screen previews. */
class WaitingMotionTest {
    @get:Rule val paparazzi=Paparazzi(deviceConfig=DeviceConfig.PIXEL_5.copy(screenWidth=160,screenHeight=160,density=com.android.resources.Density.MEDIUM),theme="android:style/Theme.Material.NoActionBar",maxPercentDifference=0.1)
    @Test fun waitingOrbitMovesAcrossACompleteCycle() {
        val view=ComposeView(paparazzi.context).apply {
            setContent {
                CompositionLocalProvider(LocalInspectionMode provides false) {
                    AproximadoTheme { Box(Modifier.fillMaxSize().background(Tokens.bg),contentAlignment=Alignment.Center) { WaitingOrbit("Ana",Tokens.players[1]) } }
                }
            }
        }
        try { paparazzi.gif(view,"waiting_orbit",start=0,end=2200,fps=10) }
        finally { view.disposeComposition() }
    }
}
