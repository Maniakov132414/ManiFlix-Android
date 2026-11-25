package bond.maniakov.maniflix.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.ThemeMode

object ManiFlixColors {

    fun luminance(argb: Long): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    @Composable
    fun isLight(): Boolean = when (AppPreferences.themeMode) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK, ThemeMode.OLED -> false
        ThemeMode.SYSTEM -> !isSystemInDarkTheme()
        ThemeMode.CUSTOM -> luminance(AppPreferences.customBgHex) > 0.55f
    }

    @Composable
    fun background(): Color = when (AppPreferences.themeMode) {
        ThemeMode.LIGHT -> Color(0xFFF4F4F7)
        ThemeMode.DARK -> Color(0xFF121214)
        ThemeMode.OLED -> Color(0xFF000000)
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) Color(0xFF121214) else Color(0xFFF4F4F7)
        ThemeMode.CUSTOM -> Color(AppPreferences.customBgHex)
    }

    @Composable
    fun surface(): Color = when (AppPreferences.themeMode) {
        ThemeMode.LIGHT -> Color(0xFFFFFFFF)
        ThemeMode.DARK -> Color(0xFF1C1C1E)
        ThemeMode.OLED -> Color(0xFF141416)
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        ThemeMode.CUSTOM -> Color(AppPreferences.customSurfaceHex)
    }

    @Composable
    fun surfaceElevated(): Color = when (AppPreferences.themeMode) {
        ThemeMode.LIGHT -> Color(0xFFE9E9EE)
        ThemeMode.DARK -> Color(0xFF26262A)
        ThemeMode.OLED -> Color(0xFF1E1E22)
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) Color(0xFF26262A) else Color(0xFFE9E9EE)
        ThemeMode.CUSTOM -> {
            val base = Color(AppPreferences.customSurfaceHex)
            if (isLight()) lerp(base, Color.Black, 0.06f) else lerp(base, Color.White, 0.07f)
        }
    }

    @Composable
    fun textPrimary(): Color =
        if (isLight()) Color(0xFF111114) else Color(0xFFFFFFFF)

    @Composable
    fun textSecondary(): Color =
        if (isLight()) Color(0xFF5C5C66) else Color(0xFFAEAEB2)

    @Composable
    fun cardBorder(): Color =
        if (isLight()) Color(0x18000000) else Color(0x26FFFFFF)

    fun background(oled: Boolean): Color =
        when (AppPreferences.themeMode) {
            ThemeMode.LIGHT -> Color(0xFFF4F4F7)
            ThemeMode.OLED -> Color(0xFF000000)
            ThemeMode.CUSTOM -> Color(AppPreferences.customBgHex)
            else -> if (oled) Color(0xFF000000) else Color(0xFF121214)
        }

    fun surface(oled: Boolean): Color =
        when (AppPreferences.themeMode) {
            ThemeMode.LIGHT -> Color(0xFFFFFFFF)
            ThemeMode.OLED -> Color(0xFF141416)
            ThemeMode.CUSTOM -> Color(AppPreferences.customSurfaceHex)
            else -> if (oled) Color(0xFF141416) else Color(0xFF1C1C1E)
        }

    fun surfaceElevated(oled: Boolean): Color =
        if (oled) Color(0xFF1E1E22) else Color(0xFF26262A)

    fun accent(): Color =
        if (AppPreferences.themeMode == ThemeMode.CUSTOM) {
            Color(AppPreferences.customAccentHex)
        } else {
            Color(AppPreferences.customAccentHex)
        }
}

@Composable
fun ManiFlixTheme(content: @Composable () -> Unit) {
    val light = ManiFlixColors.isLight()
    val accent = ManiFlixColors.accent()
    val bg = ManiFlixColors.background()
    val surface = ManiFlixColors.surface()
    val surfaceVariant = ManiFlixColors.surfaceElevated()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val border = ManiFlixColors.cardBorder()

    val colorScheme = if (light) {
        lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent,
            onSecondary = Color.White,
            tertiary = accent,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = Color(0x10000000)
        )
    } else {
        darkColorScheme(
            primary = accent,
            onPrimary = Color.White,
            secondary = accent,
            onSecondary = Color.White,
            tertiary = accent,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = Color(0x14FFFFFF)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
