package com.toylek.studio.data.video

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.media3.common.MediaItem as Media3Item

/**
 * Накладывает оформление (рамку, фон, надписи) на видео прямо на устройстве с помощью Media3 Transformer.
 * Звук сохраняется. Интернет не нужен.
 */
class VideoOverlayExporter(private val context: Context) {

    /**
     * @param overlay прозрачный слой размером с кадр видео.
     * @return временный MP4-файл в кэше приложения.
     */
    @OptIn(UnstableApi::class)
    suspend fun export(input: Uri, overlay: Bitmap, onProgress: (Int) -> Unit): File = withContext(Dispatchers.Main) {
        val output = File(context.cacheDir, "export_${System.currentTimeMillis()}.mp4")
        coroutineScope {
            var transformerRef: Transformer? = null
            val progressJob = launch {
                val holder = ProgressHolder()
                while (isActive) {
                    delay(250)
                    val t = transformerRef ?: continue
                    if (t.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                }
            }
            try {
                suspendCancellableCoroutine<File> { cont ->
                    val bitmapOverlay = BitmapOverlay.createStaticBitmapOverlay(overlay)
                    val overlayEffect = OverlayEffect(ImmutableList.of<TextureOverlay>(bitmapOverlay))
                    val edited = EditedMediaItem.Builder(Media3Item.fromUri(input))
                        .setEffects(Effects(ImmutableList.of(), ImmutableList.of<Effect>(overlayEffect)))
                        .build()
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                if (cont.isActive) cont.resume(output)
                            }

                            override fun onError(
                                composition: Composition,
                                exportResult: ExportResult,
                                exportException: ExportException,
                            ) {
                                output.delete()
                                if (cont.isActive) cont.resumeWithException(exportException)
                            }
                        })
                        .build()
                    transformerRef = transformer
                    cont.invokeOnCancellation {
                        try {
                            transformer.cancel()
                        } catch (_: Exception) {
                        }
                        output.delete()
                    }
                    transformer.start(edited, output.absolutePath)
                }
            } finally {
                progressJob.cancel()
            }
        }
    }
}
