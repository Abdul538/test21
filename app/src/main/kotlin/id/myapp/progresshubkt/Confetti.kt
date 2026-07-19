package id.myapp.progresshubkt

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiPiece(
    val startX: Float,
    val driftX: Float,
    val fallDelay: Float,
    val size: Float,
    val spinSpeed: Float,
    val color: Color
)

private val confettiColors = listOf(
    Color(0xFF5FB3A3), Color(0xFF4E9BE0), Color(0xFFE0A94E),
    Color(0xFFE0C64E), Color(0xFFFF6B9A), Color(0xFF9B6BFF)
)

private fun randomConfetti(count: Int): List<ConfettiPiece> = List(count) {
    ConfettiPiece(
        startX = Random.nextFloat(),
        driftX = (Random.nextFloat() - 0.5f) * 0.5f,
        fallDelay = Random.nextFloat() * 0.25f,
        size = 5f + Random.nextFloat() * 6f,
        spinSpeed = 1.5f + Random.nextFloat() * 3f,
        color = confettiColors.random()
    )
}

/** A short celebratory burst — small rectangles falling + spinning from
 * just above the top of the screen, fading out as they fall. Purely
 * decorative (no pointerInput), so it never blocks taps/scroll on whatever
 * it's layered over. Re-plays whenever [trigger] changes to a new nonzero
 * value (start it at 0 and bump it to celebrate). */
@Composable
fun ConfettiOverlay(trigger: Int, modifier: Modifier = Modifier) {
    if (trigger == 0) return
    key(trigger) {
        val pieces = remember { randomConfetti(46) }
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) {
            progress.animateTo(1f, animationSpec = tween(1500, easing = LinearEasing))
        }
        val t = progress.value
        if (t < 1f) {
            Canvas(modifier = modifier.fillMaxSize()) {
                pieces.forEach { p ->
                    val local = ((t - p.fallDelay) / (1f - p.fallDelay)).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEach
                    val fall = 1f - (1f - local) * (1f - local) // ease-out
                    val x = (p.startX + p.driftX * fall) * size.width
                    val y = -30f + fall * (size.height * 0.62f)
                    val alpha = (1f - local).coerceIn(0f, 1f)
                    rotate(degrees = p.spinSpeed * fall * 360f, pivot = Offset(x, y)) {
                        drawRect(
                            color = p.color.copy(alpha = alpha),
                            topLeft = Offset(x - p.size / 2f, y - p.size / 4f),
                            size = Size(p.size, p.size * 0.5f)
                        )
                    }
                }
            }
        }
    }
}
