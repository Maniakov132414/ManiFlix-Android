package bond.maniakov.maniflix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AccentPreset(val label: String, val colorHex: Long) {
    CRIMSON("Đỏ", 0xFFFA2D48),
    AMBER("Cam", 0xFFFF6448),
    SKY("Xanh dương", 0xFF0A84FF),
    VIOLET("Tím", 0xFF5E5CE6),
    EMERALD("Xanh lá", 0xFF30D158)
}

enum class SurfaceStyle(val title: String) {
    ADAPTIVE("Tự động"),
    TRANSLUCENT("Trong suốt"),
    MINIMAL("Đơn giản")
}

enum class SpatialAudioMode(val title: String) {
    AUTOMATIC("Tự động"),
    CINEMA_WIDE("Không gian mở rộng"),
    OFF("Tắt (Stereo)")
}

enum class SoundProfileMode(val title: String) {
    CINEMA("Điện ảnh"),
    DIALOGUE("Rõ lời thoại"),
    STANDARD("Mặc định")
}

enum class IntroSkipBehavior(val title: String) {
    BUTTON("Hiện nút bỏ qua"),
    AUTO("Tự động bỏ qua"),
    OFF("Tắt")
}

enum class ExternalPlayerOption(val title: String, val packageName: String?) {
    INTERNAL("Trình phát tích hợp", null),
    VLC("VLC", "org.videolan.vlc"),
    MPV("MPV", "is.xyz.mpv"),
    MX_PLAYER("MX Player", "com.mxtech.videoplayer.ad")
}

object AppPreferences {
    const val BASE_URL = "https://phim.maniakov.bond"
    private const val PREFS_NAME = "maniflix_preferences"

    private const val KEY_ACCENT = "pref.accentPreset"
    private const val KEY_SURFACE = "pref.surfaceStyle"
    private const val KEY_OLED = "pref.oledBlack"
    private const val KEY_AMBIENT = "pref.ambientArtwork"
    private const val KEY_GRID_COLS = "pref.gridColumnsPhone"
    private const val KEY_HAPTICS = "pref.hapticsEnabled"
    private const val KEY_SPATIAL = "pref.spatialAudioMode"
    private const val KEY_SOUND_PROFILE = "pref.soundProfile"
    private const val KEY_COMFORT_GAIN = "pref.comfortGain"
    private const val KEY_AUTO_PIP = "pref.autoPiP"
    private const val KEY_AUTO_ROTATE = "pref.autoRotateLandscape"
    private const val KEY_AUTO_CUT_ADS = "pref.autoCutAds"
    private const val KEY_INTRO_SKIP = "pref.introSkipBehavior"
    private const val KEY_AUTO_NEXT = "pref.autoNextEpisode"
    private const val KEY_DOUBLE_TAP = "pref.doubleTapSeekSeconds"
    private const val KEY_LONG_PRESS_SPEED = "pref.longPressSpeed"
    private const val KEY_EXTERNAL_PLAYER = "pref.externalPlayer"

    private var prefs: SharedPreferences? = null

    private var _accentPreset by mutableStateOf(AccentPreset.CRIMSON)
    var accentPreset: AccentPreset
        get() = _accentPreset
        set(value) {
            _accentPreset = value
            prefs?.edit()?.putString(KEY_ACCENT, value.name)?.apply()
        }

    private var _surfaceStyle by mutableStateOf(SurfaceStyle.ADAPTIVE)
    var surfaceStyle: SurfaceStyle
        get() = _surfaceStyle
        set(value) {
            _surfaceStyle = value
            prefs?.edit()?.putString(KEY_SURFACE, value.name)?.apply()
        }

    private var _oledBlackEnabled by mutableStateOf(false)
    var oledBlackEnabled: Boolean
        get() = _oledBlackEnabled
        set(value) {
            _oledBlackEnabled = value
            prefs?.edit()?.putBoolean(KEY_OLED, value)?.apply()
        }

    private var _ambientArtworkEnabled by mutableStateOf(true)
    var ambientArtworkEnabled: Boolean
        get() = _ambientArtworkEnabled
        set(value) {
            _ambientArtworkEnabled = value
            prefs?.edit()?.putBoolean(KEY_AMBIENT, value)?.apply()
        }

    private var _gridColumnsPhone by mutableIntStateOf(3)
    var gridColumnsPhone: Int
        get() = _gridColumnsPhone
        set(value) {
            val normalized = if (value == 2) 2 else 3
            _gridColumnsPhone = normalized
            prefs?.edit()?.putInt(KEY_GRID_COLS, normalized)?.apply()
        }

    private var _hapticsEnabled by mutableStateOf(true)
    var hapticsEnabled: Boolean
        get() = _hapticsEnabled
        set(value) {
            _hapticsEnabled = value
            prefs?.edit()?.putBoolean(KEY_HAPTICS, value)?.apply()
        }

    private var _spatialAudioMode by mutableStateOf(SpatialAudioMode.AUTOMATIC)
    var spatialAudioMode: SpatialAudioMode
        get() = _spatialAudioMode
        set(value) {
            _spatialAudioMode = value
            prefs?.edit()?.putString(KEY_SPATIAL, value.name)?.apply()
        }

    private var _soundProfile by mutableStateOf(SoundProfileMode.CINEMA)
    var soundProfile: SoundProfileMode
        get() = _soundProfile
        set(value) {
            _soundProfile = value
            prefs?.edit()?.putString(KEY_SOUND_PROFILE, value.name)?.apply()
        }

    private var _comfortGain by mutableFloatStateOf(1f)
    var comfortGain: Float
        get() = _comfortGain
        set(value) {
            val clamped = value.coerceIn(0.5f, 1.5f)
            _comfortGain = clamped
            prefs?.edit()?.putFloat(KEY_COMFORT_GAIN, clamped)?.apply()
        }

    private var _autoPiP by mutableStateOf(true)
    var autoPiP: Boolean
        get() = _autoPiP
        set(value) {
            _autoPiP = value
            prefs?.edit()?.putBoolean(KEY_AUTO_PIP, value)?.apply()
        }

    private var _autoRotateLandscape by mutableStateOf(false)
    var autoRotateLandscape: Boolean
        get() = _autoRotateLandscape
        set(value) {
            _autoRotateLandscape = value
            prefs?.edit()?.putBoolean(KEY_AUTO_ROTATE, value)?.apply()
        }

    private var _autoCutAds by mutableStateOf(true)
    var autoCutAds: Boolean
        get() = _autoCutAds
        set(value) {
            _autoCutAds = value
            prefs?.edit()?.putBoolean(KEY_AUTO_CUT_ADS, value)?.apply()
        }

    private var _introSkipBehavior by mutableStateOf(IntroSkipBehavior.BUTTON)
    var introSkipBehavior: IntroSkipBehavior
        get() = _introSkipBehavior
        set(value) {
            _introSkipBehavior = value
            prefs?.edit()?.putString(KEY_INTRO_SKIP, value.name)?.apply()
        }

    private var _autoNextEpisode by mutableStateOf(true)
    var autoNextEpisode: Boolean
        get() = _autoNextEpisode
        set(value) {
            _autoNextEpisode = value
            prefs?.edit()?.putBoolean(KEY_AUTO_NEXT, value)?.apply()
        }

    private var _doubleTapSeekSeconds by mutableIntStateOf(10)
    var doubleTapSeekSeconds: Int
        get() = _doubleTapSeekSeconds
        set(value) {
            val normalized = if (value in listOf(5, 10, 15, 30)) value else 10
            _doubleTapSeekSeconds = normalized
            prefs?.edit()?.putInt(KEY_DOUBLE_TAP, normalized)?.apply()
        }

    private var _longPressSpeed by mutableFloatStateOf(2.0f)
    var longPressSpeed: Float
        get() = _longPressSpeed
        set(value) {
            val normalized = if (value >= 1.25f) value else 2.0f
            _longPressSpeed = normalized
            prefs?.edit()?.putFloat(KEY_LONG_PRESS_SPEED, normalized)?.apply()
        }

    private var _externalPlayer by mutableStateOf(ExternalPlayerOption.INTERNAL)
    var externalPlayer: ExternalPlayerOption
        get() = _externalPlayer
        set(value) {
            _externalPlayer = value
            prefs?.edit()?.putString(KEY_EXTERNAL_PLAYER, value.name)?.apply()
        }

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        _accentPreset = parseEnum(sp.getString(KEY_ACCENT, null), AccentPreset.CRIMSON)
        _surfaceStyle = parseEnum(sp.getString(KEY_SURFACE, null), SurfaceStyle.ADAPTIVE)
        _oledBlackEnabled = sp.getBoolean(KEY_OLED, false)
        _ambientArtworkEnabled = sp.getBoolean(KEY_AMBIENT, true)
        val cols = sp.getInt(KEY_GRID_COLS, 3)
        _gridColumnsPhone = if (cols == 2 || cols == 3) cols else 3
        _hapticsEnabled = sp.getBoolean(KEY_HAPTICS, true)

        _spatialAudioMode = parseEnum(sp.getString(KEY_SPATIAL, null), SpatialAudioMode.AUTOMATIC)
        _soundProfile = parseEnum(sp.getString(KEY_SOUND_PROFILE, null), SoundProfileMode.CINEMA)
        val gain = sp.getFloat(KEY_COMFORT_GAIN, 1.0f)
        _comfortGain = if (gain > 0.1f) gain.coerceIn(0.5f, 1.5f) else 1.0f

        _autoPiP = sp.getBoolean(KEY_AUTO_PIP, true)
        _autoRotateLandscape = sp.getBoolean(KEY_AUTO_ROTATE, false)
        _autoCutAds = sp.getBoolean(KEY_AUTO_CUT_ADS, true)
        _introSkipBehavior = parseEnum(sp.getString(KEY_INTRO_SKIP, null), IntroSkipBehavior.BUTTON)
        _autoNextEpisode = sp.getBoolean(KEY_AUTO_NEXT, true)
        val seek = sp.getInt(KEY_DOUBLE_TAP, 10)
        _doubleTapSeekSeconds = if (seek in listOf(5, 10, 15, 30)) seek else 10
        val speed = sp.getFloat(KEY_LONG_PRESS_SPEED, 2.0f)
        _longPressSpeed = if (speed >= 1.25f) speed else 2.0f
        _externalPlayer = parseEnum(sp.getString(KEY_EXTERNAL_PLAYER, null), ExternalPlayerOption.INTERNAL)
    }

    fun resetToDefaults() {
        accentPreset = AccentPreset.CRIMSON
        surfaceStyle = SurfaceStyle.ADAPTIVE
        oledBlackEnabled = false
        ambientArtworkEnabled = true
        gridColumnsPhone = 3
        hapticsEnabled = true
        spatialAudioMode = SpatialAudioMode.AUTOMATIC
        soundProfile = SoundProfileMode.CINEMA
        comfortGain = 1f
        autoPiP = true
        autoRotateLandscape = false
        autoCutAds = true
        introSkipBehavior = IntroSkipBehavior.BUTTON
        autoNextEpisode = true
        doubleTapSeekSeconds = 10
        longPressSpeed = 2.0f
        externalPlayer = ExternalPlayerOption.INTERNAL
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String?, fallback: T): T {
        if (raw.isNullOrBlank()) return fallback
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
    }
}
