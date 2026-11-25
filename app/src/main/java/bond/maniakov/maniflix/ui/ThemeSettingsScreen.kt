package bond.maniakov.maniflix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AccentPreset
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.SurfaceStyle
import bond.maniakov.maniflix.data.THEME_PRESETS
import bond.maniakov.maniflix.data.ThemeMode
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    bottomContentPadding: Dp = 24.dp,
    onBack: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val isLight = ManiFlixColors.isLight()

    var customColorSlot by remember { mutableIntStateOf(0) } // 0 = Accent, 1 = Bg, 2 = Surface
    var isSyncingTheme by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "theme_header") {
            SettingsMaxWidthContainer {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            triggerHaptic(haptic)
                            onBack()
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ManiFlixColors.surface())
                            .border(0.6.dp, borderColor, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại Cài đặt",
                            tint = textPrimary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Giao diện & Chủ đề",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${AppPreferences.themeMode.title} • ${AppPreferences.formatColorHex(AppPreferences.customAccentHex)}",
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Spotify Taurus-style Live Preview
        item(key = "taurus_preview") {
            SettingsMaxWidthContainer {
                TaurusLiveThemePreviewCard(
                    accent = accent,
                    bgColor = bgColor,
                    surfaceColor = ManiFlixColors.surface(),
                    surfaceElevated = ManiFlixColors.surfaceElevated(),
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    borderColor = borderColor,
                    isLight = isLight
                )
            }
        }

        // Theme Modes + Presets + Custom RGB Sliders
        item(key = "theme_controls") {
            SettingsSection(title = "Chế độ & Bảng màu", textSecondary = textSecondary) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Chế độ hiển thị",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val selected = AppPreferences.themeMode == mode
                            Box(
                                modifier = appleGlassSurfaceModifier(
                                    cornerRadius = 50.dp,
                                    highlighted = selected
                                )
                                    .clickable {
                                        AppPreferences.themeMode = mode
                                        if (mode != ThemeMode.CUSTOM) {
                                            AppPreferences.activePresetId = mode.name.lowercase()
                                        }
                                        triggerHaptic(haptic)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = mode.title,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected && isLight) accent else textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                SettingsDivider(borderColor)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Bảng màu gợi ý",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(THEME_PRESETS, key = { it.id }) { preset ->
                            val active = AppPreferences.activePresetId == preset.id &&
                                AppPreferences.themeMode == preset.mode
                            Row(
                                modifier = appleGlassSurfaceModifier(
                                    cornerRadius = 12.dp,
                                    highlighted = active
                                )
                                    .clickable {
                                        AppPreferences.applyPreset(preset)
                                        triggerHaptic(haptic)
                                    }
                                    .padding(horizontal = 11.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(preset.bgHex))
                                        .border(2.5.dp, Color(preset.accentHex), CircleShape)
                                )
                                Text(
                                    text = preset.name,
                                    fontSize = 12.5.sp,
                                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (active && isLight) accent else textPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                SettingsDivider(borderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Màu chủ đạo",
                        fontSize = 14.5.sp,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        AccentPreset.entries.forEach { preset ->
                            val selected = AppPreferences.customAccentHex == preset.colorHex
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(if (selected) 1.12f else 1f)
                                    .clip(CircleShape)
                                    .background(Color(preset.colorHex))
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                2.2.dp,
                                                if (isLight) Color(0xFF111114) else Color.White,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        AppPreferences.accentPreset = preset
                                        triggerHaptic(haptic)
                                    }
                            )
                        }
                    }
                }

                if (AppPreferences.themeMode == ThemeMode.CUSTOM) {
                    SettingsDivider(borderColor)
                    CustomThemeEditor(
                        selectedSlot = customColorSlot,
                        onSelectSlot = { customColorSlot = it },
                        accent = accent,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor
                    )
                }

                if (AppState.isLoggedIn) {
                    SettingsDivider(borderColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isSyncingTheme = true
                                    AppState.saveAndSyncTheme()
                                    isSyncingTheme = false
                                }
                            },
                            enabled = !isSyncingTheme,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            if (isSyncingTheme) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Palette,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Lưu & đồng bộ giao diện",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // Display Style & Grid Layout
        item(key = "theme_layout") {
            SettingsSection(title = "Bố cục & Hiệu ứng", textSecondary = textSecondary) {
                SettingsDropdownRow(
                    title = "Phong cách hiển thị",
                    selectedLabel = AppPreferences.surfaceStyle.title,
                    options = SurfaceStyle.entries,
                    labelFor = { it.title },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.surfaceStyle = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsSwitchRow(
                    title = "Ánh nền theo ảnh phim",
                    checked = AppPreferences.ambientArtworkEnabled,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.ambientArtworkEnabled = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Số cột hiển thị",
                    selectedLabel = "${AppPreferences.gridColumnsPhone} cột",
                    options = listOf(2, 3),
                    labelFor = { "$it cột" },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.gridColumnsPhone = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsSwitchRow(
                    title = "Rung phản hồi",
                    checked = AppPreferences.hapticsEnabled,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.hapticsEnabled = it
                        if (it) triggerHaptic(haptic)
                    }
                )
            }
        }
    }
}

@Composable
private fun TaurusLiveThemePreviewCard(
    accent: Color,
    bgColor: Color,
    surfaceColor: Color,
    surfaceElevated: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    isLight: Boolean
) {
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 600
    val previewShape = RoundedCornerShape(18.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(appleGlassSurfaceModifier(cornerRadius = 18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "XEM TRƯỚC TRỰC TIẾP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
                color = textSecondary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${AppPreferences.themeMode.title} • ${AppPreferences.surfaceStyle.title}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                maxLines = 1
            )
        }

        // Miniature ManiFlix App Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(previewShape)
                .background(bgColor)
                .border(0.8.dp, borderColor, previewShape)
        ) {
            if (AppPreferences.ambientArtworkEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    accent.copy(alpha = if (isLight) 0.16f else 0.28f),
                                    bgColor
                                )
                            )
                        )
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                if (isWideScreen) {
                    // Mini NavigationRail on Tablet / Fold
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .background(surfaceColor.copy(alpha = 0.96f))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            Icons.Rounded.VideoLibrary to true,
                            Icons.Rounded.Search to false,
                            Icons.Rounded.Bookmarks to false,
                            Icons.Rounded.Settings to false
                        ).forEach { (icon, active) ->
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (active) accent.copy(alpha = 0.18f) else Color.Transparent)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (active) accent else textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mini Hero Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(106.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.55f),
                                        Color(0xFF1A1625),
                                        Color(0xFF0E0F14)
                                    )
                                )
                            )
                            .border(0.6.dp, borderColor, RoundedCornerShape(12.dp))
                            .padding(11.dp)
                    ) {
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "PHIM MỚI • FHD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = accent
                            )
                            Text(
                                text = "Thanh Gươm Diệt Quỷ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(accent)
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "Xem ngay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bookmark,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = "Tủ phim",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // 2 Mini 2:3 Poster Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            "Jujutsu Kaisen" to "Tập 24 • Vietsub",
                            "Dune: Phần Hai" to "2024 • 4K HDR"
                        ).forEach { (title, meta) ->
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(surfaceColor)
                                    .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
                                    .padding(7.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .aspectRatio(2f / 3f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    accent.copy(alpha = 0.36f),
                                                    surfaceElevated
                                                )
                                            )
                                        )
                                ) {
                                    Text(
                                        text = "FHD",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(3.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color.Black.copy(alpha = 0.65f))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = meta,
                                        fontSize = 9.5.sp,
                                        color = textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    if (!isWideScreen) {
                        // Mini NavigationBar on Phone
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(surfaceColor.copy(alpha = 0.96f))
                                .border(0.6.dp, borderColor, RoundedCornerShape(12.dp))
                                .padding(vertical = 7.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Icons.Rounded.VideoLibrary to "Trang chủ",
                                Icons.Rounded.Search to "Tìm kiếm",
                                Icons.Rounded.Bookmarks to "Tủ phim",
                                Icons.Rounded.Settings to "Cài đặt"
                            ).forEachIndexed { idx, (icon, label) ->
                                val active = idx == 0
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (active) accent else textSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (active) accent else textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomThemeEditor(
    selectedSlot: Int,
    onSelectSlot: (Int) -> Unit,
    accent: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    val activeColorLong = when (selectedSlot) {
        0 -> AppPreferences.customAccentHex
        1 -> AppPreferences.customBgHex
        else -> AppPreferences.customSurfaceHex
    }

    fun updateSlotColor(newHex: Long) {
        AppPreferences.themeMode = ThemeMode.CUSTOM
        AppPreferences.activePresetId = "custom"
        when (selectedSlot) {
            0 -> AppPreferences.customAccentHex = newHex
            1 -> AppPreferences.customBgHex = newHex
            else -> AppPreferences.customSurfaceHex = newHex
        }
    }

    val quickPalette = when (selectedSlot) {
        0 -> listOf(0xFFFA2D48, 0xFFFF6448, 0xFF0A84FF, 0xFF5E5CE6, 0xFF30D158, 0xFFFF375F, 0xFFFFD60A)
        1 -> listOf(0xFF121214, 0xFF000000, 0xFF0B1020, 0xFF0B1612, 0xFF1B1224, 0xFFF4F4F7, 0xFFFFFFFF)
        else -> listOf(0xFF1C1C1E, 0xFF141416, 0xFF151C32, 0xFF14241D, 0xFF281B35, 0xFFFFFFFF, 0xFFE9ECF2)
    }

    val red = ((activeColorLong shr 16) and 0xFF).toInt()
    val green = ((activeColorLong shr 8) and 0xFF).toInt()
    val blue = (activeColorLong and 0xFF).toInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tuỳ chỉnh màu",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = AppPreferences.formatColorHex(activeColorLong),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = textSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                0 to "Chủ đạo",
                1 to "Màu nền",
                2 to "Màu thẻ"
            ).forEach { (slotIdx, label) ->
                val active = selectedSlot == slotIdx
                val dotHex = when (slotIdx) {
                    0 -> AppPreferences.customAccentHex
                    1 -> AppPreferences.customBgHex
                    else -> AppPreferences.customSurfaceHex
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .then(appleGlassSurfaceModifier(cornerRadius = 10.dp, highlighted = active))
                        .clickable { onSelectSlot(slotIdx) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(dotHex))
                            .border(0.8.dp, borderColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        color = textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            quickPalette.forEach { hex ->
                val selected = activeColorLong == hex
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(hex))
                        .border(
                            width = if (selected) 2.2.dp else 0.8.dp,
                            color = if (selected) accent else borderColor,
                            shape = CircleShape
                        )
                        .clickable { updateSlotColor(hex) }
                )
            }
        }

        RgbChannelSlider(
            label = "R",
            value = red,
            trackColor = Color(0xFFFF453A),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            onChange = { newR ->
                val argb = 0xFF000000L or ((newR.toLong() and 0xFF) shl 16) or
                    ((green.toLong() and 0xFF) shl 8) or (blue.toLong() and 0xFF)
                updateSlotColor(argb)
            }
        )
        RgbChannelSlider(
            label = "G",
            value = green,
            trackColor = Color(0xFF30D158),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            onChange = { newG ->
                val argb = 0xFF000000L or ((red.toLong() and 0xFF) shl 16) or
                    ((newG.toLong() and 0xFF) shl 8) or (blue.toLong() and 0xFF)
                updateSlotColor(argb)
            }
        )
        RgbChannelSlider(
            label = "B",
            value = blue,
            trackColor = Color(0xFF0A84FF),
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            borderColor = borderColor,
            onChange = { newB ->
                val argb = 0xFF000000L or ((red.toLong() and 0xFF) shl 16) or
                    ((green.toLong() and 0xFF) shl 8) or (newB.toLong() and 0xFF)
                updateSlotColor(argb)
            }
        )
    }
}

@Composable
private fun RgbChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    onChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            modifier = Modifier.width(16.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = borderColor
            ),
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
        )
        Text(
            text = value.toString().padStart(3, ' '),
            fontSize = 11.5.sp,
            fontFamily = FontFamily.Monospace,
            color = textSecondary,
            modifier = Modifier.width(28.dp)
        )
    }
}
