package bond.maniakov.maniflix.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bond.maniakov.maniflix.data.UpdateManager
import bond.maniakov.maniflix.ui.theme.ManiFlixColors

@Composable
fun UpdatePromptDialog(
    isForced: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val accent = ManiFlixColors.accent()
    val cfg = UpdateManager.config

    fun openExternalUrl(url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Dialog(
        onDismissRequest = {
            if (!isForced) {
                UpdateManager.snoozeSoftUpdateFor24Hours()
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isForced,
            dismissOnClickOutside = !isForced
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1E),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (isForced) "Cập nhật bắt buộc" else "Bản cập nhật mới",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent
                    )
                    Text(
                        text = cfg?.title?.takeIf { it.isNotBlank() }
                            ?: "ManiFlix ${cfg?.latestVersion.orEmpty()}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = if (isForced) {
                            "Bản cập nhật này là bắt buộc để tiếp tục sử dụng."
                        } else {
                            "Phiên bản hiện tại: ${UpdateManager.displayVersion}"
                        },
                        fontSize = 12.sp,
                        color = Color(0xFF98989F)
                    )
                }

                val notes = cfg?.releaseNotes?.takeIf { it.isNotBlank() }
                if (notes != null) {
                    Text(
                        text = notes,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFD2D2DA),
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(appleGlassSurfaceModifier(cornerRadius = 12.dp))
                            .padding(12.dp)
                    )
                }

                Button(
                    onClick = { openExternalUrl(UpdateManager.recommendedDownloadUrl()) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(
                        text = "Tải bản cập nhật (.apk)",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                val variants = cfg?.variants.orEmpty()
                if (variants.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Các phiên bản APK",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF98989F)
                        )
                        variants.forEach { variant ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(appleGlassSurfaceModifier(cornerRadius = 10.dp))
                                    .clickable { openExternalUrl(variant.downloadUrl) }
                                    .padding(11.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = variant.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${variant.arch} • Android minSdk ${variant.minSdk}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF98989F)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.Download,
                                    contentDescription = "Tải xuống",
                                    tint = Color(0xFFB4B4BC),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (!isForced) {
                    TextButton(
                        onClick = {
                            UpdateManager.snoozeSoftUpdateFor24Hours()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nhắc lại sau 24 giờ",
                            color = Color(0xFF98989F),
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        }
    }
}
