package com.toylek.studio.domain.model

data class AppSettings(
    val eventTitle: String = "Добро пожаловать!",
    val eventSubtitle: String = "Оставьте тёплое поздравление",
    val kioskEnabled: Boolean = false,
    /** 0 — автовозврат выключен. */
    val autoReturnSeconds: Int = 90,
    val pin: String = DEFAULT_PIN,
    val defaultFrontCamera: Boolean = true,
    /** 0 — без обратного отсчёта. */
    val photoCountdownSeconds: Int = 3,
    /** Не более 5 минут. */
    val maxVideoSeconds: Int = MAX_VIDEO_SECONDS,
    val showThanksScreen: Boolean = true,
) {
    companion object {
        const val DEFAULT_PIN = "1234"
        const val MAX_VIDEO_SECONDS = 300
    }
}
