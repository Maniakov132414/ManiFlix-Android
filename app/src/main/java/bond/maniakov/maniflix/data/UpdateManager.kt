package bond.maniakov.maniflix.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import bond.maniakov.maniflix.model.AndroidVersionConfig

object UpdateManager {
    const val currentVersion = "1.0.0"
    const val currentBuild = "1"
    const val displayVersion = "$currentVersion ($currentBuild)"

    private const val PREFS_NAME = "maniflix_update"
    private const val KEY_SNOOZE_UNTIL = "update.softSnoozeUntil"
    private const val DEFAULT_ARM64_URL =
        "https://github.com/Maniakov132414/ManiFlix-Android/releases/latest/download/ManiFlix-arm64.apk"

    private var prefs: SharedPreferences? = null

    var config by mutableStateOf<AndroidVersionConfig?>(null)
    var showSoftPrompt by mutableStateOf(false)
    var showForceBlocker by mutableStateOf(false)
    var isChecking by mutableStateOf(false)
    var lastCheckNote by mutableStateOf("")

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun checkForUpdates(manual: Boolean = false) {
        isChecking = true
        try {
            val remote = ManiFlixApi.fetchAndroidVersion()
            config = remote

            val minRequired = remote.minRequiredVersion.ifBlank { "0.0.0" }
            val mustForceByMin = isVersionLowerThan(currentVersion, minRequired)
            val isForced = remote.forceUpdate || remote.mode == "force" || mustForceByMin
            val hasNewer = isVersionLowerThan(currentVersion, remote.latestVersion)

            if (isForced && (hasNewer || mustForceByMin || remote.forceUpdate)) {
                showSoftPrompt = false
                showForceBlocker = true
                lastCheckNote = "Yêu cầu cập nhật bắt buộc lên v${remote.latestVersion}"
                return
            }

            if (hasNewer) {
                if (manual) {
                    showSoftPrompt = true
                } else {
                    val snoozedUntil = prefs?.getLong(KEY_SNOOZE_UNTIL, 0L) ?: 0L
                    if (System.currentTimeMillis() >= snoozedUntil) {
                        showSoftPrompt = true
                    }
                }
                lastCheckNote = "Đã có phiên bản mới v${remote.latestVersion}"
            } else {
                lastCheckNote = "Bạn đang dùng bản mới nhất (v$currentVersion)"
            }
        } catch (e: Exception) {
            if (manual) {
                lastCheckNote = "Không thể kiểm tra cập nhật: ${e.message ?: "Lỗi mạng"}"
            }
        } finally {
            isChecking = false
        }
    }

    fun snoozeSoftUpdateFor24Hours() {
        val until = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        prefs?.edit()?.putLong(KEY_SNOOZE_UNTIL, until)?.apply()
        showSoftPrompt = false
    }

    fun recommendedDownloadUrl(): String {
        val variants = config?.variants.orEmpty()
        if (variants.isNotEmpty()) {
            val supportedAbis = Build.SUPPORTED_ABIS?.toList().orEmpty()
            val targetArch = when {
                supportedAbis.any { it.equals("arm64-v8a", ignoreCase = true) } -> "arm64-v8a"
                supportedAbis.any { it.equals("armeabi-v7a", ignoreCase = true) } -> "armeabi-v7a"
                else -> "universal"
            }

            val exact = variants.firstOrNull {
                it.arch.equals(targetArch, ignoreCase = true) || it.key.equals(targetArch, ignoreCase = true)
            }
            if (exact != null && exact.downloadUrl.isNotBlank()) {
                return exact.downloadUrl
            }

            val universal = variants.firstOrNull {
                it.arch.equals("universal", ignoreCase = true) || it.key.equals("universal", ignoreCase = true)
            }
            if (universal != null && universal.downloadUrl.isNotBlank()) {
                return universal.downloadUrl
            }
        }
        return config?.downloadUrl?.takeIf { it.isNotBlank() } ?: DEFAULT_ARM64_URL
    }

    private fun isVersionLowerThan(lhs: String, rhs: String): Boolean {
        val leftParts = lhs.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val rightParts = rhs.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
        val count = maxOf(leftParts.size, rightParts.size)
        for (i in 0 until count) {
            val lv = leftParts.getOrElse(i) { 0 }
            val rv = rightParts.getOrElse(i) { 0 }
            if (lv != rv) return lv < rv
        }
        return false
    }
}
