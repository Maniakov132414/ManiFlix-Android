package bond.maniakov.maniflix.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AccentPreset
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ExternalPlayerOption
import bond.maniakov.maniflix.data.IntroSkipBehavior
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.data.SpatialAudioMode
import bond.maniakov.maniflix.data.SurfaceStyle
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.player.PlayerSessionHolder
import bond.maniakov.maniflix.player.SpatialAudioController
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    bottomContentPadding: Dp = 24.dp
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val oled = AppPreferences.oledBlackEnabled
    val accent = ManiFlixColors.accent()

    val audioOutputLabel = remember { SpatialAudioController.currentAudioOutputLabel(context) }
    val spatialAvailable = remember { SpatialAudioController.isHardwareSpatializerAvailable(context) }
    val headTrackingSupported = remember { SpatialAudioController.isHeadTrackingSupported(context) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ManiFlixColors.background(oled)),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "settings_title") {
            Text(
                text = "Cài đặt",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.statusBarsPadding()
            )
        }

        // Tài khoản
        item(key = "sec_account") {
            SettingsSection(title = "Tài khoản") {
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
                                color = Color.White
                            )
                            Text(
                                text = "@${user.username}",
                                fontSize = 12.sp,
                                color = Color(0xFF98989F)
                            )
                        }

                        Text(
                            text = "Đăng xuất",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFF453A),
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
                            color = accent
                        )
                    }
                }
            }
        }

        // Giao diện
        item(key = "sec_appearance") {
            SettingsSection(title = "Giao diện") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Màu chủ đạo",
                        fontSize = 14.5.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentPreset.entries.forEach { preset ->
                            val selected = AppPreferences.accentPreset == preset
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(if (selected) 1.12f else 1f)
                                    .clip(CircleShape)
                                    .background(Color(preset.colorHex))
                                    .then(
                                        if (selected) {
                                            Modifier.border(2.2.dp, Color.White, CircleShape)
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

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Phong cách hiển thị",
                    selectedLabel = AppPreferences.surfaceStyle.title,
                    options = SurfaceStyle.entries,
                    labelFor = { it.title },
                    onSelect = {
                        AppPreferences.surfaceStyle = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Ánh nền theo ảnh phim",
                    checked = AppPreferences.ambientArtworkEnabled,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.ambientArtworkEnabled = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Nền đen thuần (OLED)",
                    checked = AppPreferences.oledBlackEnabled,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.oledBlackEnabled = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Số cột hiển thị",
                    selectedLabel = "${AppPreferences.gridColumnsPhone} cột",
                    options = listOf(2, 3),
                    labelFor = { "$it cột" },
                    onSelect = {
                        AppPreferences.gridColumnsPhone = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Rung phản hồi",
                    checked = AppPreferences.hapticsEnabled,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.hapticsEnabled = it
                        if (it) triggerHaptic(haptic)
                    }
                )
            }
        }

        // Âm thanh
        item(key = "sec_audio") {
            SettingsSection(title = "Âm thanh") {
                SettingsInfoRow(
                    title = "Đầu ra âm thanh",
                    value = audioOutputLabel
                )

                SettingsDivider()

                val spatialStatusText = when {
                    spatialAvailable && headTrackingSupported -> "Hỗ trợ • Theo dõi đầu"
                    spatialAvailable -> "Hỗ trợ phần cứng"
                    else -> "Stereo / Không khả dụng"
                }
                SettingsInfoRow(
                    title = "Spatializer phần cứng",
                    value = spatialStatusText
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Âm thanh không gian",
                    selectedLabel = AppPreferences.spatialAudioMode.title,
                    options = SpatialAudioMode.entries,
                    labelFor = { it.title },
                    onSelect = {
                        AppPreferences.spatialAudioMode = it
                        PlayerSessionHolder.activeEngine?.refreshAudioEffects()
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Cấu hình âm",
                    selectedLabel = AppPreferences.soundProfile.title,
                    options = SoundProfileMode.entries,
                    labelFor = { it.title },
                    onSelect = {
                        AppPreferences.soundProfile = it
                        PlayerSessionHolder.activeEngine?.refreshAudioEffects()
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Âm lượng nội bộ",
                            fontSize = 14.5.sp,
                            color = Color.White
                        )
                        Text(
                            text = "${(AppPreferences.comfortGain * 100f).roundToInt()}%",
                            fontSize = 13.sp,
                            color = Color(0xFF98989F)
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
                            inactiveTrackColor = Color.White.copy(alpha = 0.16f)
                        )
                    )
                }
            }
        }

        // Trình phát
        item(key = "sec_player") {
            SettingsSection(title = "Trình phát") {
                SettingsSwitchRow(
                    title = "Tự mở Picture in Picture",
                    checked = AppPreferences.autoPiP,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.autoPiP = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Tự xoay ngang khi xem",
                    checked = AppPreferences.autoRotateLandscape,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.autoRotateLandscape = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Tự cắt đoạn quảng cáo",
                    checked = AppPreferences.autoCutAds,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.autoCutAds = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsSwitchRow(
                    title = "Tự chuyển tập tiếp theo",
                    checked = AppPreferences.autoNextEpisode,
                    accent = accent,
                    onCheckedChange = {
                        AppPreferences.autoNextEpisode = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Mở đầu & kết thúc",
                    selectedLabel = AppPreferences.introSkipBehavior.title,
                    options = IntroSkipBehavior.entries,
                    labelFor = { it.title },
                    onSelect = {
                        AppPreferences.introSkipBehavior = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Chạm đúp để tua",
                    selectedLabel = "${AppPreferences.doubleTapSeekSeconds} giây",
                    options = listOf(5, 10, 15, 30),
                    labelFor = { "$it giây" },
                    onSelect = {
                        AppPreferences.doubleTapSeekSeconds = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Nhấn giữ để tăng tốc",
                    selectedLabel = "${AppPreferences.longPressSpeed}x",
                    options = listOf(1.5f, 2.0f, 2.5f, 3.0f),
                    labelFor = { "${it}x" },
                    onSelect = {
                        AppPreferences.longPressSpeed = it
                        triggerHaptic(haptic)
                    }
                )

                SettingsDivider()

                SettingsDropdownRow(
                    title = "Trình phát mặc định",
                    selectedLabel = AppPreferences.externalPlayer.title,
                    options = ExternalPlayerOption.entries,
                    labelFor = { it.title },
                    onSelect = {
                        AppPreferences.externalPlayer = it
                        triggerHaptic(haptic)
                    }
                )
            }
        }

        // Ứng dụng
        item(key = "sec_app") {
            SettingsSection(title = "Ứng dụng") {
                SettingsInfoRow(
                    title = "Phiên bản",
                    value = UpdateManager.displayVersion
                )

                SettingsDivider()

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
                    SettingsDivider()
                    Text(
                        text = UpdateManager.lastCheckNote,
                        fontSize = 12.sp,
                        color = Color(0xFF98989F),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }

                SettingsDivider()

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
                        color = Color(0xFFFF453A)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title.uppercase(),
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF8E8E93),
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

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 14.dp)
    )
}

@Composable
private fun SettingsInfoRow(
    title: String,
    value: String
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
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color(0xFF98989F)
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    accent: Color,
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
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accent,
                uncheckedThumbColor = Color(0xFFD0D0D6),
                uncheckedTrackColor = Color.White.copy(alpha = 0.16f),
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
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = selectedLabel,
                fontSize = 13.sp,
                color = Color(0xFF98989F)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF8E8E93),
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Color(0xFF232328)
        ) {
            options.forEach { option ->
                val label = labelFor(option)
                val isSelected = label == selectedLabel
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = if (isSelected) ManiFlixColors.accent() else Color.White,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = ManiFlixColors.accent(),
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
