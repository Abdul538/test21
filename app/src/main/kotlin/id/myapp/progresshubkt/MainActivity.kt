package id.myapp.progresshubkt

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Undo
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Full screen: hide the status bar (and nav bar) entirely. A swipe
        // from the edge reveals them temporarily (transient), rather than
        // requiring the user to dig into a menu to get them back.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val state = AppState(applicationContext)
        state.load()
        setContent {
            var rootSizePx by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            // Density scales with actual screen size (dp², like Dart's
            // `area / 9000`) rather than a fixed count, so bigger screens
            // naturally get more bokeh and small ones aren't overcrowded.
            val particleCount = remember(rootSizePx) {
                with(density) {
                    particleCountForArea(rootSizePx.width.toDp().value, rootSizePx.height.toDp().value)
                }
            }
            val particles = remember(particleCount) { generateParticles(particleCount) }
            val time = rememberParticleTime()

            MaterialTheme(colorScheme = appDarkColorScheme(), shapes = AppShapes) {
                CompositionLocalProvider(
                    LocalParticleField provides ParticleFieldState(particles, time, rootSizePx)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgDark)
                            .onGloballyPositioned { coords ->
                                rootSizePx = androidx.compose.ui.geometry.Size(
                                    coords.size.width.toFloat(),
                                    coords.size.height.toFloat()
                                )
                            }
                    ) {
                        ParticleBackground(modifier = Modifier.fillMaxSize(), particles = particles, time = time)
                        AppRoot(state)
                    }
                }
            }
        }
    }
}

/** A single bottom-nav destination. The active item gets a colored gradient
 * pill behind its icon (with a soft shadow) plus a bright bold label;
 * inactive items stay flat and dim — mirrors the colored-badge language
 * used for stat icons elsewhere in the app instead of Material's plain
 * default indicator. */
@Composable
fun RowScope.BottomNavItem(icon: ImageVector, label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "navItemScale"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .then(
                    if (selected) {
                        Modifier
                            .shadow(8.dp, RoundedCornerShape(50), spotColor = color.copy(alpha = 0.6f))
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(color, color.copy(alpha = 0.7f))))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun AppRoot(state: AppState) {
    if (!state.hasCompletedSetup) {
        NewProgramScreen(state)
        return
    }
    var tab by remember { mutableIntStateOf(0) }
    val phase = remember(state.week, state.settings) { phaseFor(state.week, state.settings.totalWeeks) }
    val (streak, _) = state.computeStreak()

    // Keeps the app-wide accent color (used everywhere via AccentTeal) in
    // sync with whatever the user picked in Settings. LaunchedEffect rather
    // than a bare assignment because writing composable state should happen
    // as an effect, not directly during composition.
    LaunchedEffect(state.settings.accentColorArgb) {
        AccentTeal = Color(state.settings.accentColorArgb)
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = Color.Transparent,
        // Scaffold's default contentWindowInsets is safeDrawing, which
        // already includes the IME inset — meaning its `padding` param
        // would grow on its own as the keyboard opens. Since Today and
        // Settings each handle the keyboard themselves via imePadding()
        // (so only the specific scrollable content that needs it shifts,
        // rather than the whole screen including the header/nav bar),
        // narrowing this to systemBars avoids applying that inset twice.
        //
        // The *top* side is dropped entirely: the status bar is hidden
        // full-screen (see the controller.hide() call above), but Android
        // still reports the bar's configured height for
        // WindowInsets.statusBars regardless of actual visibility.
        // Including it here stacked that phantom status-bar height on
        // top of AppHeader's own displayCutout padding below, so the
        // header sat lower than the physical notch actually needs, and
        // the scrollable content underneath lost that same amount of
        // height — meaning it took less scroll distance for the first
        // card to clear the top. AppHeader alone is responsible for top
        // clearance (sized to the real cutout, not the phantom status
        // bar), so the Scaffold only needs to reserve the bottom
        // (nav bar) and horizontal insets.
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
        bottomBar = {
            Box {
                LiquidGlassBackdrop(modifier = Modifier.matchParentSize())
                Column(modifier = Modifier.background(Color(0x8A0B0F15))) {
                // Hairline separator that brightens toward the center,
                // rather than a flat single-alpha line — a small touch that
                // keeps the bar from reading as a plain flat rectangle
                // dropped under the content.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0f),
                                    Color.White.copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0f)
                                )
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomNavItem(Icons.Filled.CheckCircle, "Hari Ini", tab == 0, AccentTeal) { tab = 0 }
                    BottomNavItem(Icons.Filled.DateRange, "Progres", tab == 1, Color(0xFF4E9BE0)) { tab = 1 }
                    BottomNavItem(Icons.Filled.Insights, "Wawasan", tab == 2, Color(0xFF9B6BFF)) { tab = 2 }
                    BottomNavItem(Icons.Filled.Settings, "Pengaturan", tab == 3, Color(0xFFE0A94E)) { tab = 3 }
                }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            AppHeader(
                phaseName = phase.name,
                phaseColor = Color(phase.colorHex),
                phaseDescription = phase.description,
                week = state.week,
                totalWeeks = state.settings.totalWeeks,
                streak = streak,
                currentWeight = state.currentWeight,
                startWeight = state.settings.startWeight,
                goalWeight = state.settings.goalWeight
            )
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        // Slides in from the direction that matches which
                        // way the user moved through the bottom nav (right
                        // when going to a later tab, left when going back),
                        // rather than every switch using the same motion.
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(260)) { w -> if (forward) w / 3 else -w / 3 } + fadeIn(tween(220)))
                            .togetherWith(slideOutHorizontally(tween(260)) { w -> if (forward) -w / 3 else w / 3 } + fadeOut(tween(180)))
                    },
                    label = "tabContent"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> TodayScreen(state)
                        1 -> ProgressScreen(state)
                        2 -> InsightsScreen(state)
                        else -> SettingsScreen(state)
                    }
                }
                // Purely decorative — no pointerInput/clickable, so it never
                // intercepts scroll or tap gestures meant for the screen
                // beneath it. Mirrors the header's own fade (BgDark rather
                // than the header's tint, since this sits over the
                // particle-lit content, not the header itself) so a card
                // scrolling up dissolves into the boundary the same way the
                // header dissolves into the content, instead of being cut
                // off by a hard edge from either side.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(BgDark.copy(alpha = 0.5f), BgDark.copy(alpha = 0f))
                            )
                        )
                )
            }
        }
    }
    AchievementUnlockBanner(state)
    }
}

@Composable
fun TodayScreen(state: AppState) {
    val (streak, _) = state.computeStreak()
    val (done, total) = state.totalSessionsAll
    var newWeight by remember { mutableStateOf("") }

    // Celebrates the moment the streak actually grows (not just any
    // recomposition) — remembers the previous value across recompositions
    // and only bumps the confetti trigger on a genuine increase.
    var previousStreak by remember { mutableStateOf(streak) }
    var confettiTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(streak) {
        if (streak > previousStreak) confettiTrigger++
        previousStreak = streak
    }

    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ProgressSummaryCard(
                streak = streak,
                fraction = state.progressFraction.toFloat(),
                currentWeight = state.currentWeight,
                goalWeight = state.settings.goalWeight,
                totalKm = state.totalKmAll,
                sessionsDone = done,
                sessionsTotal = total
            )
        }
        item {
            WeekPager(state)
        }
        item {
            SwipeableWeekPanel(state)
        }
        item {
            GlassCard(modifier = Modifier.fillMaxWidth(), hero = true) {
                val history = state.weightHistorySorted
                val lastDelta = if (history.size >= 2) history.last().second - history[history.size - 2].second else null

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(10.dp, RoundedCornerShape(50), ambientColor = AccentTeal, spotColor = AccentTeal)
                            .clip(RoundedCornerShape(50))
                            .background(Brush.linearGradient(listOf(AccentTeal, AccentTeal.copy(alpha = 0.6f))))
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Catat berat badan", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.5.sp)
                        Text("Saat ini %.1f kg".format(state.currentWeight), color = TextDim, fontSize = 11.sp)
                    }
                    if (lastDelta != null && kotlin.math.abs(lastDelta) >= 0.05) {
                        val down = lastDelta < 0
                        val dTint = if (down) AccentTeal else Color(0xFFE0605C)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(dTint.copy(alpha = 0.14f))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                if (down) Icons.Filled.TrendingDown else Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = dTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text("%.1f kg".format(kotlin.math.abs(lastDelta)), color = dTint, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // A big "digital scale" readout instead of a boxed form
                // field — the number you're entering is the whole point of
                // this card, so it gets to be the loudest thing on it.
                val parsedLive = newWeight.toDoubleOrNull()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .border(
                            1.5.dp,
                            if (parsedLive != null) AccentTeal.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.12f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (newWeight.isEmpty()) {
                                Text(
                                    "%.1f".format(state.currentWeight),
                                    color = Color.White.copy(alpha = 0.18f),
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = newWeight,
                                onValueChange = { v -> if (v.length <= 6) newWeight = v },
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = Color.White,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentTeal),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                modifier = Modifier.width(140.dp)
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("kg", color = TextDim, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
                // Live readout of what saving this number would mean —
                // updates on every keystroke instead of only after saving.
                if (parsedLive != null) {
                    val diff = parsedLive - state.currentWeight
                    val down = diff < 0
                    val flat = kotlin.math.abs(diff) < 0.05
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            flat -> "Sama seperti sekarang"
                            down -> "→ Turun %.1f kg dari sekarang".format(-diff)
                            else -> "→ Naik %.1f kg dari sekarang".format(diff)
                        },
                        color = if (flat) TextDim else if (down) AccentTeal else Color(0xFFE0605C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }

                Spacer(Modifier.height(14.dp))
                // Quick nudges off the current weight (or off whatever's
                // already typed) instead of forcing a full manual entry
                // every time — most day-to-day changes are small.
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-1.0, -0.5, 0.5, 1.0).forEach { delta ->
                        WeightDeltaChip(
                            delta = delta,
                            modifier = Modifier.weight(1f)
                        ) {
                            val base = newWeight.toDoubleOrNull() ?: state.currentWeight
                            newWeight = "%.1f".format(base + delta)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                var justLogged by remember { mutableStateOf(false) }
                LaunchedEffect(justLogged) {
                    if (justLogged) {
                        kotlinx.coroutines.delay(1200)
                        justLogged = false
                    }
                }
                val parsed = newWeight.toDoubleOrNull()
                Button(
                    onClick = {
                        parsed?.let {
                            state.logWeight(it)
                            newWeight = ""
                            justLogged = true
                        }
                    },
                    enabled = parsed != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = Color.White,
                        // Stock Material's disabled button collapses to flat
                        // gray, which reads as "broken" rather than "not
                        // ready yet" — keeping it tinted (just dim) makes
                        // clear it's still the same teal action, waiting on
                        // input.
                        disabledContainerColor = AccentTeal.copy(alpha = 0.16f),
                        disabledContentColor = AccentTeal.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (parsed != null)
                                Modifier.shadow(14.dp, RoundedCornerShape(50), ambientColor = AccentTeal, spotColor = AccentTeal)
                            else Modifier
                        )
                ) {
                    Icon(
                        if (justLogged) Icons.Filled.CheckCircle else Icons.Filled.MonitorWeight,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (justLogged) "Tersimpan" else "Simpan")
                }
            }
        }
    }
    ConfettiOverlay(trigger = confettiTrigger, modifier = Modifier.fillMaxSize())
    }
}

/** Small pill for nudging the weight field by a fixed amount (e.g. "+0.5")
 * instead of retyping the whole number for a small day-to-day change. */
@Composable
fun WeightDeltaChip(delta: Double, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val positive = delta > 0
    val tint = if (positive) AccentTeal else Color(0xFFE0605C)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "deltaChipScale"
    )
    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.22f), tint.copy(alpha = 0.10f))))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (positive) Icons.Filled.Add else Icons.Filled.Remove,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text("%.1f".format(kotlin.math.abs(delta)), color = tint, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

/** Wraps the phase chip + day list for the current week in a live,
 * finger-tracking carousel: the previous/current/next week's content is
 * composed simultaneously and shifted by the raw drag delta on every
 * single pointer move (not just committed on release), so the panel
 * visually follows your finger 1:1 while dragging — same feel as a
 * native ViewPager/PageView. On release it commits to the neighbor
 * either by distance (past ~15% of the panel's width) or by a fast
 * enough flick, and otherwise springs back to center.
 *
 * Direction is locked *before* any drag is claimed: awaitFirstDown()
 * only marks a touch, then each subsequent move is compared against the
 * *total accumulated* displacement since that first touch — not just
 * the direction of the single latest event — before deciding whether
 * the gesture is horizontal or vertical. Locking off total displacement
 * (rather than per-event direction) is far more forgiving of the small
 * zigzag any real finger makes, so a normal horizontal swipe doesn't
 * get mistaken for vertical scroll partway through. Nothing is consumed
 * until that decision is made, so if it does turn out vertical, the
 * surrounding LazyColumn's own scroll gesture is still free to take
 * over the touch.
 *
 * The previous/next weeks are only composed (and only pay their full
 * blurred-glass draw cost) while `isSwiping` is true — i.e. from the
 * moment a horizontal drag is recognized until its settle animation
 * finishes — instead of permanently, even when fully hidden behind
 * clipToBounds(). At rest that's a 3x reduction in simultaneously
 * blurred GlassCards for zero visible difference, since the neighbors
 * were invisible either way. */
@Composable
fun SwipeableWeekPanel(state: AppState) {
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val dragX = remember { Animatable(0f) }
    val totalWeeks = state.settings.totalWeeks
    val week = state.week
    var isSwiping by remember { mutableStateOf(false) }
    // A visible gap between pages while dragging/animating — without it,
    // the incoming neighbor panel's edge sits flush against the current
    // one mid-swipe and reads as one merged panel rather than two
    // distinct sliding pages.
    val panelGapPx = with(androidx.compose.ui.platform.LocalDensity.current) { 20.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clipToBounds()
            .onGloballyPositioned { coords -> containerWidthPx = coords.size.width.toFloat() }
            .pointerInput(totalWeeks, week) {
                // awaitEachGesture's block runs with an AwaitPointerEventScope
                // receiver, which is a @RestrictsSuspension scope: only
                // awaitPointerEvent()-style calls are allowed to suspend
                // directly inside it. Animatable.snapTo()/animateTo() are
                // ordinary suspend members, not part of that restricted
                // scope, so they can't be called inline there — each call
                // is dispatched via launch{} on the surrounding
                // coroutineScope instead. This dispatcher processes queued
                // coroutines in order with no perceptible lag.
                coroutineScope {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val slop = viewConfiguration.touchSlop
                        val velocityTracker = VelocityTracker()
                        var lockedHorizontal = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (lockedHorizontal) change.consume()
                                break
                            }

                            if (!lockedHorizontal) {
                                val total = change.position - down.position
                                val overSlop = kotlin.math.abs(total.x) > slop || kotlin.math.abs(total.y) > slop
                                if (overSlop) {
                                    if (kotlin.math.abs(total.x) > kotlin.math.abs(total.y)) {
                                        lockedHorizontal = true
                                        isSwiping = true
                                    } else {
                                        // Predominantly vertical — leave this
                                        // (and all further) events untouched
                                        // for the parent scroll to handle.
                                        break
                                    }
                                }
                            }

                            if (lockedHorizontal) {
                                velocityTracker.addPointerInputChange(change)
                                val dragAmount = change.positionChange().x
                                if (dragAmount != 0f) {
                                    // Rubber-band past the first/last week:
                                    // each increment is damped rather than the
                                    // running total rescaled, so it resists
                                    // smoothly instead of sliding freely into
                                    // empty space.
                                    val pushingPastStart = dragAmount > 0 && week == 1
                                    val pushingPastEnd = dragAmount < 0 && week == totalWeeks
                                    val factor = if (pushingPastStart || pushingPastEnd) 0.35f else 1f
                                    launch { dragX.snapTo(dragX.value + dragAmount * factor) }
                                }
                                change.consume()
                            }
                        }

                        if (lockedHorizontal) {
                            val current = dragX.value
                            val velocity = velocityTracker.calculateVelocity().x
                            val threshold = containerWidthPx * 0.15f
                            val flingVelocity = 800f
                            launch {
                                when {
                                    (current <= -threshold || velocity <= -flingVelocity) && week < totalWeeks -> {
                                        dragX.animateTo(-containerWidthPx, animationSpec = tween(200))
                                        state.goToWeek(week + 1)
                                        dragX.snapTo(0f)
                                    }
                                    (current >= threshold || velocity >= flingVelocity) && week > 1 -> {
                                        dragX.animateTo(containerWidthPx, animationSpec = tween(200))
                                        state.goToWeek(week - 1)
                                        dragX.snapTo(0f)
                                    }
                                    else -> dragX.animateTo(
                                        0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    )
                                }
                                isSwiping = false
                            }
                        }
                    }
                }
            }
    ) {
        if (isSwiping && week > 1) {
            Box(modifier = Modifier.fillMaxWidth().offset { IntOffset((dragX.value - containerWidthPx - panelGapPx).roundToInt(), 0) }) {
                WeekContent(state, week - 1)
            }
        }
        Box(modifier = Modifier.fillMaxWidth().offset { IntOffset(dragX.value.roundToInt(), 0) }) {
            WeekContent(state, week)
        }
        if (isSwiping && week < totalWeeks) {
            Box(modifier = Modifier.fillMaxWidth().offset { IntOffset((dragX.value + containerWidthPx + panelGapPx).roundToInt(), 0) }) {
                WeekContent(state, week + 1)
            }
        }
    }
}


/** Phase chip + day list for one specific week — used for the current
 * week and its immediate neighbors in [SwipeableWeekPanel]. */
@Composable
fun WeekContent(state: AppState, week: Int) {
    val days = remember(week, state.settings) { weekPlan(week, state.settings) }
    val phase = remember(week, state.settings) { phaseFor(week, state.settings.totalWeeks) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PhaseChip(phase.name, Color(phase.colorHex))
        DayListCard(state, days)
    }
}

/** Ring + at-a-glance stats, mirroring the Dart "PROGRESS" panel: a streak
 * pill top-right, the animated ring on the left, and a label/value stat
 * column on the right (current weight, target, total distance, sessions
 * completed). */
@Composable
fun ProgressSummaryCard(
    streak: Int,
    fraction: Float,
    currentWeight: Double,
    goalWeight: Double,
    totalKm: Double,
    sessionsDone: Int,
    sessionsTotal: Int
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), hero = true, borderColor = AccentTeal) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PROGRESS", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Color(0xFFE0A94E).copy(alpha = 0.5f), RoundedCornerShape(50))
                    .background(Color(0xFFE0A94E).copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFE0A94E), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("$streak hari", color = Color(0xFFE0A94E), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                fraction = fraction,
                color = AccentTeal,
                centerLabel = "menuju target",
                size = 130.dp
            )
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressStatLine("Berat sekarang", "%.1f kg".format(currentWeight))
                ProgressStatLine("Target", "${goalWeight.toInt()} kg")
                ProgressStatLine("Total jarak", "${"%.0f".format(totalKm)} km")
                ProgressStatLine("Sesi selesai", "$sessionsDone/$sessionsTotal")
            }
        }
    }
}

@Composable
fun ProgressStatLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

/** Horizontally scrollable row of numbered week boxes with chevrons either
 * side, auto-scrolling so the current week stays in view — mirrors the
 * Dart week pager (1 2 [3] 4 5 6 7 ‹ ›). */
@Composable
fun WeekPager(state: AppState) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(state.week) {
        listState.animateScrollToItem((state.week - 2).coerceAtLeast(0))
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { state.goToWeek(state.week - 1) }, enabled = state.week > 1) {
            Text("‹", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.settings.totalWeeks) { i ->
                val w = i + 1
                val selected = w == state.week
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (selected) AccentTeal else Color.Transparent, RoundedCornerShape(12.dp))
                        .clickable { state.goToWeek(w) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("$w", color = if (selected) AccentTeal else TextDim, fontWeight = FontWeight.Bold)
                }
            }
        }
        IconButton(onClick = { state.goToWeek(state.week + 1) }, enabled = state.week < state.settings.totalWeeks) {
            Text("›", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PhaseChip(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(8.dp))
        Text(name, color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** Each day of the week gets its own small frosted panel — a separate
 * [GlassCard] per active day (border lights up teal once done), and a
 * plain dim non-glass panel for rest days — rather than one continuous
 * card with dividers. Matches the Flutter original's `_DayRow`/`_DayListSection`. */
@Composable
fun DayListCard(state: AppState, days: List<DayPlan>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        days.forEach { day ->
            if (day.rest) {
                RestDayRow(day)
            } else {
                val key = "${state.week}-${day.key}"
                val done = state.completed[key] == true
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    radius = 16.dp,
                    borderColor = if (done) AccentTeal else GlassBorder
                ) {
                    DayListRow(state, day)
                }
            }
        }
    }
}

/** Dim, non-glass panel for rest days — intentionally skips the blur/glow
 * treatment so it reads as inactive next to the active day panels. A small
 * moon icon makes it unambiguous at a glance that this is "nothing to do
 * today" rather than an error/missing state. */
@Composable
fun RestDayRow(day: DayPlan) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BgDark.copy(alpha = 0.62f))
            .border(1.dp, Color(0xFF232A35), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bedtime, contentDescription = null, tint = Color(0xFF454E5E), modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(7.dp))
            Text(day.label, color = Color(0xFF5A6577), fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Text(formatDateId(day.date), color = Color(0xFF454E5E), fontSize = 11.sp)
        Text("Istirahat", color = Color(0xFF454E5E), fontSize = 12.sp, letterSpacing = 0.6.sp)
    }
}

@Composable
fun DayListRow(state: AppState, day: DayPlan) {
    val key = "${state.week}-${day.key}"
    val done = state.completed[key] == true
    var kmText by remember(key, state.actualKm[key]) {
        mutableStateOf((state.actualKm[key] ?: day.km).let { if (it == it.roundToInt().toDouble()) it.roundToInt().toString() else it.toString() })
    }
    val currentWeight = state.currentWeight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // The whole row toggles completion now, not just the small
            // checkbox hit target — taps on the km field still land on it
            // first since Compose hit-tests the more specific nested
            // component before this outer one.
            .clickable { state.toggleDayDone(state.week, day.key, day.km) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(50))
                .background(if (done) Brush.linearGradient(listOf(AccentTeal, AccentTeal.copy(alpha = 0.7f))) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)))
                .border(1.5.dp, if (done) Color.Transparent else Color.White.copy(alpha = 0.28f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            if (done) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.width(6.dp))
                Text(formatDateId(day.date), color = TextDim, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            val kcal = estimateCalories(currentWeight, day.km)
            Text(
                if (done) "Selesai · ${day.km.roundToInt()} km · ≈$kcal kkal" else "Target ${day.km.roundToInt()} km · ≈$kcal kkal · ketuk untuk selesai",
                color = if (done) AccentTeal.copy(alpha = 0.85f) else TextDim,
                fontSize = 12.5.sp
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.30f))
                    .border(1.dp, if (done) AccentTeal.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.text.BasicTextField(
                    value = kmText,
                    onValueChange = {
                        kmText = it
                        it.toDoubleOrNull()?.let { v -> state.setActualKm(state.week, day.key, v) }
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentTeal),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            // The number alone is ambiguous (km? minutes? calories?) —
            // this makes the unit explicit without needing to tap in.
            Text("km", color = TextDim, fontSize = 9.sp)
        }
    }
}

@Composable
fun ProgressScreen(state: AppState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            WeightChartCard(state)
        }
        item {
            MiniStatsGrid(state)
        }
        item {
            Text("Riwayat berat", fontWeight = FontWeight.Bold, color = Color.White)
        }
        items(state.weightHistorySorted.reversed()) { (offset, w) ->
            val date = state.settings.startDate + offset
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDateId(date), color = TextDim)
                    Text("${"%.1f".format(w)} kg", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Weight-over-time chart card — header with a title + a trend chip
 * showing the change since the first log entry, then the chart itself
 * (or a placeholder while there's under 2 data points). Mirrors the
 * Flutter version's fl_chart panel. */
@Composable
fun WeightChartCard(state: AppState) {
    val history = state.weightHistorySorted
    GlassCard(modifier = Modifier.fillMaxWidth(), hero = true, borderColor = AccentTeal) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("GRAFIK BERAT BADAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
            if (history.size >= 2) {
                TrendChip(delta = history.last().second - history.first().second)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (history.size < 2) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Catat berat badan minimal 2x untuk lihat grafik",
                    color = Color(0xFF6B7688),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            WeightLineChart(history = history, accent = AccentTeal, startDate = state.settings.startDate)
        }
    }
}

/** Small pill showing weight change since the first log — green with a
 * down arrow when the trend is favorable (losing weight), amber with an
 * up arrow otherwise. */
@Composable
fun TrendChip(delta: Double) {
    val improving = delta <= 0
    val color = if (improving) Color(0xFF4EE0A0) else Color(0xFFE0A94E)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(if (improving) "↓" else "↑", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Text(
            "${if (delta > 0) "+" else ""}${"%.1f".format(delta)} kg",
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Nice-looking axis bounds for a set of weight values: picks a "clean"
 * step (0.1/0.2/0.25/0.5/1/2/2.5/5/10/20/50) that gives roughly 5 grid
 * lines, then pads the min/max out to the nearest step so the data isn't
 * drawn flush against the chart edges. Mirrors the Dart chart's approach
 * so small week-to-week changes still show real movement in the line
 * instead of being flattened by an overly coarse scale. */
private fun computeNiceAxis(weights: List<Double>): Triple<Double, Double, Double> {
    val minW = weights.min()
    val maxW = weights.max()
    val rawRange = (maxW - minW)
    val span = if (rawRange < 0.1) 0.1 else rawRange
    val niceSteps = listOf(0.1, 0.2, 0.25, 0.5, 1.0, 2.0, 2.5, 5.0, 10.0, 20.0, 50.0)
    val step = niceSteps.firstOrNull { span / 5 <= it } ?: niceSteps.last()
    val pad = step / 2
    val minY = kotlin.math.floor((minW - pad) / step) * step
    val maxY = kotlin.math.ceil((maxW + pad) / step) * step
    return Triple(minY, maxY, step)
}

/** Interactive weight-over-time chart: smoothed gradient line with a
 * soft area fill beneath it, thin horizontal gridlines with left-side
 * value labels, a glowing dot on the most recent entry, and touch/drag
 * support — press or drag anywhere on the chart to see a dashed
 * indicator + tooltip with the exact date and weight at that point. */
@Composable
fun WeightLineChart(history: List<Pair<Int, Double>>, accent: Color, startDate: Long) {
    val (minY, maxY, interval) = remember(history) { computeNiceAxis(history.map { it.second }) }
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val leftLabelWidth = with(androidx.compose.ui.platform.LocalDensity.current) { 40.dp.toPx() }

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .pointerInput(history) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val chartWidth = size.width - leftLabelWidth
                        val step = if (history.size > 1) chartWidth / (history.size - 1) else chartWidth
                        fun nearest(x: Float): Int {
                            val rel = (x - leftLabelWidth).coerceIn(0f, chartWidth)
                            return (rel / step).roundToInt().coerceIn(0, history.size - 1)
                        }
                        selectedIndex = nearest(down.position.x)
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.pressed }
                            if (change != null) {
                                selectedIndex = nearest(change.position.x)
                                change.consume()
                            }
                        } while (event.changes.any { it.pressed })
                        selectedIndex = null
                    }
                }
        ) {
            val chartWidth = size.width - leftLabelWidth
            val chartHeight = size.height

            fun yFor(value: Double): Float {
                val t = ((value - minY) / (maxY - minY)).coerceIn(0.0, 1.0)
                return (chartHeight - t * chartHeight).toFloat()
            }

            // Gridlines + value labels, stepped by the computed nice interval.
            var gridValue = minY
            while (gridValue <= maxY + 0.0001) {
                val y = yFor(gridValue)
                drawLine(
                    color = Color.White.copy(alpha = 0.045f),
                    start = Offset(leftLabelWidth, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                val decimals = if (interval % 1.0 == 0.0) 0 else if (((interval * 100).roundToInt() % 10) == 0) 1 else 2
                drawText(
                    textMeasurer = textMeasurer,
                    text = "%.${decimals}f".format(gridValue),
                    topLeft = Offset(0f, y - 14f),
                    style = androidx.compose.ui.text.TextStyle(color = Color(0xFF6B7688), fontSize = 9.sp),
                    size = Size(leftLabelWidth - 6f, 20f)
                )
                gridValue += interval
            }

            if (history.isEmpty()) return@Canvas
            val n = history.size
            val stepX = if (n > 1) chartWidth / (n - 1) else 0f
            val points = history.mapIndexed { i, (_, w) ->
                Offset(leftLabelWidth + stepX * i, yFor(w))
            }

            // Smoothed line through the points (quadratic bezier via each
            // segment's midpoint) — a lightweight way to get a curved
            // line without a full spline implementation.
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val midX = (p0.x + p1.x) / 2f
                    val midY = (p0.y + p1.y) / 2f
                    quadraticBezierTo(p0.x, p0.y, midX, midY)
                }
                lineTo(points.last().x, points.last().y)
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(points.last().x, chartHeight)
                lineTo(points.first().x, chartHeight)
                close()
            }

            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(listOf(accent.copy(alpha = 0.22f), accent.copy(alpha = 0f)))
            )
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(listOf(accent.copy(alpha = 0.55f), accent)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Glowing dot on the latest entry.
            val last = points.last()
            drawCircle(color = accent.copy(alpha = 0.35f), radius = 9.dp.toPx(), center = last)
            drawCircle(color = accent, radius = 5.dp.toPx(), center = last)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = last)

            // Touch indicator + tooltip.
            selectedIndex?.let { idx ->
                val p = points[idx]
                drawLine(
                    color = accent.copy(alpha = 0.5f),
                    start = Offset(p.x, 0f),
                    end = Offset(p.x, chartHeight),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = p)
                drawCircle(color = accent, radius = 4.dp.toPx(), center = p, style = Stroke(width = 2.dp.toPx()))

                val (offset, w) = history[idx]
                val label = "${"%.1f".format(w)} kg · ${formatDateId(startDate + offset)}"
                val measured = textMeasurer.measure(
                    androidx.compose.ui.text.AnnotatedString(label),
                    style = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                val bubbleW = measured.size.width + 20
                val bubbleH = measured.size.height + 14
                var bubbleX = p.x - bubbleW / 2f
                bubbleX = bubbleX.coerceIn(0f, (size.width - bubbleW).coerceAtLeast(0f))
                val bubbleY = (p.y - bubbleH - 14f).coerceAtLeast(0f)
                drawRoundRect(
                    color = Color(0xFF10151D).copy(alpha = 0.92f),
                    topLeft = Offset(bubbleX, bubbleY),
                    size = Size(bubbleW.toFloat(), bubbleH.toFloat()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawText(
                    textLayoutResult = measured,
                    color = Color.White,
                    topLeft = Offset(bubbleX + 10, bubbleY + 7)
                )
            }
        }
    }
}

/** 2-column grid of small icon stat tiles, all inside a single [GlassCard]
 * (one blur pass shared by the whole grid rather than one per tile —
 * mirrors the Flutter version's perf note about too many simultaneous
 * blurred panels causing jank on scroll). */
@Composable
fun MiniStatsGrid(state: AppState) {
    val (streak, best) = state.computeStreak()
    val (done, total) = state.totalSessionsAll
    val remaining = (state.currentWeight - state.settings.goalWeight).coerceIn(0.0, 999.0)
    val consistency = if (total > 0) (done.toFloat() / total * 100).roundToInt() else 0

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Modifier.weight(1f), Icons.Filled.MonitorWeight, AccentTeal, "Berat sekarang", "${"%.1f".format(state.currentWeight)} kg")
                MiniStat(Modifier.weight(1f), Icons.Filled.GpsFixed, Color(0xFF4E9BE0), "Sisa target", "${"%.1f".format(remaining)} kg")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Modifier.weight(1f), Icons.Filled.DirectionsBike, Color(0xFF4EE0A0), "Total jarak", "${state.totalKmAll.roundToInt()} km")
                MiniStat(Modifier.weight(1f), Icons.Filled.Bolt, Color(0xFFD97757), "Estimasi kalori", "${state.totalCaloriesAll} kkal")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Modifier.weight(1f), Icons.Filled.CheckCircle, Color(0xFF7FE04E), "Sesi selesai", "$done/$total")
                MiniStat(Modifier.weight(1f), Icons.Filled.LocalFireDepartment, Color(0xFFE0A94E), "Streak sekarang", "$streak hari")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat(Modifier.weight(1f), Icons.Filled.EmojiEvents, Color(0xFFE0C64E), "Streak terbaik", "$best hari")
                MiniStat(Modifier.weight(1f), Icons.Filled.BarChart, Color(0xFF8A7FD1), "Konsistensi", "$consistency%")
            }
        }
    }
}

@Composable
fun MiniStat(modifier: Modifier, icon: ImageVector, color: Color, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.16f))
                .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = TextDim, fontSize = 9.5.sp, letterSpacing = 0.4.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

/** Snapshot of the settings form used for undo/redo. */
private data class SettingsSnapshot(val startWeight: String, val goalWeight: String, val totalWeeks: String)

@Composable
fun SettingsScreen(state: AppState) {
    var startWeight by remember { mutableStateOf(state.settings.startWeight.toString()) }
    var goalWeight by remember { mutableStateOf(state.settings.goalWeight.toString()) }
    var totalWeeks by remember { mutableStateOf(state.settings.totalWeeks.toString()) }

    // Undo/redo works per edit, not per keystroke: a snapshot of the whole
    // form is captured when a field gains focus, and only pushed onto the
    // undo stack when that field loses focus with a changed value. That
    // keeps each undo step meaningful ("revert my last edit to this
    // field") instead of flooding the stack character by character.
    val undoStack = remember { mutableStateListOf<SettingsSnapshot>() }
    val redoStack = remember { mutableStateListOf<SettingsSnapshot>() }
    var focusSnapshot by remember { mutableStateOf<SettingsSnapshot?>(null) }

    fun currentSnapshot() = SettingsSnapshot(startWeight, goalWeight, totalWeeks)
    fun applySnapshot(s: SettingsSnapshot) {
        startWeight = s.startWeight
        goalWeight = s.goalWeight
        totalWeeks = s.totalWeeks
    }
    fun commitPendingEdit() {
        val before = focusSnapshot
        if (before != null && before != currentSnapshot()) {
            undoStack.add(before)
            redoStack.clear()
        }
        focusSnapshot = null
    }
    fun undo() {
        commitPendingEdit()
        if (undoStack.isEmpty()) return
        redoStack.add(currentSnapshot())
        applySnapshot(undoStack.removeAt(undoStack.lastIndex))
    }
    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(currentSnapshot())
        applySnapshot(redoStack.removeAt(redoStack.lastIndex))
    }
    fun fieldFocusModifier() = Modifier.onFocusChanged { f ->
        if (f.isFocused) focusSnapshot = currentSnapshot() else commitPendingEdit()
    }

    var justSaved by remember { mutableStateOf(false) }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(1500)
            justSaved = false
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // "Create document" lets the user pick where the export file goes
    // (Downloads, Drive, etc.) rather than us guessing a path.
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        OutputStreamWriter(out).use { it.write(state.exportJson()) }
                    }
                    true
                } catch (_: Exception) { false }
            }
            Toast.makeText(context, if (ok) "Data diekspor" else "Gagal mengekspor data", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        BufferedReader(InputStreamReader(input)).readText()
                    }
                } catch (_: Exception) { null }
            }
            val ok = text != null && state.importData(text)
            if (ok) {
                // Reflect the freshly-imported settings back into the form fields.
                startWeight = state.settings.startWeight.toString()
                goalWeight = state.settings.goalWeight.toString()
                totalWeeks = state.settings.totalWeeks.toString()
                undoStack.clear(); redoStack.clear()
            }
            Toast.makeText(context, if (ok) "Data diimpor" else "Gagal mengimpor data — file tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(50))
                    .background(AccentTeal.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Pengaturan Program",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            // A single grouped pill (icon, hairline divider, icon) reads as
            // one "undo/redo" control — two separate floating circles with
            // a gap between them just looked like two unrelated buttons
            // that happened to be near each other.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(50)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { undo() },
                    enabled = undoStack.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Undo,
                        contentDescription = "Undo perubahan",
                        modifier = Modifier.size(16.dp),
                        tint = if (undoStack.isNotEmpty()) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.2f)
                    )
                }
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
                IconButton(
                    onClick = { redo() },
                    enabled = redoStack.isNotEmpty(),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Redo,
                        contentDescription = "Redo perubahan",
                        modifier = Modifier.size(16.dp),
                        tint = if (redoStack.isNotEmpty()) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }
        Text("PROGRAM", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            SteppedField(
                value = startWeight,
                onValueChange = { startWeight = it },
                label = "Berat awal (kg)",
                icon = Icons.Filled.MonitorWeight,
                iconColor = AccentTeal,
                step = 0.5,
                min = 1.0,
                focusModifier = fieldFocusModifier()
            )
            Spacer(Modifier.height(12.dp))
            SteppedField(
                value = goalWeight,
                onValueChange = { goalWeight = it },
                label = "Berat target (kg)",
                icon = Icons.Filled.GpsFixed,
                iconColor = Color(0xFF4E9BE0),
                step = 0.5,
                min = 1.0,
                focusModifier = fieldFocusModifier()
            )
            Spacer(Modifier.height(12.dp))
            SteppedField(
                value = totalWeeks,
                onValueChange = { totalWeeks = it },
                label = "Jumlah minggu",
                icon = Icons.Filled.DateRange,
                iconColor = Color(0xFFE0A94E),
                step = 1.0,
                min = 1.0,
                focusModifier = fieldFocusModifier()
            )
        }

        // Live feedback on what the numbers above actually mean — computed
        // from whatever's currently typed (not the saved settings), so
        // editing a field updates this immediately instead of only after
        // hitting Simpan.
        val liveStart = startWeight.toDoubleOrNull()
        val liveGoal = goalWeight.toDoubleOrNull()
        val liveWeeks = totalWeeks.toIntOrNull()
        if (liveStart != null && liveGoal != null && liveWeeks != null && liveWeeks > 0 && liveStart > liveGoal) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Insights, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("RINGKASAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.6.sp)
                }
                Spacer(Modifier.height(8.dp))
                val totalLoss = liveStart - liveGoal
                val perWeek = totalLoss / liveWeeks
                val etaDay = todayEpochDay() + liveWeeks * 7
                Text(
                    "Turun %.1f kg dalam %d minggu".format(totalLoss, liveWeeks),
                    color = TextDim,
                    fontSize = 12.5.sp
                )
                Text(
                    "~%.2f kg/minggu · target selesai %s".format(perWeek, formatDateId(etaDay)),
                    color = TextDim,
                    fontSize = 12.5.sp
                )
            }
        }

        Text("WARNA AKSEN", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                AccentPresets.forEach { argb ->
                    AccentSwatch(
                        argb = argb,
                        selected = state.settings.accentColorArgb == argb,
                        onClick = { state.updateSettings(state.settings.copy(accentColorArgb = argb)) }
                    )
                }
            }
        }
        Text("DATA", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text("Ekspor progres & pengaturan ke file, atau pulihkan dari file sebelumnya.", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("progress-hub-backup.json") },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.04f),
                        contentColor = AccentTeal
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ekspor")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.04f),
                        contentColor = AccentTeal
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Impor")
                }
            }
        }
        // Only worth showing "Simpan Pengaturan" once there's something to
        // save — comparing parsed values rather than raw strings so e.g.
        // "26" vs "26" typed differently doesn't falsely count as a change.
        val isDirty = startWeight.toDoubleOrNull() != state.settings.startWeight ||
            goalWeight.toDoubleOrNull() != state.settings.goalWeight ||
            totalWeeks.toIntOrNull() != state.settings.totalWeeks
        if (isDirty || justSaved) {
            Button(
                onClick = {
                    commitPendingEdit()
                    state.updateSettings(
                        state.settings.copy(
                            startWeight = startWeight.toDoubleOrNull() ?: state.settings.startWeight,
                            goalWeight = goalWeight.toDoubleOrNull() ?: state.settings.goalWeight,
                            totalWeeks = totalWeeks.toIntOrNull() ?: state.settings.totalWeeks
                        )
                    )
                    justSaved = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, RoundedCornerShape(50), ambientColor = AccentTeal, spotColor = AccentTeal)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (justSaved) "Tersimpan" else "Simpan Pengaturan")
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("ZONA BAHAYA", color = Color(0xFFE0605C), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        var showResetConfirm by remember { mutableStateOf(false) }
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Menghapus semua sesi selesai, jarak, dan riwayat berat, lalu memulai dari Minggu 1. Pengaturan program di atas tidak berubah.",
                color = TextDim,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { showResetConfirm = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFE0605C).copy(alpha = 0.06f),
                    contentColor = Color(0xFFE0605C)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0605C).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset Program")
            }
        }
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                containerColor = BgDark2,
                title = { Text("Reset progres?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        "Semua sesi selesai, jarak, dan riwayat berat akan dihapus permanen dan tidak bisa dikembalikan. Pengaturan program tidak berubah.",
                        color = TextDim,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { state.resetProgress(); showResetConfirm = false }) {
                        Text("Reset", color = Color(0xFFE0605C), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Batal", color = TextDim) }
                }
            )
        }
    }
}

/** One accent color option: scales down slightly while pressed (a spring
 * back on release) instead of relying only on a ripple, and glows in its
 * own color with a white ring when selected. */
@Composable
private fun AccentSwatch(argb: Long, selected: Boolean, onClick: () -> Unit) {
    val swatch = Color(argb)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "swatchScale"
    )
    Box(
        modifier = Modifier
            .size(38.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (selected) Modifier.shadow(10.dp, RoundedCornerShape(50), spotColor = swatch, ambientColor = swatch)
                else Modifier
            )
            .clip(RoundedCornerShape(50))
            .background(swatch)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(Icons.Filled.CheckCircle, contentDescription = "Dipilih", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** A numeric field with small +/- stepper buttons in the trailing slot, so
 * small adjustments don't require clearing the field and retyping. */
@Composable
private fun SteppedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    step: Double,
    min: Double,
    focusModifier: Modifier
) {
    fun formatStep(v: Double): String = if (v == v.toLong().toDouble()) v.toLong().toString() else "%.1f".format(v)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(iconColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
            }
        },
        trailingIcon = {
            // A single tinted capsule with a hairline divider between −/+,
            // instead of two bare floating icon buttons — reads as one
            // stepper control rather than two disconnected taps.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.10f))
            ) {
                IconButton(
                    onClick = {
                        val next = ((value.toDoubleOrNull() ?: min) - step).coerceAtLeast(min)
                        onValueChange(formatStep(next))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "Kurangi", tint = iconColor, modifier = Modifier.size(14.dp))
                }
                Box(modifier = Modifier.width(1.dp).height(16.dp).background(iconColor.copy(alpha = 0.25f)))
                IconButton(
                    onClick = {
                        val next = (value.toDoubleOrNull() ?: min) + step
                        onValueChange(formatStep(next))
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah", tint = iconColor, modifier = Modifier.size(14.dp))
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = iconColor,
            cursorColor = iconColor,
            focusedLabelColor = iconColor
        ),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        ),
        modifier = Modifier.fillMaxWidth().then(focusModifier)
    )
}
