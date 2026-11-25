package bond.maniakov.maniflix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import bond.maniakov.maniflix.data.AppPreferences

object ManiFlixColors {
    fun background(oled: Boolean): Color =
        if (oled) Color(0xFF000000) else Color(0xFF121214)

    fun surface(oled: Boolean): Color =
        if (oled) Color(0xFF141416) else Color(0xFF1C1C1E)

    fun surfaceElevated(oled: Boolean): Color =
        if (oled) Color(0xFF1E1E22) else Color(0xFF26262A)

    fun accent(): Color = Color(AppPreferences.accentPreset.colorHex)
}

@Composable
fun ManiFlixTheme(content: @Composable () -> Unit) {
    val oled = AppPreferences.oledBlackEnabled
    val accent = ManiFlixColors.accent()
    val bg = ManiFlixColors.background(oled)
    val surface = ManiFlixColors.surface(oled)
    val surfaceVariant = ManiFlixColors.surfaceElevated(oled)

    val colorScheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        secondary = accent,
        onSecondary = Color.White,
        tertiary = accent,
        background = bg,
        onBackground = Color.White,
        surface = surface,
        onSurface = Color.White,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFFAEAEB2),
        outline = Color(0x26FFFFFF),
        outlineVariant = Color(0x14FFFFFF)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
