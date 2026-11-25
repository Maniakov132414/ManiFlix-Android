package bond.maniakov.maniflix.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.os.Build
import bond.maniakov.maniflix.data.AppPreferences
import bond.maniakov.maniflix.data.SoundProfileMode

/**
 * Manages audio output routing diagnostics and ExoPlayer audiofx Equalizer/LoudnessEnhancer.
 */
object SpatialAudioController {

    private var equalizer: Equalizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var attachedSessionId: Int = 0

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

    fun activeAudioSummary(context: Context): String {
        val gainPct = (AppPreferences.comfortGain * 100).toInt()
        return "${AppPreferences.soundProfile.title} • $gainPct% (${currentAudioOutputLabel(context)})"
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

        applyCurrentEffects()
    }

    private fun applyCurrentEffects() {
        val profile = AppPreferences.soundProfile
        val comfortGain = AppPreferences.comfortGain

        runCatching {
            val eq = equalizer ?: return@runCatching
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range[0].toInt()
            val maxLevel = range[1].toInt()

            eq.enabled = profile != SoundProfileMode.STANDARD
            if (!eq.enabled) return@runCatching

            for (bandIdx in 0 until numBands) {
                val band = bandIdx.toShort()
                val centerHz = eq.getCenterFreq(band) / 1000
                val targetMb = when (profile) {
                    SoundProfileMode.STANDARD -> 0
                    SoundProfileMode.DIALOGUE -> when {
                        centerHz <= 150 -> -180
                        centerHz in 900..4200 -> 340
                        centerHz >= 6000 -> 120
                        else -> 40
                    }
                    SoundProfileMode.BASS_BOOST -> when {
                        centerHz <= 120 -> 380
                        centerHz <= 320 -> 220
                        centerHz in 1000..3000 -> 40
                        centerHz >= 5000 -> 140
                        else -> 0
                    }
                }
                val clamped = targetMb.coerceIn(minLevel, maxLevel).toShort()
                eq.setBandLevel(band, clamped)
            }
        }

        runCatching {
            val enhancer = loudnessEnhancer ?: return@runCatching
            val extraGainMb = ((comfortGain - 1.0f).coerceAtLeast(0f) * 800f).toInt()
            val profileGainMb = when (profile) {
                SoundProfileMode.STANDARD -> 0
                SoundProfileMode.DIALOGUE -> 160
                SoundProfileMode.BASS_BOOST -> 100
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
