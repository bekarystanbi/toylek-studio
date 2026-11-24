package com.toylek.studio.di

import android.content.Context
import com.toylek.studio.data.export.UsbExporter
import com.toylek.studio.data.media.CaptionStore
import com.toylek.studio.data.media.MediaStoreRepository
import com.toylek.studio.data.render.CompositionRenderer
import com.toylek.studio.data.render.FontProvider
import com.toylek.studio.data.settings.PrefsSettingsRepository
import com.toylek.studio.data.video.VideoOverlayExporter

/** Ручное внедрение зависимостей (без кодогенерации — быстрее сборка, проще поддержка). */
class AppContainer(context: Context) {
    val app: Context = context.applicationContext

    val captionStore = CaptionStore(app)
    val mediaRepository = MediaStoreRepository(app, captionStore)
    val settingsRepository = PrefsSettingsRepository(app)
    val usbExporter = UsbExporter(app)
    val fontProvider = FontProvider(app)
    val renderer = CompositionRenderer(fontProvider)
    val videoExporter = VideoOverlayExporter(app)
}
