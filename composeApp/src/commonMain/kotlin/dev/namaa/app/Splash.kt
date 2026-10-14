package dev.namaa.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val Ink = Color(0xFF120F0A)
private val Cream = Color(0xFFF5F1E8)
private val Orange = Color(0xFFFF5A1F)

/** The opening screen: a ring is drawn, the Zimam dot settles inside it, then the name and "by Zimam" fade in. */
@Composable
fun Splash(onDone: () -> Unit) {
    val moon = remember { Animatable(0f) }
    val pop = remember { Animatable(0f) }
    val name = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { moon.animateTo(1f, tween(600)) }
        launch { delay(450); pop.animateTo(1f, tween(300)) }
        launch { delay(650); name.animateTo(1f, tween(380)) }
        delay(1300)
        onDone()
    }
    Column(Modifier.fillMaxSize().background(Ink), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        BrandMark(Modifier.size(150.dp).graphicsLayer { val k = 0.88f + 0.12f * moon.value; scaleX = k; scaleY = k; alpha = moon.value })
        Spacer(Modifier.height(32.dp))
        Text("Namaa", Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Cream, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("by Zimam", Modifier.alpha(name.value).offset(y = ((1f - name.value) * 14f).dp), color = Orange, fontSize = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.sp)
    }
}
