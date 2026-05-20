package com.tasktracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.tasktracker.data.models.AppTheme

// Default purple scheme (existing)
private val PurpleScheme = darkColorScheme(
    primary = DarkPrimary, onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer, onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary, error = DarkError, onError = DarkOnError,
    background = DarkBackground, onBackground = DarkOnBackground,
    surface = DarkSurface, onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val BlueScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4488FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF001A6B),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF003390),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFD4E2FF),
    secondary = androidx.compose.ui.graphics.Color(0xFF82CFFF),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF003549),
    background = androidx.compose.ui.graphics.Color(0xFF0A0F1E),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E4FF),
    surface = androidx.compose.ui.graphics.Color(0xFF141929),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E4FF),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E2840),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB8C4E0),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF3D4F70)
)

private val GreenScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4CAF50),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF002106),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF00390E),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFB6F5B8),
    secondary = androidx.compose.ui.graphics.Color(0xFF8BC34A),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1B3700),
    background = androidx.compose.ui.graphics.Color(0xFF0A120A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1F5E1),
    surface = androidx.compose.ui.graphics.Color(0xFF141E14),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1F5E1),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1E2E1E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB8D8B8),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF3D5C3D)
)

private val RedScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFCF4444),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF690000),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF93000A),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDAD6),
    secondary = androidx.compose.ui.graphics.Color(0xFFFF8A80),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF690000),
    background = androidx.compose.ui.graphics.Color(0xFF180A0A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF5E0E0),
    surface = androidx.compose.ui.graphics.Color(0xFF281414),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF5E0E0),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3A1E1E),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD8B8B8),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF703D3D)
)

private val TealScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF00BCD4),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF003640),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF004E5B),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFA2EEFF),
    secondary = androidx.compose.ui.graphics.Color(0xFF4DD0E1),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF00363D),
    background = androidx.compose.ui.graphics.Color(0xFF080F10),
    onBackground = androidx.compose.ui.graphics.Color(0xFFDFF5F8),
    surface = androidx.compose.ui.graphics.Color(0xFF10191A),
    onSurface = androidx.compose.ui.graphics.Color(0xFFDFF5F8),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF1A2829),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFB0D0D5),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF2D5055)
)

private val OrangeScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFFF9800),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF4A2000),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF6E3200),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDB0),
    secondary = androidx.compose.ui.graphics.Color(0xFFFFCC80),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF4A2C00),
    background = androidx.compose.ui.graphics.Color(0xFF18100A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF5EDE0),
    surface = androidx.compose.ui.graphics.Color(0xFF251A10),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF5EDE0),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF382614),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD8C0A0),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF705030)
)

private val PinkScheme = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFE91E63),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF5C0026),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF880038),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFFFD9E3),
    secondary = androidx.compose.ui.graphics.Color(0xFFF48FB1),
    onSecondary = androidx.compose.ui.graphics.Color(0xFF5C0026),
    background = androidx.compose.ui.graphics.Color(0xFF180A10),
    onBackground = androidx.compose.ui.graphics.Color(0xFFF5E0EC),
    surface = androidx.compose.ui.graphics.Color(0xFF281420),
    onSurface = androidx.compose.ui.graphics.Color(0xFFF5E0EC),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF3A1E2A),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFD8B0C0),
    error = DarkError, onError = DarkOnError,
    outline = androidx.compose.ui.graphics.Color(0xFF703D55)
)

fun appColorScheme(theme: AppTheme) = when (theme) {
    AppTheme.PURPLE -> PurpleScheme
    AppTheme.BLUE -> BlueScheme
    AppTheme.GREEN -> GreenScheme
    AppTheme.RED -> RedScheme
    AppTheme.TEAL -> TealScheme
    AppTheme.ORANGE -> OrangeScheme
    AppTheme.PINK -> PinkScheme
}

@Composable
fun TaskTrackerTheme(appTheme: AppTheme = AppTheme.PURPLE, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = appColorScheme(appTheme),
        typography = TaskTrackerTypography,
        content = content
    )
}
