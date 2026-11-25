package bond.maniakov.maniflix.ui

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build
import android.os.Debug
import android.os.StatFs
import android.view.WindowManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.player.SpatialAudioController
import bond.maniakov.maniflix.ui.theme.ManiFlixColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private data class UserNetSample(
    val downMbps: Double,
    val upMbps: Double,
    val pingMs: Int
)

private data class ServerNetSample(
    val txMbps: Double,
    val rxMbps: Double,
    val cpuPct: Int,
    val memPct: Int
)

private data class ServerNetSnapshot(
    val txMbps: Double = 0.0,
    val rxMbps: Double = 0.0,
    val cpuPct: Int = 0,
    val memPct: Int = 0,
    val reqPerSec: Int = 0,
    val activeRequests: Int = 0,
    val uptimeSec: Long = 0L,
    val history: List<ServerNetSample> = emptyList()
)

private data class CodecSupportItem(
    val name: String,
    val supported: Boolean
)

private data class AndroidHardwareTelemetry(
    val deviceModel: String,
    val osVersion: String,
    val totalRamGb: Double,
    val availRamGb: Double,
    val usedRamGb: Double,
    val appHeapMb: Double,
    val nativeHeapMb: Double,
    val cpuCores: Int,
    val abiSummary: String,
    val freeStorageGb: Double,
    val totalStorageGb: Double,
    val screenSummary: String,
    val refreshHz: Int,
    val audioOutput: String,
    val codecs: List<CodecSupportItem>
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceMonitorScreen(
    bottomContentPadding: Dp = 24.dp,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val accent = ManiFlixColors.accent()
    val bgColor = ManiFlixColors.background()
    val textPrimary = ManiFlixColors.textPrimary()
    val textSecondary = ManiFlixColors.textSecondary()
    val borderColor = ManiFlixColors.cardBorder()
    val isWideScreen = LocalConfiguration.current.screenWidthDp >= 700

    var liveProbeEnabled by remember { mutableStateOf(true) }
    var isManualTesting by remember { mutableStateOf(false) }

    val userSeries = remember { mutableStateListOf<UserNetSample>() }
    val serverSeries = remember { mutableStateListOf<ServerNetSample>() }
    val pingHistory = remember { mutableStateListOf<Int>() }

    var currentDownMbps by remember { mutableDoubleStateOf(0.0) }
    var currentUpMbps by remember { mutableDoubleStateOf(0.0) }
    var currentPingMs by remember { mutableIntStateOf(0) }
    var currentJitterMs by remember { mutableIntStateOf(0) }
    var srvSnapshot by remember { mutableStateOf(ServerNetSnapshot()) }
    var serverOnline by remember { mutableStateOf(true) }

    var hardware by remember { mutableStateOf(readAndroidHardwareTelemetry(context)) }

    suspend fun performTick(downKb: Int, upKb: Int, runSpeedProbe: Boolean) {
        hardware = withContext(Dispatchers.Default) { readAndroidHardwareTelemetry(context) }

        val statsResult = withContext(Dispatchers.IO) { fetchServerStatsTimed() }
        val pingMs = statsResult.first
        val snapshot = statsResult.second

        if (pingMs > 0) {
            currentPingMs = pingMs
            pingHistory.add(pingMs)
            if (pingHistory.size > 20) pingHistory.removeAt(0)
            currentJitterMs = if (pingHistory.size >= 2) {
                var diffSum = 0
                for (i in 1 until pingHistory.size) {
                    diffSum += abs(pingHistory[i] - pingHistory[i - 1])
                }
                diffSum / (pingHistory.size - 1)
            } else {
                0
            }
        }

        if (snapshot != null) {
            serverOnline = true
            srvSnapshot = snapshot
            if (serverSeries.isEmpty() && snapshot.history.isNotEmpty()) {
                serverSeries.addAll(snapshot.history.takeLast(24))
            }
            serverSeries.add(
                ServerNetSample(
                    txMbps = snapshot.txMbps,
                    rxMbps = snapshot.rxMbps,
                    cpuPct = snapshot.cpuPct,
                    memPct = snapshot.memPct
                )
            )
            while (serverSeries.size > 36) serverSeries.removeAt(0)
        } else {
            serverOnline = false
        }

        if (runSpeedProbe) {
            val downMbps = withContext(Dispatchers.IO) { measureDownloadProbeMbps(downKb) }
            val upMbps = withContext(Dispatchers.IO) { measureUploadProbeMbps(upKb) }
            if (downMbps > 0.0) currentDownMbps = downMbps
            if (upMbps > 0.0) currentUpMbps = upMbps
            userSeries.add(
                UserNetSample(
                    downMbps = currentDownMbps,
                    upMbps = currentUpMbps,
                    pingMs = currentPingMs
                )
            )
            while (userSeries.size > 36) userSeries.removeAt(0)
        } else if (userSeries.isEmpty()) {
            userSeries.add(UserNetSample(currentDownMbps, currentUpMbps, currentPingMs))
        }
    }

    LaunchedEffect(liveProbeEnabled) {
        performTick(downKb = 256, upKb = 128, runSpeedProbe = liveProbeEnabled)
        while (isActive) {
            delay(2_200L)
            performTick(downKb = 256, upKb = 128, runSpeedProbe = liveProbeEnabled)
        }
    }

    val peakDown = userSeries.maxOfOrNull { it.downMbps } ?: currentDownMbps
    val peakUp = userSeries.maxOfOrNull { it.upMbps } ?: currentUpMbps
    val effectiveMbps = max(currentDownMbps, peakDown)

    val downColor = Color(0xFF10B981)
    val upColor = Color(0xFF38BDF8)
    val srvOutColor = Color(0xFFF43F5E)
    val srvInColor = Color(0xFFA855F7)

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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header Bar + Controls
        item(key = "monitor_header") {
            SettingsMaxWidthContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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

                        Text(
                            text = "Kiểm tra mạng & Thiết bị",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(
                                    if (serverOnline) downColor.copy(alpha = 0.16f)
                                    else Color(0xFFFF453A).copy(alpha = 0.16f)
                                )
                                .border(
                                    0.7.dp,
                                    if (serverOnline) downColor.copy(alpha = 0.45f)
                                    else Color(0xFFFF453A).copy(alpha = 0.45f),
                                    CircleShape
                                )
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (serverOnline) downColor else Color(0xFFFF453A))
                            )
                            Text(
                                text = if (serverOnline) "LIVE" else "OFF",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (serverOnline) downColor else Color(0xFFFF453A)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (!isManualTesting) {
                                    triggerHaptic(haptic)
                                    scope.launch {
                                        isManualTesting = true
                                        performTick(downKb = 768, upKb = 128, runSpeedProbe = true)
                                        isManualTesting = false
                                    }
                                }
                            },
                            enabled = !isManualTesting,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                        ) {
                            if (isManualTesting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Đang đo...",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Đo Speedtest Ngay",
                                    color = Color.White,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = appleGlassSurfaceModifier(
                                cornerRadius = 10.dp,
                                highlighted = liveProbeEnabled
                            )
                                .clickable {
                                    liveProbeEnabled = !liveProbeEnabled
                                    triggerHaptic(haptic)
                                }
                                .height(42.dp)
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NetworkCheck,
                                contentDescription = null,
                                tint = if (liveProbeEnabled) downColor else textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (liveProbeEnabled) "Live Probe: BẬT" else "Live Probe: TẮT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (liveProbeEnabled) downColor else textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 4 Top KPI Cards
        item(key = "kpi_grid") {
            SettingsMaxWidthContainer {
                val cards: List<@Composable (Modifier) -> Unit> = listOf(
                    { mod ->
                        MonitorKpiCard(
                            modifier = mod,
                            title = "Mạng User Tải Về",
                            value = formatMbps(currentDownMbps),
                            subtitle = if (liveProbeEnabled) "Đỉnh: ${formatMbps(peakDown)}" else "Live Probe đang tắt",
                            valueColor = downColor,
                            icon = Icons.Rounded.ArrowDownward,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    },
                    { mod ->
                        MonitorKpiCard(
                            modifier = mod,
                            title = "Mạng User Tải Lên",
                            value = formatMbps(currentUpMbps),
                            subtitle = "Ping: ${currentPingMs}ms • Jitter: ±${currentJitterMs}ms",
                            valueColor = upColor,
                            icon = Icons.Rounded.ArrowUpward,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    },
                    { mod ->
                        MonitorKpiCard(
                            modifier = mod,
                            title = "Tải I/O Server",
                            value = "↑ ${formatMbps(srvSnapshot.txMbps)}",
                            subtitle = "↓ ${formatMbps(srvSnapshot.rxMbps)} • CPU ${srvSnapshot.cpuPct}%",
                            valueColor = srvOutColor,
                            icon = Icons.Rounded.Dns,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    },
                    { mod ->
                        MonitorKpiCard(
                            modifier = mod,
                            title = "RAM Thiết Bị",
                            value = String.format(Locale.US, "%.1f / %.1f GB", hardware.usedRamGb, hardware.totalRamGb),
                            subtitle = String.format(Locale.US, "App Heap: %.0f MB", hardware.appHeapMb),
                            valueColor = srvInColor,
                            icon = Icons.Rounded.Memory,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary
                        )
                    }
                )

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        cards.forEach { renderCard -> renderCard(Modifier.weight(1f)) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cards[0](Modifier.weight(1f))
                            cards[1](Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            cards[2](Modifier.weight(1f))
                            cards[3](Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 2 Live Stock-Style Canvas Charts
        item(key = "live_charts") {
            SettingsMaxWidthContainer {
                val userChart: @Composable (Modifier) -> Unit = { mod ->
                    LiveStockChartCard(
                        modifier = mod,
                        title = "Mạng User ↔ Web (Live)",
                        legendA = "Down" to downColor,
                        legendB = "Up" to upColor,
                        valuesA = userSeries.map { it.downMbps },
                        valuesB = userSeries.map { it.upMbps },
                        footerChips = listOf(
                            "Ping: ${currentPingMs}ms",
                            "Jitter: ±${currentJitterMs}ms",
                            "Đỉnh Down: ${formatMbps(peakDown)}",
                            "Đỉnh Up: ${formatMbps(peakUp)}"
                        ),
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor
                    )
                }

                val serverChart: @Composable (Modifier) -> Unit = { mod ->
                    LiveStockChartCard(
                        modifier = mod,
                        title = "Lưu Lượng I/O Server",
                        legendA = "Out" to srvOutColor,
                        legendB = "In" to srvInColor,
                        valuesA = serverSeries.map { it.txMbps },
                        valuesB = serverSeries.map { it.rxMbps },
                        footerChips = listOf(
                            "CPU: ${srvSnapshot.cpuPct}%",
                            "RAM: ${srvSnapshot.memPct}%",
                            "HTTP: ${srvSnapshot.reqPerSec} req/s",
                            "Uptime: ${formatUptime(srvSnapshot.uptimeSec)}"
                        ),
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor
                    )
                }

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        userChart(Modifier.weight(1f))
                        serverChart(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        userChart(Modifier.fillMaxWidth())
                        serverChart(Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Capacity Assessment Bar + Tier Badges
        item(key = "capacity_card") {
            SettingsMaxWidthContainer {
                CapacityAssessmentCard(
                    effectiveMbps = effectiveMbps,
                    hardware = hardware,
                    accent = accent,
                    downColor = downColor,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    borderColor = borderColor
                )
            }
        }

        // 2 Hardware / Network Spec Cards
        item(key = "spec_cards") {
            SettingsMaxWidthContainer {
                val hwCard: @Composable (Modifier) -> Unit = { mod ->
                    Column(
                        modifier = mod
                            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Phần cứng & Màn hình",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        SpecKvRow("Thiết bị", hardware.deviceModel, textPrimary, textSecondary)
                        SpecKvRow("Hệ điều hành", hardware.osVersion, textPrimary, textSecondary)
                        SpecKvRow(
                            "RAM trống / tổng",
                            String.format(Locale.US, "%.2f / %.2f GB", hardware.availRamGb, hardware.totalRamGb),
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow(
                            "App Heap / Native",
                            String.format(Locale.US, "%.1f MB / %.1f MB", hardware.appHeapMb, hardware.nativeHeapMb),
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow(
                            "CPU & Kiến trúc",
                            "${hardware.cpuCores} nhân (${hardware.abiSummary})",
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow(
                            "Bộ nhớ trống",
                            String.format(Locale.US, "%.1f / %.1f GB", hardware.freeStorageGb, hardware.totalStorageGb),
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow(
                            "Màn hình & Tần số quét",
                            "${hardware.screenSummary} • ${hardware.refreshHz}Hz",
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow("Đầu ra âm thanh", hardware.audioOutput, textPrimary, textSecondary)
                    }
                }

                val netCodecCard: @Composable (Modifier) -> Unit = { mod ->
                    Column(
                        modifier = mod
                            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Mạng, Máy chủ & Video Codec",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        SpecKvRow("Máy chủ API", AppPreferences.BASE_URL, textPrimary, textSecondary)
                        SpecKvRow("Ping / Jitter", "${currentPingMs} ms / ±${currentJitterMs} ms", textPrimary, textSecondary)
                        SpecKvRow("Tải xuống / Lên", "${formatMbps(currentDownMbps)} / ${formatMbps(currentUpMbps)}", textPrimary, textSecondary)
                        SpecKvRow(
                            "Server I/O",
                            "Out ${formatMbps(srvSnapshot.txMbps)} • In ${formatMbps(srvSnapshot.rxMbps)}",
                            textPrimary,
                            textSecondary
                        )
                        SpecKvRow(
                            "Kết nối HTTP",
                            "${srvSnapshot.reqPerSec} req/s (${srvSnapshot.activeRequests} active)",
                            textPrimary,
                            textSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Giải mã phần cứng (MediaCodec)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            hardware.codecs.forEach { codec ->
                                val badgeColor = if (codec.supported) downColor else textSecondary
                                Text(
                                    text = "${if (codec.supported) "✓" else "×"} ${codec.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = badgeColor,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(7.dp))
                                        .background(badgeColor.copy(alpha = 0.14f))
                                        .border(0.6.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(7.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        hwCard(Modifier.weight(1f))
                        netCodecCard(Modifier.weight(1f))
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        hwCard(Modifier.fillMaxWidth())
                        netCodecCard(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun MonitorKpiCard(
    modifier: Modifier,
    title: String,
    value: String,
    subtitle: String,
    valueColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        modifier = modifier
            .then(appleGlassSurfaceModifier(cornerRadius = 12.dp))
            .padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = valueColor,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 10.5.sp,
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveStockChartCard(
    modifier: Modifier,
    title: String,
    legendA: Pair<String, Color>,
    legendB: Pair<String, Color>,
    valuesA: List<Double>,
    valuesB: List<Double>,
    footerChips: List<String>,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    Column(
        modifier = modifier
            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(legendA, legendB).forEach { (lbl, col) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(col)
                        )
                        Text(
                            text = lbl,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = col
                        )
                    }
                }
            }
        }

        val ptsA = valuesA.takeLast(32)
        val ptsB = valuesB.takeLast(32)
        val maxVal = max(
            0.5,
            max(ptsA.maxOrNull() ?: 0.0, ptsB.maxOrNull() ?: 0.0) * 1.18
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF060B16))
                .border(0.6.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(8.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                for (i in 0..4) {
                    val y = h * i / 4f
                    drawLine(
                        color = Color(0xFF334155).copy(alpha = 0.45f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1f,
                        pathEffect = dashEffect
                    )
                }

                if (ptsA.size >= 2) {
                    fun xAt(idx: Int, count: Int): Float =
                        if (count <= 1) w else (idx.toFloat() / (count - 1)) * w

                    fun yAt(v: Double): Float =
                        h - ((v.coerceAtLeast(0.0) / maxVal).toFloat().coerceIn(0f, 1f) * h)

                    if (ptsB.size >= 2) {
                        val pathB = Path()
                        ptsB.forEachIndexed { idx, v ->
                            val x = xAt(idx, ptsB.size)
                            val y = yAt(v)
                            if (idx == 0) pathB.moveTo(x, y) else pathB.lineTo(x, y)
                        }
                        drawPath(
                            path = pathB,
                            color = legendB.second,
                            style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }

                    val linePathA = Path()
                    val fillPathA = Path()
                    ptsA.forEachIndexed { idx, v ->
                        val x = xAt(idx, ptsA.size)
                        val y = yAt(v)
                        if (idx == 0) {
                            linePathA.moveTo(x, y)
                            fillPathA.moveTo(x, h)
                            fillPathA.lineTo(x, y)
                        } else {
                            linePathA.lineTo(x, y)
                            fillPathA.lineTo(x, y)
                        }
                    }
                    fillPathA.lineTo(w, h)
                    fillPathA.close()

                    drawPath(
                        path = fillPathA,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                legendA.second.copy(alpha = 0.30f),
                                Color.Transparent
                            )
                        )
                    )
                    drawPath(
                        path = linePathA,
                        color = legendA.second,
                        style = Stroke(width = 4.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    val lastY = yAt(ptsA.last())
                    drawCircle(
                        color = legendA.second,
                        radius = 6f,
                        center = Offset(w - 3f, lastY)
                    )
                }
            }

            if (ptsA.size < 2) {
                Text(
                    text = "Đang thu thập dữ liệu live...",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Text(
                    text = formatMbps(maxVal),
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                Text(
                    text = formatMbps(ptsA.last()),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = legendA.second,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xCC060B16))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            footerChips.forEach { chip ->
                Text(
                    text = chip,
                    fontSize = 10.5.sp,
                    color = textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(7.dp))
                        .background(ManiFlixColors.surfaceElevated())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CapacityAssessmentCard(
    effectiveMbps: Double,
    hardware: AndroidHardwareTelemetry,
    accent: Color,
    downColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color
) {
    val hasModernCodec = hardware.codecs.any {
        it.supported && (it.name.contains("HEVC") || it.name.contains("AV1") || it.name.contains("VP9"))
    }
    val can4k = effectiveMbps >= 18.0 && (hardware.totalRamGb >= 3.5 || hasModernCodec)
    val can1080p60 = effectiveMbps >= 6.0 || effectiveMbps == 0.0
    val can720p = effectiveMbps >= 2.5 || effectiveMbps == 0.0

    val headline = when {
        can4k -> "2K / 4K HDR (Siêu Mượt)"
        effectiveMbps >= 6.0 -> "1080p60 FHD (Mượt Mà)"
        effectiveMbps >= 2.5 -> "720p HD (Ổn Định)"
        effectiveMbps > 0.0 -> "480p SD (Tiết kiệm)"
        else -> "Đang phân tích băng thông..."
    }

    val scoreRatio = when {
        can4k -> 1.0f
        effectiveMbps >= 6.0 -> 0.72f
        effectiveMbps >= 2.5 -> 0.45f
        effectiveMbps > 0.0 -> 0.24f
        else -> 0.35f
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(appleGlassSurfaceModifier(cornerRadius = 14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = downColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "Sức Tải Tối Đa Của Máy & Mạng",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = headline,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = downColor,
                maxLines = 1
            )
        }

        LinearProgressIndicator(
            progress = { scoreRatio },
            color = downColor,
            trackColor = borderColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
        )

        Text(
            text = if (effectiveMbps > 0.0) {
                "Đo thực tế ${formatMbps(effectiveMbps)} • RAM ${String.format(Locale.US, "%.1f GB", hardware.totalRamGb)} • Media3 HLS Adaptive Buffer."
            } else {
                "Đang đo mẫu tốc độ mạng tới ${AppPreferences.BASE_URL}..."
            },
            fontSize = 12.sp,
            color = textSecondary,
            lineHeight = 17.sp
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(
                "720p" to can720p,
                "1080p60" to can1080p60,
                "2K/4K HDR" to can4k
            ).forEach { (tier, ok) ->
                val tierColor = if (ok) downColor else accent
                Text(
                    text = "${if (ok) "✓" else "•"} $tier",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = tierColor,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tierColor.copy(alpha = 0.14f))
                        .border(0.6.dp, tierColor.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun SpecKvRow(
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.9f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatMbps(mbps: Double): String {
    return if (mbps >= 100.0) {
        String.format(Locale.US, "%.0f Mbps", mbps)
    } else {
        String.format(Locale.US, "%.2f Mbps", mbps)
    }
}

private fun formatUptime(uptimeSec: Long): String {
    if (uptimeSec <= 0L) return "--"
    val hours = uptimeSec / 3600L
    val mins = (uptimeSec % 3600L) / 60L
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

private fun readAndroidHardwareTelemetry(context: Context): AndroidHardwareTelemetry {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(memInfo)

    val totalRamGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
    val availRamGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
    val usedRamGb = max(0.0, totalRamGb - availRamGb)

    val rt = Runtime.getRuntime()
    val appHeapMb = (rt.totalMemory() - rt.freeMemory()) / (1024.0 * 1024.0)
    val nativeHeapMb = Debug.getNativeHeapAllocatedSize() / (1024.0 * 1024.0)

    val statFs = runCatching { StatFs(context.filesDir.absolutePath) }.getOrNull()
    val freeStorageGb = (statFs?.availableBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
    val totalStorageGb = (statFs?.totalBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)

    val dm = context.resources.displayMetrics
    val screenSummary = "${dm.widthPixels}×${dm.heightPixels} (${String.format(Locale.US, "%.1fx", dm.density)})"

    val refreshHz = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display?.refreshRate?.roundToInt() ?: 60
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            wm?.defaultDisplay?.refreshRate?.roundToInt() ?: 60
        }
    }.getOrDefault(60)

    val codecs = detectAndroidCodecs()
    val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
    }
    val deviceModel = "$manufacturer ${Build.MODEL} (${Build.DEVICE})".trim()

    return AndroidHardwareTelemetry(
        deviceModel = deviceModel,
        osVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
        totalRamGb = totalRamGb,
        availRamGb = availRamGb,
        usedRamGb = usedRamGb,
        appHeapMb = appHeapMb,
        nativeHeapMb = nativeHeapMb,
        cpuCores = rt.availableProcessors(),
        abiSummary = Build.SUPPORTED_ABIS.take(2).joinToString(", "),
        freeStorageGb = freeStorageGb,
        totalStorageGb = totalStorageGb,
        screenSummary = screenSummary,
        refreshHz = refreshHz,
        audioOutput = SpatialAudioController.currentAudioOutputLabel(context),
        codecs = codecs
    )
}

private fun detectAndroidCodecs(): List<CodecSupportItem> {
    val mimeSet = runCatching {
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
            .filter { !it.isEncoder }
            .flatMap { it.supportedTypes.toList() }
            .map { it.lowercase(Locale.US) }
            .toSet()
    }.getOrDefault(emptySet())

    return listOf(
        CodecSupportItem("HLS Media3", true),
        CodecSupportItem("H.264 / AVC", "video/avc" in mimeSet),
        CodecSupportItem("HEVC / H.265", "video/hevc" in mimeSet),
        CodecSupportItem("VP9", "video/x-vnd.on2.vp9" in mimeSet),
        CodecSupportItem("AV1", "video/av01" in mimeSet)
    )
}

private fun fetchServerStatsTimed(): Pair<Int, ServerNetSnapshot?> {
    val startNs = System.nanoTime()
    var conn: HttpURLConnection? = null
    return try {
        val url = URL("${AppPreferences.BASE_URL}/api/net-stats?_t=${System.currentTimeMillis()}")
        conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "ManiFlixAndroid/1.0")
            setRequestProperty("Accept", "application/json")
        }
        val code = conn.responseCode
        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).toInt().coerceAtLeast(1)
        if (code !in 200..299) {
            return elapsedMs to null
        }
        val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(body)
        val histArr = json.optJSONArray("history")
        val histList = buildList {
            if (histArr != null) {
                for (i in 0 until histArr.length()) {
                    val pt = histArr.optJSONObject(i) ?: continue
                    add(
                        ServerNetSample(
                            txMbps = pt.optDouble("txMbps", 0.0),
                            rxMbps = pt.optDouble("rxMbps", 0.0),
                            cpuPct = pt.optInt("cpuPct", json.optInt("cpuPct", 0)),
                            memPct = pt.optInt("memPct", json.optInt("memPct", 0))
                        )
                    )
                }
            }
        }
        elapsedMs to ServerNetSnapshot(
            txMbps = json.optDouble("txMbps", 0.0),
            rxMbps = json.optDouble("rxMbps", 0.0),
            cpuPct = json.optInt("cpuPct", 0),
            memPct = json.optInt("memPct", 0),
            reqPerSec = json.optInt("reqPerSec", 0),
            activeRequests = json.optInt("activeRequests", 0),
            uptimeSec = json.optLong("uptimeSec", 0L),
            history = histList
        )
    } catch (_: Throwable) {
        0 to null
    } finally {
        conn?.disconnect()
    }
}

private fun measureDownloadProbeMbps(kb: Int): Double {
    val startNs = System.nanoTime()
    var conn: HttpURLConnection? = null
    return try {
        val url = URL("${AppPreferences.BASE_URL}/api/net-probe?kb=$kb&_t=${System.currentTimeMillis()}")
        conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "ManiFlixAndroid/1.0")
        }
        if (conn.responseCode !in 200..299) return 0.0
        val bytes = ByteArrayOutputStream().use { out ->
            conn.inputStream.copyTo(out)
            out.size()
        }
        val dtSec = max(0.004, (System.nanoTime() - startNs) / 1_000_000_000.0)
        (bytes * 8.0) / dtSec / 1_000_000.0
    } catch (_: Throwable) {
        0.0
    } finally {
        conn?.disconnect()
    }
}

private fun measureUploadProbeMbps(kb: Int): Double {
    val payload = ByteArray(kb * 1024) { idx -> ((idx * 29) and 0xFF).toByte() }
    val startNs = System.nanoTime()
    var conn: HttpURLConnection? = null
    return try {
        val url = URL("${AppPreferences.BASE_URL}/api/net-up?_t=${System.currentTimeMillis()}")
        conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 8_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "ManiFlixAndroid/1.0")
            setRequestProperty("Content-Type", "application/octet-stream")
            setFixedLengthStreamingMode(payload.size)
        }
        conn.outputStream.use { it.write(payload) }
        if (conn.responseCode !in 200..299) return 0.0
        conn.inputStream.bufferedReader().use { it.readText() }
        val dtSec = max(0.004, (System.nanoTime() - startNs) / 1_000_000_000.0)
        (payload.size * 8.0) / dtSec / 1_000_000.0
    } catch (_: Throwable) {
        0.0
    } finally {
        conn?.disconnect()
    }
}
