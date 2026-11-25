package bond.maniakov.maniflix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import bond.maniakov.maniflix.data.ExternalPlayerOption
import bond.maniakov.maniflix.data.IntroSkipBehavior
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.data.SurfaceStyle
import bond.maniakov.maniflix.data.THEME_PRESETS
import bond.maniakov.maniflix.data.ThemeMode
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.player.PlayerSessionHolder
import bond.maniakov.maniflix.player.SpatialAudioController
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    bottomContentPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val isLight = ManiFlixColors.isLight()

    val audioOutputLabel = remember { SpatialAudioController.currentAudioOutputLabel(context) }
    var customColorSlot by remember { mutableIntStateOf(0) } // 0 = Accent, 1 = Bg, 2 = Surface
    var isSyncingTheme by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "settings_title") {
            SettingsMaxWidthContainer {
                Text(
                    text = "Cài đặt",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    modifier = Modifier.statusBarsPadding()
                )
            }
        }

        // Tài khoản
        item(key = "sec_account") {
            SettingsSection(title = "Tài khoản", textSecondary = textSecondary) {
                val user = AppState.currentUser
                if (AppState.isLoggedIn && user != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user.displayName.ifBlank { user.username },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "@${user.username}",
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Đăng xuất",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A),
                            maxLines = 1,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    triggerHaptic(haptic)
                                    AppState.logout()
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                triggerHaptic(haptic)
                                AppState.showAuthSheet = true
                            }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Đăng nhập hoặc tạo tài khoản",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Giao diện
        item(key = "sec_appearance") {
            SettingsSection(title = "Giao diện", textSecondary = textSecondary) {
                // Theme mode pills
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

                // Curated Theme Presets
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

                // Accent swatches
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

                // Custom Theme Editor when CUSTOM mode is active
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

                SettingsDivider(borderColor)

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

        // Âm thanh (Spatial removed cleanly on Android)
        item(key = "sec_audio") {
            SettingsSection(title = "Âm thanh", textSecondary = textSecondary) {
                SettingsInfoRow(
                    title = "Đầu ra âm thanh",
                    value = audioOutputLabel,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Cấu hình âm thanh",
                    selectedLabel = AppPreferences.soundProfile.title,
                    options = SoundProfileMode.entries,
                    labelFor = { it.title },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.soundProfile = it
                        PlayerSessionHolder.activeEngine?.refreshAudioEffects()
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tăng cường âm lượng",
                            fontSize = 14.5.sp,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(AppPreferences.comfortGain * 100f).roundToInt()}%",
                            fontSize = 13.sp,
                            color = textSecondary
                        )
                    }
                    Slider(
                        value = AppPreferences.comfortGain,
                        onValueChange = {
                            AppPreferences.comfortGain = it
                            PlayerSessionHolder.activeEngine?.refreshAudioEffects()
                        },
                        valueRange = 0.5f..1.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = accent,
                            activeTrackColor = accent,
                            inactiveTrackColor = borderColor
                        )
                    )
                }
            }
        }

        // Trình phát
        item(key = "sec_player") {
            SettingsSection(title = "Trình phát", textSecondary = textSecondary) {
                SettingsSwitchRow(
                    title = "Tự mở Picture in Picture",
                    checked = AppPreferences.autoPiP,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.autoPiP = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsSwitchRow(
                    title = "Tự xoay ngang khi xem",
                    checked = AppPreferences.autoRotateLandscape,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.autoRotateLandscape = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsSwitchRow(
                    title = "Tự cắt đoạn quảng cáo",
                    checked = AppPreferences.autoCutAds,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.autoCutAds = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsSwitchRow(
                    title = "Tự chuyển tập tiếp theo",
                    checked = AppPreferences.autoNextEpisode,
                    accent = accent,
                    textPrimary = textPrimary,
                    borderColor = borderColor,
                    onCheckedChange = {
                        AppPreferences.autoNextEpisode = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Mở đầu & kết thúc",
                    selectedLabel = AppPreferences.introSkipBehavior.title,
                    options = IntroSkipBehavior.entries,
                    labelFor = { it.title },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.introSkipBehavior = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Chạm đúp để tua",
                    selectedLabel = "${AppPreferences.doubleTapSeekSeconds} giây",
                    options = listOf(5, 10, 15, 30),
                    labelFor = { "$it giây" },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.doubleTapSeekSeconds = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Nhấn giữ để tăng tốc",
                    selectedLabel = "${AppPreferences.longPressSpeed}x",
                    options = listOf(1.5f, 2.0f, 2.5f, 3.0f),
                    labelFor = { "${it}x" },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.longPressSpeed = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider(borderColor)

                SettingsDropdownRow(
                    title = "Trình phát mặc định",
                    selectedLabel = AppPreferences.externalPlayer.title,
                    options = ExternalPlayerOption.entries,
                    labelFor = { it.title },
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    accent = accent,
                    onSelect = {
                        AppPreferences.externalPlayer = it
                        triggerHaptic(haptic)
                    }
                )
            }
        }

        // Ứng dụng
        item(key = "sec_app") {
            SettingsSection(title = "Ứng dụng", textSecondary = textSecondary) {
                SettingsInfoRow(
                    title = "Phiên bản",
                    value = UpdateManager.displayVersion,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary
                )

                SettingsDivider(borderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            triggerHaptic(haptic)
                            scope.launch { UpdateManager.checkForUpdates(manual = true) }
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kiểm tra cập nhật",
                        fontSize = 14.5.sp,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (UpdateManager.isChecking) {
                        CircularProgressIndicator(
                            color = accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (UpdateManager.lastCheckNote.isNotBlank()) {
                    SettingsDivider(borderColor)
                    Text(
                        text = UpdateManager.lastCheckNote,
                        fontSize = 12.sp,
                        color = textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                SettingsDivider(borderColor)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            AppPreferences.resetToDefaults()
                            triggerHaptic(haptic, strong = true)
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Đặt lại mặc định",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFF453A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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

        // Slot tabs: Màu chủ đạo | Màu nền | Màu thẻ
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

        // Quick pick swatches for active slot
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

        // RGB sliders
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

@Composable
private fun SettingsMaxWidthContainer(
    content: @Composable () -> Unit
) {
    val isWide = LocalConfiguration.current.screenWidthDp >= 700
    if (isWide) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 720.dp)
                    .fillMaxWidth()
            ) {
                content()
            }
        }
    } else {
        content()
    }
}

@Composable
private fun SettingsSection(
    title: String,
    textSecondary: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsMaxWidthContainer {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title.uppercase(),
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(appleGlassSurfaceModifier(cornerRadius = 14.dp)),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsDivider(borderColor: Color) {
    HorizontalDivider(
        color = borderColor,
        modifier = Modifier.padding(start = 14.dp)
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.5.sp,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    accent: Color,
    textPrimary: Color,
    borderColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.5.sp,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = textPrimary.copy(alpha = 0.7f),
                uncheckedTrackColor = borderColor,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun <T> SettingsDropdownRow(
    title: String,
    selectedLabel: String,
    options: List<T>,
    labelFor: (T) -> String,
    textPrimary: Color,
    textSecondary: Color,
    accent: Color,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedLabel,
                fontSize = 13.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = ManiFlixColors.surfaceElevated()
        ) {
            options.forEach { option ->
                val label = labelFor(option)
                val isSelected = label == selectedLabel
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (isSelected) accent else textPrimary,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}
