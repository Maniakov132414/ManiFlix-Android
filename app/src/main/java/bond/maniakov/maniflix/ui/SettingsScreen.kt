package bond.maniakov.maniflix.ui

import android.app.ActivityManager
import android.content.Context
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Palette
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.AppState
import bond.maniakov.maniflix.data.ExternalPlayerOption
import bond.maniakov.maniflix.data.IntroSkipBehavior
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.player.PlayerSessionHolder
import bond.maniakov.maniflix.player.SpatialAudioController
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private enum class SettingsSubPage {
    MAIN,
    THEME,
    DEVICE_MONITOR
}

@Composable
fun SettingsScreen(
    bottomContentPadding: Dp = 24.dp
) {
    var subPage by rememberSaveable { mutableStateOf(SettingsSubPage.MAIN) }

    BackHandler(enabled = subPage != SettingsSubPage.MAIN) {
        subPage = SettingsSubPage.MAIN
    }

    when (subPage) {
        SettingsSubPage.THEME -> {
            ThemeSettingsScreen(
                bottomContentPadding = bottomContentPadding,
                onBack = { subPage = SettingsSubPage.MAIN }
            )
            return
        }

        SettingsSubPage.DEVICE_MONITOR -> {
            DeviceMonitorScreen(
                bottomContentPadding = bottomContentPadding,
                onBack = { subPage = SettingsSubPage.MAIN }
            )
            return
        }

        SettingsSubPage.MAIN -> Unit
    }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()

    val audioOutputLabel = remember { SpatialAudioController.currentAudioOutputLabel(context) }
    val ramSummary = remember {
        runCatching {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am?.getMemoryInfo(mi)
            val totalGb = mi.totalMem / (1024.0 * 1024.0 * 1024.0)
            String.format(Locale.US, "%.1f GB RAM • Live Probe", totalGb)
        }.getOrDefault("Live Telemetry & Codec")
    }

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

        // Drill-down sub-pages: Giao diện & Chủ đề + Kiểm tra mạng & Thiết bị
        item(key = "sec_appearance_and_monitor") {
            SettingsSection(title = "Giao diện & Hệ thống", textSecondary = textSecondary) {
                SettingsDrillDownRow(
                    icon = Icons.Rounded.Palette,
                    iconTint = accent,
                    title = "Giao diện & Chủ đề",
                    subtitle = "${AppPreferences.themeMode.title} • ${AppPreferences.formatColorHex(AppPreferences.customAccentHex)}",
                    trailingDotColor = accent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    borderColor = borderColor,
                    onClick = {
                        triggerHaptic(haptic)
                        subPage = SettingsSubPage.THEME
                    }
                )

                SettingsDivider(borderColor)

                SettingsDrillDownRow(
                    icon = Icons.Rounded.NetworkCheck,
                    iconTint = Color(0xFF10B981),
                    title = "Kiểm tra mạng & Thiết bị",
                    subtitle = ramSummary,
                    trailingDotColor = null,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    borderColor = borderColor,
                    onClick = {
                        triggerHaptic(haptic)
                        subPage = SettingsSubPage.DEVICE_MONITOR
                    }
                )
            }
        }

        // Âm thanh (Zero Spatial Audio!)
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
private fun SettingsDrillDownRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    trailingDotColor: Color?,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconTint.copy(alpha = 0.16f))
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (trailingDotColor != null) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(trailingDotColor)
                    .border(0.8.dp, borderColor, CircleShape)
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
internal fun SettingsMaxWidthContainer(
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
internal fun SettingsSection(
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
internal fun SettingsDivider(borderColor: Color) {
    HorizontalDivider(
        color = borderColor,
        modifier = Modifier.padding(start = 14.dp)
    )
}

@Composable
internal fun SettingsInfoRow(
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
internal fun SettingsSwitchRow(
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
internal fun <T> SettingsDropdownRow(
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
