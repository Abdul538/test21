package id.myapp.progresshubkt

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// `by mutableStateOf` on a top-level property works the same as inside a
// composable: any @Composable that reads AccentTeal gets subscribed to it,
// so changing the accent color (e.g. from Settings) recomposes every place
// that uses it — no plumbing a CompositionLocal through the whole tree.
var AccentTeal by mutableStateOf(Color(0xFF5FB3A3))
val BgDark = Color(0xFF0B0F15)
val BgDark2 = Color(0xFF11161D)
val GlassBorder = Color(0x33FFFFFF)
val GlassFill = Color(0x140C0F14) // subtle translucent panel fill
val TextDim = Color(0xFF9AA5B1)
val DangerRed = Color(0xFFE0605C)

/** A curated set of accent colors the user can pick from in Settings,
 * stored as raw ARGB longs (same representation as
 * ProgramSettings.accentColorArgb) so picking a swatch is a simple,
 * exact equality check rather than comparing Color instances. */
val AccentPresets = listOf(
    0xFF5FB3A3L, // teal (default)
    0xFF4C8DFFL, // blue
    0xFF9B6BFFL, // violet
    0xFFFF8A5CL, // orange
    0xFFFF6B9AL, // pink
    0xFF6BCB77L  // green
)

// Built inside a @Composable (rather than as a plain top-level val) so it
// re-reads AccentTeal on every recomposition instead of freezing whatever
// value AccentTeal held the first time this file was loaded.
//
// Also tunes the tokens that stock Material components (OutlinedTextField
// borders, AlertDialog surfaces, error states) pull from by default, which
// otherwise default to Material's stock purple-gray outline and red error
// — neither of which reads as part of the same frosted-glass palette as
// the hand-built GlassCard everything else sits in.
@Composable
fun appDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = AccentTeal,
    onPrimary = Color.White,
    secondary = AccentTeal,
    tertiary = AccentTeal,
    background = BgDark,
    surface = BgDark2,
    surfaceVariant = BgDark2,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = TextDim,
    outline = Color.White.copy(alpha = 0.28f),
    outlineVariant = Color.White.copy(alpha = 0.14f),
    error = DangerRed,
    onError = Color.White
)

/** Rounded app-wide, matching the glass cards' own radius instead of
 * Material's default tight 4dp corners — applies to any stock component
 * (text fields, dialogs, menus) that isn't manually shaped. */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)
