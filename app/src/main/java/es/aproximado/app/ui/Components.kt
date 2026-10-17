package es.aproximado.app.ui

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import es.aproximado.app.R
import es.aproximado.core.formatNumber
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable fun GameButton(label: String, modifier: Modifier = Modifier, color: Color = Tokens.players[0].color,
    ink: Color = Color.White, outline: Boolean = false, enabled: Boolean = true, detail: String? = null, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed) 4.dp else 0.dp, tween(80), label = "press")
    val feedback = LocalFeedback.current
    Box(modifier.fillMaxWidth().heightIn(min = if (detail == null) 64.dp else 80.dp)
        .alpha(if (enabled) 1f else .4f).semantics { contentDescription=label }
        .background(if (outline) Color.Transparent else color.copy(red = color.red * .75f,green = color.green * .75f,blue = color.blue * .75f), CircleShape)) {
        Column(Modifier.fillMaxWidth().offset { IntOffset(0,shift.roundToPx()) }.heightIn(min = if (detail == null) 60.dp else 76.dp)
            .clip(CircleShape).background(if (outline) Color.Transparent else color)
            .then(if (outline) Modifier.border(2.dp, Color.White.copy(alpha = .4f),CircleShape) else Modifier)
            .clickable(interactionSource = source,indication = null,enabled = enabled,role = Role.Button) {
                feedback(false); onClick()
            }.padding(horizontal = 16.dp,vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,verticalArrangement = Arrangement.Center) {
            Text(label,color = ink,fontFamily = Tokens.display,fontWeight = FontWeight.Black,fontSize = 20.sp,textAlign = TextAlign.Center)
            detail?.let { Text(it,color = ink.copy(alpha = .7f),fontSize = 13.sp,textAlign = TextAlign.Center) }
        }
    }
}

@Composable fun RoundButton(label: String, description: String, modifier: Modifier = Modifier, color: Color = Tokens.high, ink: Color = Color.White, enabled: Boolean = true, onClick: () -> Unit) {
    Box(modifier.sizeIn(minWidth = 48.dp,minHeight = 48.dp).clip(CircleShape).background(color)
        .alpha(if(enabled) 1f else .4f).clickable(enabled = enabled,role = Role.Button,onClick = onClick)
        .semantics { contentDescription = description },contentAlignment = Alignment.Center) {
        Text(label,color = ink,fontSize = 26.sp,fontWeight = FontWeight.Bold)
    }
}

@Composable fun TopBar(title: String, back: Boolean = false, close: Boolean = false, onAction: () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().heightIn(min=64.dp)) {
        Text(title,Modifier.align(Alignment.Center).padding(horizontal = 60.dp, vertical = 16.dp),color = Color.White,fontSize = 16.sp,fontWeight = FontWeight.SemiBold,textAlign = TextAlign.Center,maxLines = 3)
        if(back || close) RoundButton(if(back) "‹" else "×",stringResource(if(back) R.string.back else R.string.close),
            Modifier.padding(horizontal=8.dp).align(if(back) Alignment.CenterStart else Alignment.CenterEnd),Color.Transparent,onClick = onAction)
    }
}

@Composable fun RulerCanvas(modifier: Modifier = Modifier, offset: Float = 0f) {
    Canvas(modifier) {
        val gap = 12.dp.toPx()
        val start = floor(offset / gap).toInt()
        val remainder = offset % gap
        for(i in -1..(size.height/gap).toInt()+1) {
            val y = i * gap + remainder
            val index = i - start
            val major = index % 10 == 0
            val length = (if(major) 56 else if(index % 5 == 0) 36 else 20).dp.toPx()
            val edge = (min(y,size.height-y)/24.dp.toPx()).coerceIn(0f,1f)
            val color = Color.White.copy(alpha = (if(major) .85f else .55f)*edge)
            drawLine(color,Offset(0f,y),Offset(length,y),2.dp.toPx(),StrokeCap.Round)
            drawLine(color,Offset(size.width-length,y),Offset(size.width,y),2.dp.toPx(),StrokeCap.Round)
        }
    }
}

@Composable fun BigNumber(text: String, modifier: Modifier = Modifier, style: PlayerStyle = Tokens.players[0], maxSize: Int = 96, glass: Boolean = false) {
    val density = LocalDensity.current
    val context = LocalContext.current
    val typeface = remember { context.resources.getFont(R.font.fredoka_bold) }
    val paint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { this.typeface = typeface; strokeJoin = Paint.Join.ROUND } }
    Canvas(modifier.fillMaxWidth().height(120.dp).semantics { contentDescription = text }) {
        with(density) {
            paint.textSize = maxSize.sp.toPx()
            val available = (size.width - 12.dp.toPx()).coerceAtLeast(1f)
            var displayText=text
            if(paint.measureText(text)>0 && paint.textSize*available/paint.measureText(text)<36.sp.toPx()) {
                val numeric=text.replace(".","").replace(",",".").toDoubleOrNull()
                if(numeric!=null) displayText=formatNumber(numeric,compact=true)
            }
            if(paint.measureText(displayText) > available) paint.textSize *= available/paint.measureText(displayText)
            // At extreme accessibility scales the measured fit takes priority over the nominal 36sp minimum.
            val x = (size.width - paint.measureText(displayText))/2
            val y = size.height/2 - (paint.ascent()+paint.descent())/2
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (if(glass) 4 else 6).dp.toPx()
            paint.color = (if(glass) Color.White.copy(alpha = .7f) else style.inkLight).toArgb()
            drawContext.canvas.nativeCanvas.drawText(displayText,x,y,paint)
            paint.style = Paint.Style.FILL
            paint.color = (if(glass) Color.White.copy(alpha = .25f) else style.ink).toArgb()
            drawContext.canvas.nativeCanvas.drawText(displayText,x,y,paint)
        }
    }
}

@Composable fun PlayerAvatar(name: String, style: PlayerStyle, size: Dp = 48.dp, pale: Boolean = false) {
    Box(Modifier.size(size).clip(CircleShape).background(if(pale) Color.White.copy(alpha = .25f) else style.color)
        .semantics { contentDescription = name },contentAlignment = Alignment.Center) {
        Text(name.take(1).uppercase(),color = style.ink,fontFamily = Tokens.display,fontWeight = FontWeight.Black,fontSize = if(size >= 100.dp) 56.sp else 20.sp)
    }
}

@Composable fun ValuePill(text: String, modifier: Modifier = Modifier, small: Boolean = false) {
    Box(modifier.heightIn(min = 40.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xE61F222B)).padding(horizontal = if(small) 4.dp else 12.dp,vertical = 8.dp),contentAlignment = Alignment.Center) {
        Text(text,color = Color.White,fontFamily = Tokens.display,fontWeight = FontWeight.Black,fontSize = if(small) 12.sp else 18.sp,maxLines = 1)
    }
}

@Composable fun Crown(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.winner)
    Canvas(modifier.size(28.dp).semantics { contentDescription = description }) {
        val p = Path().apply { moveTo(size.width*.1f,size.height*.25f);lineTo(size.width*.32f,size.height*.48f);lineTo(size.width*.5f,size.height*.08f);lineTo(size.width*.68f,size.height*.48f);lineTo(size.width*.9f,size.height*.25f);lineTo(size.width*.8f,size.height*.85f);lineTo(size.width*.2f,size.height*.85f);close() }
        drawPath(p,Tokens.gold)
    }
}

@Composable fun Confetti(modifier: Modifier = Modifier, continuous: Boolean = false) {
    val progress = remember { Animatable(0f) }
    val particles = remember { val r = Random(42); List(60) { floatArrayOf(r.nextFloat(),r.nextFloat(),r.nextFloat(),r.nextFloat()) } }
    LaunchedEffect(continuous) { do { progress.snapTo(0f);progress.animateTo(1f,tween(1800,easing = LinearEasing));if(continuous) delay(500) } while(continuous) }
    Canvas(modifier) {
        val t = progress.value
        particles.forEachIndexed { i,p ->
            val x = size.width/2 + (p[0]-.5f)*size.width*2*t
            val y = size.height*.2f - (120+p[1]*300)*t + size.height*t*t
            rotate(p[2]*720*t,Offset(x,y)) {
                drawOval((if(i%9==0) Color.White else Tokens.players[i%8].color).copy(alpha=(1-t).coerceIn(0f,1f)),Offset(x,y),Size((4+p[2]*6).dp.toPx(),(4+p[3]*4).dp.toPx()))
            }
        }
    }
}

@Composable fun GameSwitch(checked: Boolean, label: String, onChange: () -> Unit) {
    val enabledText = stringResource(if(checked) R.string.active else R.string.inactive)
    val x by animateDpAsState(if(checked) 20.dp else 0.dp,tween(150),label="toggle")
    Box(Modifier.size(56.dp,48.dp).semantics { contentDescription=label;stateDescription=enabledText }
        .toggleableCompat(checked,onChange),contentAlignment=Alignment.CenterStart) {
        Box(Modifier.size(52.dp,32.dp).clip(CircleShape).background(if(checked) Tokens.players[0].color else Tokens.high)) {
            Box(Modifier.offset { IntOffset((4.dp+x).roundToPx(),4.dp.roundToPx()) }.size(24.dp).clip(CircleShape).background(Color.White))
        }
    }
}
private fun Modifier.toggleableCompat(checked: Boolean, action: () -> Unit): Modifier = this.then(Modifier.clickable(role=Role.Switch,onClick=action).semantics { toggleableState=if(checked) androidx.compose.ui.state.ToggleableState.On else androidx.compose.ui.state.ToggleableState.Off })

@Preview @Composable private fun ButtonPreview() = AproximadoTheme { GameButton("Jugar") {} }
@Preview @Composable private fun TopBarPreview() = AproximadoTheme { TopBar("Nueva partida",back=true) }
@Preview(widthDp=360,heightDp=300) @Composable private fun RulerPreview() = AproximadoTheme { Box(Modifier.background(Tokens.bg)) { RulerCanvas(Modifier.fillMaxSize()) } }
@Preview @Composable private fun BigPreview() = AproximadoTheme { BigNumber("1.400.000.000") }
@Preview @Composable private fun AvatarPreview() = AproximadoTheme { PlayerAvatar("Nico",Tokens.players[0]) }
@Preview @Composable private fun PillPreview() = AproximadoTheme { ValuePill("55.000") }
@Preview(widthDp=360,heightDp=300) @Composable private fun ConfettiPreview() = AproximadoTheme { Confetti(Modifier.fillMaxSize()) }
@Preview @Composable private fun CrownPreview() = AproximadoTheme { Crown() }
@Preview @Composable private fun SwitchPreview() = AproximadoTheme { GameSwitch(true,"Sonido") {} }
