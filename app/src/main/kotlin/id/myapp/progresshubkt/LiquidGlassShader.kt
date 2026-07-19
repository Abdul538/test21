package id.myapp.progresshubkt

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asComposeRenderEffect

/**
 * True per-pixel refraction, not a drawn approximation: near the edges of
 * the pane, sampled UV coordinates are pushed inward toward the surface
 * normal (here, toward the card's center) proportional to how close to the
 * edge they are — the same "lensing" a real slab of glass does to whatever
 * sits behind it, exaggerated as it would be along a bevelled edge. The
 * red and blue channels are sampled with a slightly different offset than
 * green, which is what produces a real chromatic-fringe rainbow at the
 * rim instead of a painted-on one.
 *
 * This is genuinely closer to how Apple's Liquid Glass works (real-time
 * backdrop sampling + lensing) than the earlier CPU-drawn sweep-gradient
 * rim — but it is not pixel-for-pixel identical to iOS's implementation.
 * iOS runs on a completely different rendering stack (Core
 * Animation/Metal, with additional dynamics tied to specular
 * lighting/device motion), and there's no way to compare this against a
 * real iOS device to tune it to match exactly. This is the closest
 * equivalent achievable with Android's public shader API (AGSL
 * RuntimeShader), which itself only exists on Android 13+ — devices below
 * that fall back to the earlier drawn-rim approximation, not this.
 */
private const val LIQUID_GLASS_AGSL = """
uniform shader content;
uniform float2 size;
uniform float refractPx;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord;
    float distL = uv.x;
    float distR = size.x - uv.x;
    float distT = uv.y;
    float distB = size.y - uv.y;
    float edgeDist = min(min(distL, distR), min(distT, distB));

    float lensWidth = 46.0;
    float t = clamp(1.0 - edgeDist / lensWidth, 0.0, 1.0);
    t = t * t;

    float2 center = size * 0.5;
    float2 toCenter = center - uv;
    float len = length(toCenter) + 0.0001;
    float2 dir = toCenter / len;

    float bend = t * refractPx;
    float2 offset = -dir * bend;

    half4 baseColor = content.eval(uv + offset);
    half4 colorR = content.eval(uv + offset + dir * bend * 0.35);
    half4 colorB = content.eval(uv + offset - dir * bend * 0.35);

    half4 outColor = half4(colorR.r, baseColor.g, colorB.b, baseColor.a);

    float rim = smoothstep(0.5, 1.0, t) * 0.35;
    outColor.rgb = outColor.rgb + half3(rim, rim, rim);

    return outColor;
}
"""

/** Returns a real GPU refraction RenderEffect sized to [sizePx], or null on
 * API < 33 or if shader compilation fails for any reason (e.g. a GPU
 * driver that doesn't support AGSL) — callers should treat null as "skip
 * this layer, fall back to whatever's underneath" rather than crash. */
@Composable
fun rememberLiquidGlassEffect(sizePx: Size, refractPx: Float = 10f): androidx.compose.ui.graphics.RenderEffect? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
    if (sizePx.width <= 0f || sizePx.height <= 0f) return null
    return remember(sizePx, refractPx) {
        try {
            val shader = RuntimeShader(LIQUID_GLASS_AGSL)
            shader.setFloatUniform("size", sizePx.width, sizePx.height)
            shader.setFloatUniform("refractPx", refractPx)
            RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
        } catch (_: Throwable) {
            null
        }
    }
}
