package com.toylek.studio.data.camera

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.toylek.studio.data.media.MediaStoreRepository
import com.toylek.studio.domain.model.MediaType
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

enum class CaptureMode { PHOTO, VIDEO }

sealed interface RecordingEvent {
    data class Progress(val elapsedMs: Long) : RecordingEvent
    data class Finished(val uri: Uri, val limitReached: Boolean) : RecordingEvent
    data class Failed(val message: String) : RecordingEvent
}

/** Обёртка над CameraX: предпросмотр, фото и видео сразу в галерею Android. */
class CameraService(
    private val context: Context,
    private val media: MediaStoreRepository,
) {
    private var provider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    val isRecording: Boolean get() = recording != null

    private suspend fun provider(): ProcessCameraProvider {
        provider?.let { return it }
        val p = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cont.resume(future.get())
                } catch (e: Exception) {
                    cont.resumeWithException(e)
                }
            }, mainExecutor)
        }
        provider = p
        return p
    }

    /** @return пара (есть передняя камера, есть задняя камера). */
    suspend fun availableLenses(): Pair<Boolean, Boolean> {
        val p = provider()
        val front = try { p.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) } catch (e: Exception) { false }
        val back = try { p.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) } catch (e: Exception) { false }
        return front to back
    }

    suspend fun bind(
        owner: LifecycleOwner,
        previewView: PreviewView,
        front: Boolean,
        mode: CaptureMode,
        rotation: Int = Surface.ROTATION_0,
    ) {
        val p = provider()
        p.unbindAll()
        val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        val resolution = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()
        val preview = Preview.Builder()
            .setResolutionSelector(resolution)
            .setTargetRotation(rotation)
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        if (mode == CaptureMode.PHOTO) {
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setResolutionSelector(resolution)
                .setTargetRotation(rotation)
                .build()
            imageCapture = capture
            videoCapture = null
            p.bindToLifecycle(owner, selector, preview, capture)
        } else {
            val recorder = Recorder.Builder()
                .setQualitySelector(
                    QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD))
                )
                .build()
            val capture = VideoCapture.Builder(recorder)
                .setTargetRotation(rotation)
                .build()
            videoCapture = capture
            imageCapture = null
            p.bindToLifecycle(owner, selector, preview, capture)
        }
    }

    fun takePhoto(onPhotoSaved: (Uri) -> Unit, onFailure: (String) -> Unit) {
        val capture = imageCapture
        if (capture == null) {
            onFailure("Камера ещё не готова")
            return
        }
        val values = media.newContentValues(MediaType.PHOTO, media.newFileName(MediaType.PHOTO), pending = false)
        val options = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            media.collectionUri(MediaType.PHOTO),
            values,
        ).build()
        capture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                val uri = outputFileResults.savedUri
                if (uri != null) onPhotoSaved(uri) else onFailure("Фото не сохранилось")
            }

            override fun onError(exception: ImageCaptureException) {
                onFailure("Не удалось сделать фото (${exception.imageCaptureError})")
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun startRecording(maxMs: Long, withAudio: Boolean, onEvent: (RecordingEvent) -> Unit): Boolean {
        val capture = videoCapture ?: return false
        if (recording != null) return false
        val values = media.newContentValues(MediaType.VIDEO, media.newFileName(MediaType.VIDEO), pending = false)
        val options = MediaStoreOutputOptions.Builder(context.contentResolver, media.collectionUri(MediaType.VIDEO))
            .setContentValues(values)
            .setDurationLimitMillis(maxMs)
            .build()
        return try {
            var pending = capture.output.prepareRecording(context, options)
            if (withAudio) pending = pending.withAudioEnabled()
            recording = pending.start(mainExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Status ->
                        onEvent(RecordingEvent.Progress(event.recordingStats.recordedDurationNanos / 1_000_000L))

                    is VideoRecordEvent.Finalize -> {
                        recording = null
                        val uri = event.outputResults.outputUri
                        val acceptable = setOf(
                            VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
                            VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED,
                            VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE,
                            VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE,
                        )
                        val ok = uri != Uri.EMPTY && (!event.hasError() || event.error in acceptable)
                        if (ok) {
                            onEvent(
                                RecordingEvent.Finished(
                                    uri,
                                    event.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED,
                                )
                            )
                        } else {
                            if (uri != Uri.EMPTY) {
                                try {
                                    context.contentResolver.delete(uri, null, null)
                                } catch (_: Exception) {
                                }
                            }
                            onEvent(RecordingEvent.Failed(describeError(event.error)))
                        }
                    }

                    else -> Unit
                }
            }
            true
        } catch (e: Exception) {
            recording = null
            onEvent(RecordingEvent.Failed("Не удалось начать запись: ${e.message}"))
            false
        }
    }

    fun stopRecording() {
        try {
            recording?.stop()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stopRecording()
        try {
            provider?.unbindAll()
        } catch (_: Exception) {
        }
        imageCapture = null
        videoCapture = null
    }

    private fun describeError(code: Int): String = when (code) {
        VideoRecordEvent.Finalize.ERROR_INSUFFICIENT_STORAGE -> "Недостаточно памяти на устройстве"
        VideoRecordEvent.Finalize.ERROR_NO_VALID_DATA -> "Запись слишком короткая"
        VideoRecordEvent.Finalize.ERROR_ENCODING_FAILED -> "Ошибка кодирования видео"
        VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE -> "Камера была отключена во время записи"
        else -> "Ошибка записи видео (код $code)"
    }
}
