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
    @Test fun nextPlayer()=capture("next_player") {
        val m=state.match!!.copy(config=state.config.copy(players=state.config.players.map { it.copy(name="Alejandra123") }),guesses=listOf(listOf(6500.0)))
        SelectorScreen(state.copy(match=m),Actions())
    }
    @Test fun preciseReference()=capture("precise_reference") {
        val q=state.pool.first().copy(text="¿Cuántos metros por segundo al cuadrado vale la gravedad estándar de referencia terrestre?",answer=9.80665,unit="m/s²",min=0.0,max=30.0,decimalPlaces=5)
        RevealScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(List(players){9.8+it/10.0}))),Actions())
    }
    @Test fun selector()=capture("selector") { SelectorScreen(state.copy(match=state.match!!.copy(guesses=listOf(emptyList()))),Actions()) }
    @Test fun reveal()=capture("reveal") { RevealScreen(state,Actions()) }
    @Test fun score()=capture("score") { ScoreScreen(state,Actions()) }
    @Test fun final()=capture("final") { FinalScreen(state,Actions()) }
    @Test fun settings()=capture("settings") { SettingsScreen(state,Actions()) }
    @Test fun hugeNumber()=capture("number_huge") { BigNumber("1.400.000.000",Modifier.padding(horizontal=24.dp)) }
    @Test fun decimalNumber()=capture("number_decimal") { BigNumber("0,5",Modifier.padding(horizontal=24.dp)) }
    @Test fun reportedOverlap()=capture("reported_overlap") {
        val q=state.pool.first().copy(text="¿Cuántas provincias tiene Argentina?",answer=23.0,unit=null,min=1.0,max=100.0)
        val guesses=List(players){if(it%2==0) 28.0 else 38.0}
        RevealScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(guesses))),Actions())
    }
    @Test fun tiedResultsLowerRows()=capture("tied_results_lower") {
        val q=state.pool.first().copy(answer=.5,unit=null,min=.001,max=100.0,scale="log",decimalPlaces=3)
        val m=state.match!!.copy(config=state.config.copy(players=state.config.players.map{it.copy(name="Alejandra123")}),guesses=listOf(List(players){.5}))
        RevealScreen(state.copy(pool=listOf(q),match=m),Actions())
    }
    @Test fun hugeLogResults()=capture("huge_log_results") {
        val q=state.pool.first().copy(answer=1.4e9,unit=null,min=1.0,max=1e12,scale="log")
        RevealScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(List(players){1.4e9*(it+1)}))),Actions())
    }
    @Test fun setupFilters()=capture("setup_filters") {
        SetupScreen(state,Actions(),listState=androidx.compose.foundation.lazy.LazyListState(3))
    }
    @Test fun selectorHuge()=capture("selector_huge") {
        val q=state.pool.first().copy(answer=1.4e9,min=1e8,max=2.7e9,unit=null)
        SelectorScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(emptyList()))),Actions())
    }
    @Test fun selectorDecimal()=capture("selector_decimal") {
        val q=state.pool.first().copy(answer=7.0,min=0.0,max=10.0,unit=null,decimalPlaces=2)
        SelectorScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(emptyList()))),Actions(),initialPosition=.332)
    }
    @Test fun selectorDecimalTrailingZero()=capture("selector_decimal_zero") {
        val q=state.pool.first().copy(answer=7.0,min=0.0,max=10.0,unit=null,decimalPlaces=2)
        SelectorScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(emptyList()))),Actions(),initialPosition=.34)
    }
    @Test fun selectorMinimum()=capture("selector_minimum") {
        SelectorScreen(state.copy(match=state.match!!.copy(guesses=listOf(emptyList()))),Actions(),initialPosition=0.0)
    }
    @Test fun selectorMaximum()=capture("selector_maximum") {
        SelectorScreen(state.copy(match=state.match!!.copy(guesses=listOf(emptyList()))),Actions(),initialPosition=1.0)
    }
    @Test fun closeEstimates()=capture("close_estimates") {
        val q=state.pool.first().copy(text="¿Cuántos metros mide esta longitud?",answer=10.0,min=0.0,max=30.0,unit="m",decimalPlaces=2)
        RevealScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(List(players){if(it%2==0) 9.99 else 8.99}))),Actions())
    }
    @Test fun zodiacIntegers()=capture("zodiac_integers") {
        val q=state.pool.first().copy(text="¿Cuántos signos tiene el zodiaco occidental tradicional?",answer=12.0,min=0.0,max=36.0,unit=null,decimalPlaces=0)
        SelectorScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(emptyList()))),Actions(),initialPosition=.333)
    }
    @Test fun preciseSelector()=capture("precise_selector") {
        val q=state.pool.first().copy(text="¿Cuántos metros por segundo al cuadrado vale la gravedad estándar de referencia terrestre?",answer=9.80665,unit="m/s²",min=0.0,max=30.0,decimalPlaces=5)
        SelectorScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(emptyList()))),Actions())
    }
    @Test fun fullReferenceCaption()=capture("full_reference_caption") {
        val q=state.pool.first().copy(text="¿Cuántos kilómetros cuadrados de superficie tiene Brasil según la referencia IBGE de 2015?",answer=8515767.049,unit="km²",min=10000.0,max=100000000.0,scale="log",decimalPlaces=3)
        RevealScreen(state.copy(pool=listOf(q),match=state.match!!.copy(guesses=listOf(List(players){8000000.0+it*100000.0}))),Actions())
    }
    @Test fun lanEntry()=capture("lan_entry") { LanEntry(es.aproximado.app.lan.LanUiState(name="Alejandra123"),LanActions()) {} }
    @Test fun lanLobbyHost()=capture("lan_lobby_host") { LanLobby(previewLan(players),state.pool,LanActions()) {} }
    @Test fun lanLobbyGuest()=capture("lan_lobby_guest") { LanLobby(previewLan(players,host=false),state.pool,LanActions()) {} }
    @Test fun lanWaiting()=capture("lan_waiting") { LanWaiting(previewLan(players,es.aproximado.core.lan.LanStage.ANSWERING),LanActions(),{},{}) }
    @Test fun lanReconnecting()=capture("lan_reconnecting") { LanScreen(previewLan(players).copy(connection=es.aproximado.core.lan.LanConnection.RECONNECTING),state.pool,LanActions()) {} }
    @Test fun lanOwnSelector()=capture("lan_own_selector") {
        val room=previewLan(players,es.aproximado.core.lan.LanStage.ANSWERING,host=false).snapshot!!
        SelectorScreen(lanGameState(room),Actions(),playerIndex=room.self)
    }
    @Test fun lanJoinForm()=capture("lan_join_form") { LanEntry(es.aproximado.app.lan.LanUiState(name="Alejandra123",endpoint="192.168.43.1:45555",code="12345678"),LanActions(),initialJoin=true) {} }
    @Test fun lanLobbyRoster()=capture("lan_lobby_roster") { LanLobby(previewLan(players),state.pool,LanActions(),scrollState=androidx.compose.foundation.ScrollState(560)) {} }
    @Test fun lanLobbyFilters()=capture("lan_lobby_filters") { LanLobby(previewLan(players),state.pool,LanActions(),scrollState=androidx.compose.foundation.ScrollState(1500)) {} }
    companion object {
        @JvmStatic @Parameterized.Parameters(name="{0}x{1}@{2}-{3}p") fun params()=listOf(
            arrayOf<Any>(360,800,1f,2),arrayOf<Any>(360,800,1f,8),
            arrayOf<Any>(320,640,1.3f,8),arrayOf<Any>(411,914,1.3f,2)
        )
    }
}
