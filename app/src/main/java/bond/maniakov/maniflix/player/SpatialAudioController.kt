package bond.maniakov.maniflix.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Spatializer
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.SoundProfileMode
import bond.maniakov.maniflix.data.SpatialAudioMode

/**
 * Manages hardware Spatializer diagnostics and ExoPlayer audiofx Equalizer/LoudnessEnhancer.
 */
object SpatialAudioController {

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedSessionId: Int = 0

    fun isHardwareSpatializerAvailable(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S_V2) return false
        return runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val spatializer = am.spatializer
            spatializer.isAvailable &&
                spatializer.isEnabled &&
                spatializer.immersiveAudioLevel != Spatializer.SPATIALIZER_IMMERSIVE_LEVEL_NONE
        }.getOrDefault(false)
    }

    fun isHeadTrackingSupported(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        return runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
            val spatializer = am.spatializer
            spatializer.isAvailable && spatializer.isHeadTrackerAvailable
        }.getOrDefault(false)
    }

    fun isHeadphoneOutput(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return outputs.any { info ->
            when (info.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> true
                else -> (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    info.type == AudioDeviceInfo.TYPE_USB_HEADSET) ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            }
        }
    }

    fun currentAudioOutputLabel(context: Context): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "Loa thiết bị"
        val outputs = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val priority = outputs.firstOrNull { info ->
            info.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                info.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                info.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && info.type == AudioDeviceInfo.TYPE_USB_HEADSET) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && info.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
        } ?: outputs.firstOrNull { info ->
            info.type == AudioDeviceInfo.TYPE_HDMI ||
                info.type == AudioDeviceInfo.TYPE_HDMI_ARC ||
                info.type == AudioDeviceInfo.TYPE_USB_DEVICE ||
                info.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        } ?: outputs.firstOrNull()

        val name = priority?.productName?.toString()?.trim().orEmpty()
        if (name.isNotEmpty()) return name

        return when {
            priority == null -> "Loa thiết bị"
            priority.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Tai nghe Bluetooth"
            priority.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                priority.type == AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Tai nghe có dây"
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                priority.type == AudioDeviceInfo.TYPE_USB_HEADSET -> "Âm thanh USB"
            priority.type == AudioDeviceInfo.TYPE_USB_DEVICE -> "Âm thanh USB"
            priority.type == AudioDeviceInfo.TYPE_HDMI ||
                priority.type == AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI / TV"
            else -> "Loa thiết bị"
        }
    }

    fun activeSpatialSummary(context: Context): String {
        val mode = AppPreferences.spatialAudioMode
        if (mode == SpatialAudioMode.OFF) return "Stereo Gốc"
        if (isHeadTrackingSupported(context) && isHardwareSpatializerAvailable(context)) {
            return "Spatial • Head Tracking"
        }
        if (isHardwareSpatializerAvailable(context)) {
            return "Spatial Audio (${currentAudioOutputLabel(context)})"
        }
        if (isHeadphoneOutput(context)) {
            return "${mode.title} (${currentAudioOutputLabel(context)})"
        }
        return "${mode.title} • ${AppPreferences.soundProfile.title}"
    }

    fun attachToAudioSession(context: Context, audioSessionId: Int) {
        if (audioSessionId <= 0) return

        if (attachedSessionId != audioSessionId) {
            release()
            attachedSessionId = audioSessionId
            runCatching {
                equalizer = Equalizer(0, audioSessionId)
            }
            runCatching {
                loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            }
        }

        applyCurrentEffects(context)
    }

    private fun applyCurrentEffects(context: Context) {
        val spatialMode = AppPreferences.spatialAudioMode
        val profile = AppPreferences.soundProfile
        val isStereoOff = spatialMode == SpatialAudioMode.OFF && profile == SoundProfileMode.STANDARD

        runCatching {
            val eq = equalizer ?: return@runCatching
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()

            eq.enabled = !isStereoOff
            if (!eq.enabled) return@runCatching

            for (bandIdx in 0 until numBands) {
                val band = bandIdx.toShort()
                val centerHz = eq.getCenterFreq(band) / 1000
                var targetMb = when (profile) {
                    SoundProfileMode.CINEMA -> when {
                        centerHz <= 120 -> 280
                        centerHz <= 350 -> 140
                        centerHz in 1000..3500 -> 80
                        centerHz >= 5000 -> 220
                        else -> 0
                    }
                    SoundProfileMode.DIALOGUE -> when {
                        centerHz <= 150 -> -180
                        centerHz in 900..4200 -> 340
                        centerHz >= 6000 -> 120
                        else -> 40
                    }
                    SoundProfileMode.STANDARD -> 0
                }

                if (spatialMode == SpatialAudioMode.CINEMA_WIDE) {
                    targetMb += when {
                        centerHz <= 150 -> 120
                        centerHz >= 4500 -> 180
                        else -> 40
                    }
                } else if (spatialMode == SpatialAudioMode.AUTOMATIC && isHeadphoneOutput(context)) {
                    targetMb += if (centerHz >= 5000) 100 else 0
                }

                val clamped = targetMb.coerceIn(minLevel, maxLevel).toShort()
                eq.setBandLevel(band, clamped)
            }
        }

        runCatching {
            val enhancer = loudnessEnhancer ?: return@runCatching
            val extraGainMb = ((AppPreferences.comfortGain - 1.0f).coerceAtLeast(0f) * 800f).toInt()
            val profileGainMb = when {
                spatialMode == SpatialAudioMode.OFF && profile == SoundProfileMode.STANDARD -> 0
                profile == SoundProfileMode.DIALOGUE -> 180
                spatialMode == SpatialAudioMode.CINEMA_WIDE -> 140
                else -> 60
            }
            val gainMb = profileGainMb + extraGainMb
            enhancer.enabled = gainMb > 0
            if (gainMb > 0) {
                enhancer.setTargetGain(gainMb)
            }
        }
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        loudnessEnhancer = null
        attachedSessionId = 0
    }
}
