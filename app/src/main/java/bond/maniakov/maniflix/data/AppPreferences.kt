package bond.maniakov.maniflix.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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

enum class ThemeMode(val title: String) {
    SYSTEM("Tự động"),
    LIGHT("Sáng"),
    DARK("Tối"),
    OLED("Đen OLED"),
    CUSTOM("Tuỳ chỉnh")
}

data class ThemePresetItem(
    val id: String,
    val name: String,
    val mode: ThemeMode,
    val accentHex: Long,
    val bgHex: Long,
    val surfaceHex: Long
)

val THEME_PRESETS: List<ThemePresetItem> = listOf(
    ThemePresetItem(
        id = "crimson_violet",
        name = "ManiFlix Mặc Định",
        mode = ThemeMode.DARK,
        accentHex = 0xFFE11D48,
        bgHex = 0xFF0B0F19,
        surfaceHex = 0xFF0F172A
    ),
    ThemePresetItem(
        id = "netflix_oled",
        name = "Rạp Đen OLED",
        mode = ThemeMode.OLED,
        accentHex = 0xFFE50914,
        bgHex = 0xFF050507,
        surfaceHex = 0xFF0D0D12
    ),
    ThemePresetItem(
        id = "midnight_indigo",
        name = "Đêm Xanh Indigo",
        mode = ThemeMode.CUSTOM,
        accentHex = 0xFF5E5CE6,
        bgHex = 0xFF0B1020,
        surfaceHex = 0xFF151C32
    ),
    ThemePresetItem(
        id = "emerald_aurora",
        name = "Lục Bảo Cực Quang",
        mode = ThemeMode.CUSTOM,
        accentHex = 0xFF10B981,
        bgHex = 0xFF06120F,
        surfaceHex = 0xFF0B1D19
    ),
    ThemePresetItem(
        id = "cyber_rose",
        name = "Cyberpunk Rose",
        mode = ThemeMode.CUSTOM,
        accentHex = 0xFFFF2D55,
        bgHex = 0xFF140818,
        surfaceHex = 0xFF221029
    ),
    ThemePresetItem(
        id = "pure_light",
        name = "Trắng Tinh Khôi",
        mode = ThemeMode.LIGHT,
        accentHex = 0xFFE11D48,
        bgHex = 0xFFF4F5F8,
        surfaceHex = 0xFFFFFFFF
    )
)

enum class SoundProfileMode(val title: String) {
    STANDARD("Mặc định"),
    DIALOGUE("Rõ lời thoại"),
    BASS_BOOST("Tăng âm trầm")
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
    private const val KEY_THEME_MODE = "pref.themeMode"
    private const val KEY_CUSTOM_ACCENT = "pref.customAccentHex"
    private const val KEY_CUSTOM_BG = "pref.customBgHex"
    private const val KEY_CUSTOM_SURFACE = "pref.customSurfaceHex"
    private const val KEY_ACTIVE_PRESET = "pref.activePresetId"
    private const val KEY_AMBIENT = "pref.ambientArtwork"
    private const val KEY_GRID_COLS = "pref.gridColumnsPhone"
    private const val KEY_HAPTICS = "pref.hapticsEnabled"
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
    private const val KEY_UI_LANGUAGE = "pref.uiLanguage"
    private const val KEY_DEFAULT_SUBTITLE = "pref.defaultSubtitleLang"
    private const val KEY_LANG_SETUP_DONE = "pref.hasCompletedLanguageSetup"

    private var prefs: SharedPreferences? = null

    private var _accentPreset by mutableStateOf(AccentPreset.CRIMSON)
    var accentPreset: AccentPreset
        get() = _accentPreset
        set(value) {
            _accentPreset = value
            _customAccentHex = value.colorHex
            prefs?.edit()
                ?.putString(KEY_ACCENT, value.name)
                ?.putLong(KEY_CUSTOM_ACCENT, value.colorHex)
                ?.apply()
        }

    private var _surfaceStyle by mutableStateOf(SurfaceStyle.ADAPTIVE)
    var surfaceStyle: SurfaceStyle
        get() = _surfaceStyle
        set(value) {
            _surfaceStyle = value
            prefs?.edit()?.putString(KEY_SURFACE, value.name)?.apply()
        }

    private var _themeMode by mutableStateOf(ThemeMode.DARK)
    var themeMode: ThemeMode
        get() = _themeMode
        set(value) {
            _themeMode = value
            prefs?.edit()
                ?.putString(KEY_THEME_MODE, value.name)
                ?.putBoolean(KEY_OLED, value == ThemeMode.OLED)
                ?.apply()
        }

    private var _customAccentHex by mutableLongStateOf(0xFFFA2D48)
    var customAccentHex: Long
        get() = _customAccentHex
        set(value) {
            val opaque = value or 0xFF000000L
            _customAccentHex = opaque
            prefs?.edit()?.putLong(KEY_CUSTOM_ACCENT, opaque)?.apply()
        }

    private var _customBgHex by mutableLongStateOf(0xFF121214)
    var customBgHex: Long
        get() = _customBgHex
        set(value) {
            val opaque = value or 0xFF000000L
            _customBgHex = opaque
            prefs?.edit()?.putLong(KEY_CUSTOM_BG, opaque)?.apply()
        }

    private var _customSurfaceHex by mutableLongStateOf(0xFF1C1C1E)
    var customSurfaceHex: Long
        get() = _customSurfaceHex
        set(value) {
            val opaque = value or 0xFF000000L
            _customSurfaceHex = opaque
            prefs?.edit()?.putLong(KEY_CUSTOM_SURFACE, opaque)?.apply()
        }

    private var _activePresetId by mutableStateOf("apple-dark")
    var activePresetId: String
        get() = _activePresetId
        set(value) {
            _activePresetId = value
            prefs?.edit()?.putString(KEY_ACTIVE_PRESET, value)?.apply()
        }

    val oledBlackEnabled: Boolean
        get() = themeMode == ThemeMode.OLED

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

    private var _soundProfile by mutableStateOf(SoundProfileMode.STANDARD)
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

    private var _uiLanguage by mutableStateOf("vi")
    var uiLanguage: String
        get() = _uiLanguage
        set(value) {
            val norm = if (value == "en") "en" else "vi"
            _uiLanguage = norm
            prefs?.edit()?.putString(KEY_UI_LANGUAGE, norm)?.apply()
        }

    private var _defaultSubtitleLang by mutableStateOf("vi")
    var defaultSubtitleLang: String
        get() = _defaultSubtitleLang
        set(value) {
            val norm = if (value == "en" || value == "off") value else "vi"
            _defaultSubtitleLang = norm
            prefs?.edit()?.putString(KEY_DEFAULT_SUBTITLE, norm)?.apply()
        }

    private var _hasCompletedLanguageSetup by mutableStateOf(true)
    var hasCompletedLanguageSetup: Boolean
        get() = _hasCompletedLanguageSetup
        set(value) {
            _hasCompletedLanguageSetup = value
            prefs?.edit()?.putBoolean(KEY_LANG_SETUP_DONE, value)?.apply()
        }

    fun tr(vi: String, en: String): String = if (uiLanguage == "en") en else vi

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        _accentPreset = parseEnum(sp.getString(KEY_ACCENT, null), AccentPreset.CRIMSON)
        _surfaceStyle = parseEnum(sp.getString(KEY_SURFACE, null), SurfaceStyle.ADAPTIVE)
        val savedThemeRaw = sp.getString(KEY_THEME_MODE, null)
        _themeMode = if (savedThemeRaw != null) {
            parseEnum(savedThemeRaw, ThemeMode.DARK)
        } else if (sp.getBoolean(KEY_OLED, false)) {
            ThemeMode.OLED
        } else {
            ThemeMode.DARK
        }
        _customAccentHex = sp.getLong(KEY_CUSTOM_ACCENT, _accentPreset.colorHex) or 0xFF000000L
        _customBgHex = sp.getLong(KEY_CUSTOM_BG, 0xFF121214) or 0xFF000000L
        _customSurfaceHex = sp.getLong(KEY_CUSTOM_SURFACE, 0xFF1C1C1E) or 0xFF000000L
        _activePresetId = sp.getString(KEY_ACTIVE_PRESET, "apple-dark").orEmpty().ifBlank { "apple-dark" }
        _ambientArtworkEnabled = sp.getBoolean(KEY_AMBIENT, true)
        val cols = sp.getInt(KEY_GRID_COLS, 3)
        _gridColumnsPhone = if (cols == 2 || cols == 3) cols else 3
        _hapticsEnabled = sp.getBoolean(KEY_HAPTICS, true)

        _soundProfile = parseEnum(sp.getString(KEY_SOUND_PROFILE, null), SoundProfileMode.STANDARD)
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
        val savedLang = sp.getString(KEY_UI_LANGUAGE, "vi")
        _uiLanguage = if (savedLang == "en") "en" else "vi"
        val savedSub = sp.getString(KEY_DEFAULT_SUBTITLE, "vi")
        _defaultSubtitleLang = if (savedSub == "en" || savedSub == "off") savedSub else "vi"
        _hasCompletedLanguageSetup = sp.getBoolean(KEY_LANG_SETUP_DONE, false)
    }

    fun applyPreset(preset: ThemePresetItem) {
        themeMode = preset.mode
        customAccentHex = preset.accentHex
        customBgHex = preset.bgHex
        customSurfaceHex = preset.surfaceHex
        activePresetId = preset.id
        AccentPreset.entries.firstOrNull { it.colorHex == (preset.accentHex or 0xFF000000L) }?.let { match ->
            _accentPreset = match
            prefs?.edit()?.putString(KEY_ACCENT, match.name)?.apply()
        }
    }

    fun formatColorHex(argb: Long): String {
        val rgb = (argb and 0x00FFFFFFL).toInt()
        return String.format("#%06X", rgb)
    }

    fun parseColorHex(hex: String?, fallback: Long): Long {
        val cleaned = hex?.trim()?.removePrefix("#").orEmpty()
        if (cleaned.length != 6) return fallback
        val parsed = cleaned.toLongOrNull(16) ?: return fallback
        return 0xFF000000L or parsed
    }

    fun resetToDefaults() {
        accentPreset = AccentPreset.CRIMSON
        surfaceStyle = SurfaceStyle.ADAPTIVE
        themeMode = ThemeMode.DARK
        customAccentHex = 0xFFFA2D48
        customBgHex = 0xFF121214
        customSurfaceHex = 0xFF1C1C1E
        activePresetId = "apple-dark"
        ambientArtworkEnabled = true
        gridColumnsPhone = 3
        hapticsEnabled = true
        soundProfile = SoundProfileMode.STANDARD
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
