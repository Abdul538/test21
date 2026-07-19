package id.myapp.progresshubkt

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/** A single unlockable milestone shown in the Insights tab's achievement
 * grid — purely derived from existing AppState data, no new persistence. */
data class Achievement(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
    val unlocked: Boolean
)

/** Rounds up to a visually "nice" number (1/2/5 × a power of ten) so
 * plan-scaled tiers read as e.g. "150 km" instead of "147 km". */
private fun niceCeil(x: Double): Double {
    if (x <= 0) return 0.0
    val exp = kotlin.math.floor(kotlin.math.log10(x))
    val magnitude = Math.pow(10.0, exp)
    val residual = x / magnitude
    val niceResidual = when {
        residual <= 1.0 -> 1.0
        residual <= 2.0 -> 2.0
        residual <= 5.0 -> 5.0
        else -> 10.0
    }
    val candidate = niceResidual * magnitude
    return if (candidate < x) candidate * 2 else candidate
}

/** Fraction points of a plan total, rounded to nice numbers — except the
 * final 100% tier, which stays exact so "finish the whole plan" lines up
 * with the plan's real total instead of a rounded-up number that would
 * sit just out of reach. */
private fun scaledTiers(total: Double): List<Double> =
    listOf(0.15, 0.35, 0.6, 0.85, 1.0)
        .map { f -> if (f >= 1.0) total else niceCeil(total * f) }
        .filter { it > 0 }
        .distinct()
        .sorted()

fun computeAchievements(state: AppState): List<Achievement> {
    val settings = state.settings
    val (_, bestStreak) = state.computeStreak()
    val (done, _) = state.totalSessionsAll
    val totalKm = state.totalKmAll
    val lost = (settings.startWeight - state.currentWeight).coerceAtLeast(0.0)
    val progressPct = state.progressFraction * 100
    val weeksIn = state.week

    // Everything below is computed from the person's *actual* plan
    // (totalWeeks) rather than fixed constants — a 15-week plan won't show
    // a "1000 km" badge it could never reach, and a 48-week plan gets
    // milestones that actually stretch across all of it.
    val planSessions = (1..settings.totalWeeks).sumOf { w -> weekPlan(w, settings).count { !it.rest } }
    val planKm = (1..settings.totalWeeks).sumOf { w -> weekPlan(w, settings).sumOf { it.km } }
    val planKcal = estimateCalories(settings.startWeight, planKm).toDouble()
    val planLossKg = (settings.startWeight - settings.goalWeight).coerceAtLeast(0.5)
    val planDays = settings.totalWeeks * 7

    val list = mutableListOf<Achievement>()
    val heatColors = listOf(Color(0xFFE0C64E), Color(0xFFE0A94E), Color(0xFFE08A4E), Color(0xFFD97757), Color(0xFFE0605C))
    val coolColors = listOf(Color(0xFF6BCB77), Color(0xFF4EE0A0), Color(0xFF4EE0C4), Color(0xFF4EC4E0), Color(0xFF4E9BE0))

    // Streak — scaled to the plan's total length in days.
    scaledTiers(planDays.toDouble()).forEachIndexed { i, v ->
        val n = v.toInt().coerceAtLeast(1)
        list += Achievement("Streak $n Hari", "$n hari beruntun tanpa putus", Icons.Filled.LocalFireDepartment, heatColors[i % heatColors.size], bestStreak >= n)
    }

    // Distance — scaled to the total km this specific plan schedules.
    scaledTiers(planKm).forEachIndexed { i, v ->
        val n = v.roundToInt().coerceAtLeast(1)
        list += Achievement("$n KM Ditempuh", "Total jarak kumulatif $n km dari ${planKm.roundToInt()} km rencana", Icons.Filled.DirectionsBike, coolColors[i % coolColors.size], totalKm >= n)
    }

    // Calories — scaled to the estimated total calorie burn of the plan.
    scaledTiers(planKcal).forEachIndexed { i, v ->
        val n = v.roundToInt().coerceAtLeast(1)
        val burnedSoFar = estimateCalories(settings.startWeight, totalKm)
        list += Achievement("$n Kkal Terbakar", "Estimasi kalori terbakar dari ${planKcal.roundToInt()} kkal rencana", Icons.Filled.Bolt, heatColors[(i + 2) % heatColors.size], burnedSoFar >= n)
    }

    // Weight loss — scaled to this plan's actual start→goal difference,
    // so a modest 5kg goal still has reachable milestones along the way.
    scaledTiers(planLossKg).forEachIndexed { i, v ->
        val label = if (v == v.toInt().toDouble()) v.toInt().toString() else "%.1f".format(v)
        list += Achievement("Turun $label KG", "Berat turun $label kg dari awal", Icons.Filled.MonitorWeight, coolColors[(i + 1) % coolColors.size], lost >= v)
    }

    // Session-count tiers, scaled to how many active (non-rest) sessions
    // this plan actually contains.
    scaledTiers(planSessions.toDouble()).forEachIndexed { i, v ->
        val n = v.toInt().coerceAtLeast(1)
        list += Achievement(
            if (n <= 1) "Langkah Pertama" else "$n Sesi Selesai",
            if (n <= 1) "Selesaikan sesi pertamamu" else "Total $n dari $planSessions sesi rencana",
            Icons.Filled.CheckCircle, heatColors[(i + 1) % heatColors.size], done >= n
        )
    }

    // Progress-toward-goal tiers (already plan-relative by definition).
    val progressTiers = listOf(
        10.0 to Color(0xFF4E9BE0), 25.0 to Color(0xFF4EC4E0), 50.0 to Color(0xFF4EE0A0),
        75.0 to Color(0xFFD8C74E), 100.0 to Color(0xFFE0C64E)
    )
    progressTiers.forEach { (n, color) ->
        list += Achievement(
            if (n >= 100.0) "Target Tercapai" else "${n.toInt()}% Menuju Target",
            if (n >= 100.0) "Capai 100% dari target beratmu" else "${n.toInt()}% perjalanan menuju target",
            Icons.Filled.GpsFixed, color, progressPct >= n
        )
    }

    // Program-milestone tiers — spread across whatever totalWeeks actually
    // is, so a 15-week plan gets ~4 evenly-spaced checkpoints instead of
    // fixed week 4/8/13/48 markers that would never fire past week 15.
    val weekTiers = scaledTiers(settings.totalWeeks.toDouble())
        .map { it.roundToInt() }
        .filter { it in 1..settings.totalWeeks }
        .distinct()
        .sorted()
    weekTiers.forEach { n ->
        val isFinal = n == settings.totalWeeks
        list += Achievement(
            if (isFinal) "Program Selesai" else "Minggu $n Tercapai",
            if (isFinal) "Sampai di minggu terakhir program (minggu $n)" else "Bertahan sampai minggu ke-$n dari ${settings.totalWeeks}",
            if (isFinal) Icons.Filled.EmojiEvents else Icons.Filled.CalendarMonth,
            if (isFinal) Color(0xFFE0C64E) else Color(0xFF4E9BE0),
            weeksIn >= n
        )
    }

    return list
}

/** Simple linear projection: pace (kg lost per day) from the first to the
 * most recent weigh-in, extrapolated forward to the goal weight. Returns
 * null when there isn't enough history yet, or when the trend isn't
 * actually moving toward the goal (extrapolating a stalled/rising trend
 * would just produce a misleading date). */
data class GoalPrediction(val etaEpochDay: EpochDay, val kgPerWeek: Double)

fun predictGoalDate(state: AppState): GoalPrediction? {
    val history = state.weightHistorySorted
    if (history.size < 2) return null
    val first = history.first()
    val last = history.last()
    val daysElapsed = last.first - first.first
    if (daysElapsed <= 0) return null
    val kgLost = first.second - last.second
    if (kgLost <= 0.01) return null
    val kgPerDay = kgLost / daysElapsed
    val remainingKg = last.second - state.settings.goalWeight
    if (remainingKg <= 0) return GoalPrediction(todayEpochDay(), kgPerDay * 7)
    val daysNeeded = (remainingKg / kgPerDay).roundToInt()
    return GoalPrediction(todayEpochDay() + daysNeeded, kgPerDay * 7)
}

@Composable
fun InsightsScreen(state: AppState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF9B6BFF).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFF9B6BFF).copy(alpha = 0.35f), RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Insights, contentDescription = null, tint = Color(0xFF9B6BFF), modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("Wawasan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        }
        item { PredictionCard(state) }
        item { StatsCard(state) }
        item { AchievementsCard(state) }
        item { ConsistencyHeatmap(state) }
    }
}

@Composable
fun StatsCard(state: AppState) {
    val (done, _) = state.totalSessionsAll
    val totalKm = state.totalKmAll
    val (_, bestStreak) = state.computeStreak()
    val avgKm = if (done > 0) totalKm / done else 0.0
    val totalKcal = estimateCalories(state.currentWeight, totalKm)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = Color(0xFF4E9BE0), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("STATISTIK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total Jarak", "%.0f km".format(totalKm), Icons.Filled.DirectionsBike, Color(0xFF4EE0A0), Modifier.weight(1f))
                StatTile("Sesi Selesai", "$done", Icons.Filled.CheckCircle, Color(0xFFE0C64E), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Streak Terbaik", "$bestStreak hari", Icons.Filled.LocalFireDepartment, Color(0xFFE0A94E), Modifier.weight(1f))
                StatTile("Rata-rata", "%.1f km".format(avgKm), Icons.Filled.GpsFixed, Color(0xFF4E9BE0), Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total Kalori", "≈$totalKcal kkal", Icons.Filled.MonitorWeight, AccentTeal, Modifier.weight(1f))
                StatTile("Minggu Berjalan", "${state.week}/${state.settings.totalWeeks}", Icons.Filled.CalendarMonth, Color(0xFF9B6BFF), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = TextDim, fontSize = 10.sp)
    }
}

@Composable
fun PredictionCard(state: AppState) {
    val prediction = remember(state.weights, state.settings) { predictGoalDate(state) }
    GlassCard(modifier = Modifier.fillMaxWidth(), hero = true, borderColor = AccentTeal) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.TrendingDown, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("PREDIKSI TARGET", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.height(14.dp))
        if (prediction == null) {
            Text(
                "Catat berat badan minimal 2x untuk melihat prediksi kapan target tercapai.",
                color = TextDim,
                fontSize = 12.sp
            )
        } else {
            Text(formatDateId(prediction.etaEpochDay), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Berdasarkan tren saat ini, ~%.2f kg/minggu".format(prediction.kgPerWeek),
                color = TextDim,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AchievementsCard(state: AppState) {
    val achievements = remember(state.completed, state.weights, state.settings) { computeAchievements(state) }
    val unlockedCount = achievements.count { it.unlocked }
    var expanded by remember { mutableStateOf(false) }
    val visible = if (expanded) achievements else achievements.take(6)

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PENCAPAIAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
            Text("$unlockedCount/${achievements.size}", color = TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        val pct = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size else 0f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFE0C64E), AccentTeal)))
            )
        }
        Spacer(Modifier.height(14.dp))
        if (unlockedCount == 0) {
            Text(
                "Selesaikan sesi pertamamu untuk mulai membuka pencapaian.",
                color = TextDim,
                fontSize = 11.5.sp
            )
            Spacer(Modifier.height(10.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            visible.chunked(2).forEach { pair ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { a -> AchievementTile(a, Modifier.weight(1f)) }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        if (achievements.size > 6) {
            Spacer(Modifier.height(12.dp))
            Text(
                if (expanded) "Sembunyikan" else "Lihat semua (${achievements.size})",
                color = AccentTeal,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { expanded = !expanded }
                    .padding(vertical = 6.dp)
            )
        }
    }
}

@Composable
fun AchievementTile(a: Achievement, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (a.unlocked) a.color.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f))
            .border(
                1.dp,
                if (a.unlocked) a.color.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(50))
                .background(if (a.unlocked) a.color.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                a.icon,
                contentDescription = null,
                tint = if (a.unlocked) a.color else TextDim.copy(alpha = 0.5f),
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            a.title,
            color = if (a.unlocked) Color.White else TextDim,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(a.description, color = TextDim, fontSize = 9.5.sp, maxLines = 2)
    }
}

/** GitHub-contribution-style grid: one column per week, one cell per day
 * (rest days dimmed, completed days lit up in the accent color, missed
 * past days flagged in a muted red, future days barely-there). Rendered
 * with LazyRow so a 26+ week program stays cheap to scroll through. */
@Composable
fun ConsistencyHeatmap(state: AppState) {
    val settings = state.settings
    val today = todayEpochDay()
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CalendarViewWeek, contentDescription = null, tint = Color(0xFF4E9BE0), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("KALENDER KONSISTENSI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.6.sp)
        }
        Spacer(Modifier.height(14.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(settings.totalWeeks) { wIdx ->
                val w = wIdx + 1
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    weekPlan(w, settings).forEach { day ->
                        val done = state.completed["$w-${day.key}"] == true
                        val isToday = day.date == today
                        val isFuture = day.date > today
                        val color = when {
                            day.rest -> Color.White.copy(alpha = 0.04f)
                            done -> AccentTeal
                            isFuture -> Color.White.copy(alpha = 0.06f)
                            else -> Color(0xFFE0605C).copy(alpha = 0.4f)
                        }
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                                .then(
                                    if (isToday) Modifier.border(1.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(3.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            HeatmapLegendDot(AccentTeal, "Selesai")
            HeatmapLegendDot(Color(0xFFE0605C).copy(alpha = 0.5f), "Terlewat")
            HeatmapLegendDot(Color.White.copy(alpha = 0.08f), "Akan datang")
        }
    }
}

@Composable
private fun HeatmapLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = TextDim, fontSize = 10.sp)
    }
}

/** Floats above the whole app (all tabs, bottom nav included) and slides
 * down from the top the moment a new achievement unlocks — checked
 * globally here rather than inside InsightsScreen so it fires even if the
 * person is on the Today tab when it happens, not just while looking at
 * the achievements list. */
@Composable
fun AchievementUnlockBanner(state: AppState) {
    val achievements = remember(state.completed, state.weights, state.settings) { computeAchievements(state) }
    var seenTitles by remember { mutableStateOf<Set<String>?>(null) }
    var justUnlocked by remember { mutableStateOf<Achievement?>(null) }

    LaunchedEffect(achievements) {
        val unlockedNow = achievements.filter { it.unlocked }.map { it.title }.toSet()
        val previouslySeen = seenTitles
        if (previouslySeen != null) {
            val newOnes = unlockedNow - previouslySeen
            if (newOnes.isNotEmpty()) {
                justUnlocked = achievements.first { it.title in newOnes }
            }
        }
        // First composition just establishes the baseline (whatever was
        // already unlocked before this session) — nothing to celebrate
        // retroactively for achievements earned in a previous session.
        seenTitles = unlockedNow
    }
    LaunchedEffect(justUnlocked) {
        if (justUnlocked != null) {
            delay(3200)
            justUnlocked = null
        }
    }

    val a = justUnlocked
    AnimatedVisibility(
        visible = a != null,
        enter = slideInVertically(tween(320)) { -it } + fadeIn(tween(220)),
        exit = slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)),
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        if (a != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BgDark2.copy(alpha = 0.96f))
                    .border(1.dp, a.color.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(a.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(a.icon, contentDescription = null, tint = a.color, modifier = Modifier.size(19.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("PENCAPAIAN BARU", color = a.color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
                    Text(a.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
