package com.nightstand.clock.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.nightstand.clock.math.AlarmConfig
import com.nightstand.clock.math.HourFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 錶面種類，順序就是左右滑動切換的順序。 */
enum class FaceType(val label: String) {
    DIGITAL("數位"),
    ANALOG("指針"),
    FLIP("翻頁"),
}

/** 所有使用者設定。App 和螢幕保護程式共用同一份。 */
data class ClockSettings(
    val face: FaceType = FaceType.DIGITAL,
    /** 每個錶面各自的縮放倍率，索引對應 [FaceType.ordinal]。 */
    val faceScales: List<Float> = List(FaceType.entries.size) { 1f },
    val nightMode: Boolean = false,
    val hourFormat: HourFormat = HourFormat.SYSTEM,
    val showSeconds: Boolean = false,
    val showQuotes: Boolean = true,
    /** 白天亮度 0..1；小於 0 代表跟隨系統。 */
    val dayBrightness: Float = BRIGHTNESS_FOLLOW_SYSTEM,
    /** 夜間亮度 0..1。 */
    val nightBrightness: Float = DEFAULT_NIGHT_BRIGHTNESS,
    val alarm: AlarmConfig = AlarmConfig(),
) {
    val faceScale: Float get() = faceScales[face.ordinal]

    companion object {
        const val BRIGHTNESS_FOLLOW_SYSTEM = -1f
        const val DEFAULT_NIGHT_BRIGHTNESS = 0.05f
    }
}

/**
 * 用 SharedPreferences 存設定，並以 [StateFlow] 提供給畫面。
 * 整個 App 只有一個實例（Activity 和 DreamService 在同一個行程），所以一邊改另一邊立刻看得到。
 */
class SettingsStore private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ClockSettings> = _settings.asStateFlow()

    val current: ClockSettings get() = _settings.value

    @Synchronized
    fun update(transform: (ClockSettings) -> ClockSettings) {
        val next = transform(_settings.value)
        if (next == _settings.value) return
        _settings.value = next
        save(next)
    }

    private fun load(): ClockSettings {
        val defaults = ClockSettings()
        return ClockSettings(
            face = enumOrDefault(prefs.getString(KEY_FACE, null), defaults.face),
            faceScales = FaceType.entries.map { prefs.getFloat(scaleKey(it), 1f) },
            nightMode = prefs.getBoolean(KEY_NIGHT, defaults.nightMode),
            hourFormat = enumOrDefault(prefs.getString(KEY_HOUR_FORMAT, null), defaults.hourFormat),
            showSeconds = prefs.getBoolean(KEY_SECONDS, defaults.showSeconds),
            showQuotes = prefs.getBoolean(KEY_QUOTES, defaults.showQuotes),
            dayBrightness = prefs.getFloat(KEY_DAY_BRIGHTNESS, defaults.dayBrightness),
            nightBrightness = prefs.getFloat(KEY_NIGHT_BRIGHTNESS, defaults.nightBrightness),
            alarm = AlarmConfig(
                enabled = prefs.getBoolean(KEY_ALARM_ENABLED, defaults.alarm.enabled),
                hour = prefs.getInt(KEY_ALARM_HOUR, defaults.alarm.hour),
                minute = prefs.getInt(KEY_ALARM_MINUTE, defaults.alarm.minute),
                repeatMask = prefs.getInt(KEY_ALARM_REPEAT, defaults.alarm.repeatMask),
                snoozeUntilMillis = prefs.getLong(KEY_ALARM_SNOOZE, defaults.alarm.snoozeUntilMillis),
            ),
        )
    }

    private fun save(s: ClockSettings) {
        prefs.edit {
            putString(KEY_FACE, s.face.name)
            FaceType.entries.forEach { putFloat(scaleKey(it), s.faceScales[it.ordinal]) }
            putBoolean(KEY_NIGHT, s.nightMode)
            putString(KEY_HOUR_FORMAT, s.hourFormat.name)
            putBoolean(KEY_SECONDS, s.showSeconds)
            putBoolean(KEY_QUOTES, s.showQuotes)
            putFloat(KEY_DAY_BRIGHTNESS, s.dayBrightness)
            putFloat(KEY_NIGHT_BRIGHTNESS, s.nightBrightness)
            putBoolean(KEY_ALARM_ENABLED, s.alarm.enabled)
            putInt(KEY_ALARM_HOUR, s.alarm.hour)
            putInt(KEY_ALARM_MINUTE, s.alarm.minute)
            putInt(KEY_ALARM_REPEAT, s.alarm.repeatMask)
            putLong(KEY_ALARM_SNOOZE, s.alarm.snoozeUntilMillis)
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private fun scaleKey(face: FaceType) = "scale_${face.name.lowercase()}"

    companion object {
        private const val PREFS_NAME = "clock_settings"
        private const val KEY_FACE = "face"
        private const val KEY_NIGHT = "night_mode"
        private const val KEY_HOUR_FORMAT = "hour_format"
        private const val KEY_SECONDS = "show_seconds"
        private const val KEY_QUOTES = "show_quotes"
        private const val KEY_DAY_BRIGHTNESS = "day_brightness"
        private const val KEY_NIGHT_BRIGHTNESS = "night_brightness"
        private const val KEY_ALARM_ENABLED = "alarm_enabled"
        private const val KEY_ALARM_HOUR = "alarm_hour"
        private const val KEY_ALARM_MINUTE = "alarm_minute"
        private const val KEY_ALARM_REPEAT = "alarm_repeat"
        private const val KEY_ALARM_SNOOZE = "alarm_snooze_until"

        @Volatile
        private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) {
                instance ?: SettingsStore(context).also { instance = it }
            }
    }
}
