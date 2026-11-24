package com.toylek.studio.data.settings

import android.content.Context
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class PrefsSettingsRepository(context: Context) : SettingsRepository {
    private val prefs = context.getSharedPreferences("toylek_settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    override val settings: StateFlow<AppSettings> = _settings

    private fun read(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            eventTitle = prefs.getString(K_TITLE, d.eventTitle) ?: d.eventTitle,
            eventSubtitle = prefs.getString(K_SUBTITLE, d.eventSubtitle) ?: d.eventSubtitle,
            kioskEnabled = prefs.getBoolean(K_KIOSK, d.kioskEnabled),
            autoReturnSeconds = prefs.getInt(K_AUTO_RETURN, d.autoReturnSeconds),
            pin = prefs.getString(K_PIN, d.pin) ?: d.pin,
            defaultFrontCamera = prefs.getBoolean(K_FRONT, d.defaultFrontCamera),
            photoCountdownSeconds = prefs.getInt(K_COUNTDOWN, d.photoCountdownSeconds),
            maxVideoSeconds = prefs.getInt(K_MAX_VIDEO, d.maxVideoSeconds)
                .coerceIn(10, AppSettings.MAX_VIDEO_SECONDS),
            showThanksScreen = prefs.getBoolean(K_THANKS, d.showThanksScreen),
        )
    }

    @Synchronized
    override fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_settings.value).let {
            it.copy(maxVideoSeconds = it.maxVideoSeconds.coerceIn(10, AppSettings.MAX_VIDEO_SECONDS))
        }
        prefs.edit()
            .putString(K_TITLE, s.eventTitle)
            .putString(K_SUBTITLE, s.eventSubtitle)
            .putBoolean(K_KIOSK, s.kioskEnabled)
            .putInt(K_AUTO_RETURN, s.autoReturnSeconds)
            .putString(K_PIN, s.pin)
            .putBoolean(K_FRONT, s.defaultFrontCamera)
            .putInt(K_COUNTDOWN, s.photoCountdownSeconds)
            .putInt(K_MAX_VIDEO, s.maxVideoSeconds)
            .putBoolean(K_THANKS, s.showThanksScreen)
            .apply()
        _settings.value = s
    }

    /** Синхронное чтение для обработчика сбоев (до инициализации UI). */
    fun kioskEnabledNow(): Boolean = prefs.getBoolean(K_KIOSK, false)

    private companion object {
        const val K_TITLE = "event_title"
        const val K_SUBTITLE = "event_subtitle"
        const val K_KIOSK = "kiosk"
        const val K_AUTO_RETURN = "auto_return"
        const val K_PIN = "pin"
        const val K_FRONT = "front_camera"
        const val K_COUNTDOWN = "countdown"
        const val K_MAX_VIDEO = "max_video"
        const val K_THANKS = "thanks"
    }
}
