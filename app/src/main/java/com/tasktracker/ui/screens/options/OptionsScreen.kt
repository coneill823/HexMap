package com.tasktracker.ui.screens.options

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tasktracker.data.models.AppTheme
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

private val themeColors = mapOf(
    AppTheme.PURPLE to Color(0xFF9C71FF),
    AppTheme.BLUE to Color(0xFF4488FF),
    AppTheme.GREEN to Color(0xFF4CAF50),
    AppTheme.RED to Color(0xFFCF4444),
    AppTheme.TEAL to Color(0xFF00BCD4),
    AppTheme.ORANGE to Color(0xFFFF9800),
    AppTheme.PINK to Color(0xFFE91E63)
)

private data class HabitLawSection(
    val sectionHeader: String,
    val laws: List<HabitLaw>
)

private data class HabitLaw(
    val header: String,
    val items: List<Pair<String, String>>
)

private val habitSections = listOf(
    HabitLawSection(
        sectionHeader = "HOW TO BUILD A GOOD HABIT",
        laws = listOf(
            HabitLaw(
                header = "The 1st Law — Make It Obvious",
                items = listOf(
                    "1.1" to "Fill out the Habits Scorecard. Write down your current habits to become aware of them.",
                    "1.2" to "Use implementation intentions: \"I will [BEHAVIOR] at [TIME] in [LOCATION].\"",
                    "1.3" to "Use habit stacking: \"After [CURRENT HABIT], I will [NEW HABIT].\"",
                    "1.4" to "Design your environment. Make the cues of good habits obvious and visible."
                )
            ),
            HabitLaw(
                header = "The 2nd Law — Make It Attractive",
                items = listOf(
                    "2.1" to "Use temptation bundling. Pair an action you want to do with an action you need to do.",
                    "2.2" to "Join a culture where your desired behavior is the normal behavior.",
                    "2.3" to "Create a motivation ritual. Do something you enjoy immediately before a difficult habit."
                )
            ),
            HabitLaw(
                header = "The 3rd Law — Make It Easy",
                items = listOf(
                    "3.1" to "Reduce friction. Decrease the number of steps between you and your good habits.",
                    "3.2" to "Prime the environment. Prepare your environment to make future actions easier.",
                    "3.3" to "Master the decisive moment. Optimize the small choices that deliver outsized impact.",
                    "3.4" to "Use the Two-Minute Rule. Downscale your habits until they can be done in two minutes or less.",
                    "3.5" to "Automate your habits. Invest in technology and onetime purchases that lock in future behavior."
                )
            ),
            HabitLaw(
                header = "The 4th Law — Make It Satisfying",
                items = listOf(
                    "4.1" to "Use reinforcement. Give yourself an immediate reward when you complete your habit.",
                    "4.2" to "Make \"doing nothing\" enjoyable. When avoiding a bad habit, design a way to see the benefits.",
                    "4.3" to "Use a habit tracker. Keep track of your habit streak and \"don't break the chain.\"",
                    "4.4" to "Never miss twice. When you forget to do a habit, make sure you get back on track immediately."
                )
            )
        )
    ),
    HabitLawSection(
        sectionHeader = "HOW TO BREAK A BAD HABIT",
        laws = listOf(
            HabitLaw(
                header = "Inversion of the 1st Law — Make It Invisible",
                items = listOf(
                    "1.5" to "Reduce exposure. Remove the cues of your bad habits from your environment."
                )
            ),
            HabitLaw(
                header = "Inversion of the 2nd Law — Make It Unattractive",
                items = listOf(
                    "2.4" to "Reframe your mindset. Highlight the benefits of avoiding your bad habits."
                )
            ),
            HabitLaw(
                header = "Inversion of the 3rd Law — Make It Difficult",
                items = listOf(
                    "3.6" to "Increase friction. Increase the number of steps between you and your bad habits.",
                    "3.7" to "Use a commitment device. Restrict your future choices to the ones that benefit you."
                )
            ),
            HabitLaw(
                header = "Inversion of the 4th Law — Make It Unsatisfying",
                items = listOf(
                    "4.5" to "Get an accountability partner. Ask someone to watch your behavior.",
                    "4.6" to "Create a habit contract. Make the costs of your bad habits public and painful."
                )
            )
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionsScreen(viewModel: OptionsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Options") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = state.selectedTab) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Theme") }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = { Text("Recovery") }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = { Text("Info") }
                )
            }

            when (state.selectedTab) {
                0 -> ThemeTab(
                    currentTheme = state.currentTheme,
                    onThemeSelected = { viewModel.setTheme(it) }
                )
                1 -> RecoveryTab(
                    deletedRoutines = state.deletedRoutines,
                    onRestore = { viewModel.restoreRoutine(it) }
                )
                2 -> InfoTab()
            }
        }
    }
}

@Composable
private fun ThemeTab(
    currentTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Choose a color theme",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        // 2-column grid of theme swatches
        val themes = AppTheme.values().toList()
        items(themes.chunked(2)) { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowThemes.forEach { theme ->
                    ThemeSwatch(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onSelect = { onThemeSelected(theme) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (rowThemes.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = themeColors[theme] ?: Color.Gray

    Card(
        modifier = modifier
            .clickable { onSelect() }
            .then(
                if (isSelected) Modifier.border(2.dp, color, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun RecoveryTab(
    deletedRoutines: List<com.tasktracker.data.database.entities.Routine>,
    onRestore: (Long) -> Unit
) {
    if (deletedRoutines.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No routines in recovery",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Routines are permanently deleted after 7 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(deletedRoutines) { routine ->
                RecoveryRoutineRow(
                    routine = routine,
                    onRestore = { onRestore(routine.id) }
                )
            }
        }
    }
}

@Composable
private fun RecoveryRoutineRow(
    routine: com.tasktracker.data.database.entities.Routine,
    onRestore: () -> Unit
) {
    val deletedAt = routine.deletedAt
    val deletionDate = deletedAt?.let {
        val ldt = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDateTime()
        ldt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } ?: "Unknown"

    val timeRemainingMs = deletedAt?.let {
        7 * 24 * 60 * 60 * 1000L - (System.currentTimeMillis() - it)
    } ?: 0L

    val timeRemainingText = if (timeRemainingMs <= 0L) {
        "Expiring soon"
    } else {
        val days = TimeUnit.MILLISECONDS.toDays(timeRemainingMs)
        val hours = TimeUnit.MILLISECONDS.toHours(timeRemainingMs) % 24
        when {
            days > 0 -> "${days}d ${hours}h remaining"
            hours > 0 -> "${hours}h remaining"
            else -> "Less than 1 hour remaining"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Deleted: $deletionDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeRemainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            IconButton(onClick = onRestore) {
                Icon(
                    imageVector = Icons.Filled.Restore,
                    contentDescription = "Restore",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun InfoTab() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        habitSections.forEach { section ->
            item {
                Text(
                    text = section.sectionHeader,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            section.laws.forEach { law ->
                item {
                    // Law header row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = law.header,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                items(law.items) { (number, text) ->
                    HabitItemRow(number = number, text = text)
                }

                item {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitItemRow(number: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 0.5.dp
        )
    }
}
