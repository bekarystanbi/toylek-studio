package com.toylek.studio.ui

import android.content.IntentSender
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.toylek.studio.data.camera.CaptureMode
import com.toylek.studio.data.render.RenderSpec
import com.toylek.studio.data.render.TemplateCatalog
import com.toylek.studio.di.AppContainer
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.BackgroundChoice
import com.toylek.studio.domain.model.CapturedMedia
import com.toylek.studio.domain.model.DeleteResult
import com.toylek.studio.domain.model.MediaItem
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.domain.model.TextLayer
import com.toylek.studio.util.BitmapLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.max

sealed interface Screen {
    data object Home : Screen
    data class Camera(val mode: CaptureMode) : Screen
    data class Review(val media: CapturedMedia) : Screen
    data class Editor(val uri: Uri, val type: MediaType, val fromGallery: Boolean) : Screen
    data class Gallery(val tab: MediaType, val pickForEdit: Boolean = false) : Screen
    data class Viewer(val uri: Uri, val type: MediaType) : Screen
    data object Settings : Screen
    data object Thanks : Screen
}

data class ProgressState(val title: String, val percent: Int?)

data class PendingDelete(val intentSender: IntentSender, val items: List<MediaItem>)

/** Что сохранить из редактора. Растровые изображения загружаются заново в полном разрешении. */
data class EditorExport(
    val sourceUri: Uri,
    val type: MediaType,
    val templateId: String,
    val texts: List<TextLayer>,
    val background: BackgroundChoice,
    val fromGallery: Boolean,
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.mediaRepository
    private val ctx = container.app

    val settings: StateFlow<AppSettings> = container.settingsRepository.settings
    val media: StateFlow<List<MediaItem>> = repo.items

    private val _stack = MutableStateFlow<List<Screen>>(listOf(Screen.Home))
    val stack: StateFlow<List<Screen>> = _stack

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    private val _progress = MutableStateFlow<ProgressState?>(null)
    val progress: StateFlow<ProgressState?> = _progress

    private val _pendingDelete = MutableStateFlow<PendingDelete?>(null)
    val pendingDelete: StateFlow<PendingDelete?> = _pendingDelete

    private val busyKeys = HashSet<String>()
    @Volatile
    private var lastInteraction = SystemClock.elapsedRealtime()

    init {
        refreshMedia()
        startAutoReturnLoop()
    }

    // ---------- Навигация ----------

    fun navigate(screen: Screen) {
        _stack.value = _stack.value + screen
        touch()
    }

    fun replace(screen: Screen) {
        _stack.value = _stack.value.dropLast(1) + screen
        touch()
    }

    fun back() {
        if (_stack.value.size > 1) _stack.value = _stack.value.dropLast(1)
        touch()
    }

    fun goHome() {
        _stack.value = listOf(Screen.Home)
        busyKeys.clear()
    }

    /** Завершение работы гостя: экран «Спасибо» (если включён) и возврат на главный. */
    fun finishSession() {
        _stack.value = if (settings.value.showThanksScreen) listOf(Screen.Home, Screen.Thanks) else listOf(Screen.Home)
        busyKeys.clear()
    }

    fun touch() {
        lastInteraction = SystemClock.elapsedRealtime()
    }

    /** Пока есть «занятость» (идёт запись, ввод текста), автовозврат не срабатывает. */
    fun setBusy(key: String, busy: Boolean) {
        if (busy) busyKeys += key else busyKeys -= key
        touch()
    }

    private fun startAutoReturnLoop() {
        viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val seconds = settings.value.autoReturnSeconds
                val current = _stack.value.last()
                if (seconds <= 0) continue
                if (current is Screen.Home || current is Screen.Thanks) continue
                if (busyKeys.isNotEmpty() || _progress.value != null) continue
                if (SystemClock.elapsedRealtime() - lastInteraction > seconds * 1000L) {
                    goHome()
                }
            }
        }
    }

    fun message(text: String) {
        _messages.tryEmit(text)
    }

    // ---------- Настройки ----------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = container.settingsRepository.update(transform)

    // ---------- Медиа ----------

    fun refreshMedia() {
        viewModelScope.launch { repo.refresh() }
    }

    fun freeSpaceBytes(): Long = repo.freeSpaceBytes()

    fun onCaptured(media: CapturedMedia) {
        viewModelScope.launch { repo.refresh() }
        replace(Screen.Review(media))
    }

    /** «Переснять»: удаляем только что снятый файл и возвращаемся к камере. */
    fun retake(media: CapturedMedia) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ctx.contentResolver.delete(media.uri, null, null)
                } catch (_: Exception) {
                }
            }
            repo.refresh()
        }
        replace(Screen.Camera(if (media.type == MediaType.PHOTO) CaptureMode.PHOTO else CaptureMode.VIDEO))
    }

    fun setCaption(uri: Uri, caption: String) {
        viewModelScope.launch {
            repo.setCaption(uri, caption)
            message(if (caption.isBlank()) "Подпись удалена" else "Подпись сохранена")
        }
    }

    fun captionOf(uri: Uri): String = repo.caption(uri)

    fun delete(items: List<MediaItem>, onDone: () -> Unit = {}) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            when (val result = repo.delete(items)) {
                DeleteResult.Deleted -> {
                    message(if (items.size == 1) "Файл удалён" else "Удалено файлов: ${items.size}")
                    onDone()
                }
                is DeleteResult.NeedsConfirmation -> _pendingDelete.value = PendingDelete(result.intentSender, items)
                is DeleteResult.Failed -> message("Не удалось удалить: ${result.message}")
            }
        }
    }

    fun onDeleteConfirmationResult(confirmed: Boolean) {
        val pending = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch {
            if (confirmed) {
                repo.onDeleteConfirmed(pending.items)
                message("Файлы удалены")
            } else {
                repo.refresh()
            }
        }
    }

    // ---------- USB ----------

    fun exportToUsb(treeUri: Uri, items: List<MediaItem>) {
        if (items.isEmpty()) {
            message("Нет файлов для экспорта")
            return
        }
        viewModelScope.launch {
            _progress.value = ProgressState("Копирование на флешку…", 0)
            val result = try {
                container.usbExporter.export(treeUri, items) { done, total ->
                    _progress.value = ProgressState("Копирование на флешку: $done из $total", if (total == 0) 100 else done * 100 / total)
                }
            } finally {
                _progress.value = null
            }
            when {
                result.errorMessage != null -> message("Ошибка экспорта: ${result.errorMessage}")
                result.failed == 0 -> message("Скопировано файлов: ${result.copied}. Папка «${result.folderName}»")
                else -> message("Скопировано: ${result.copied}, с ошибкой: ${result.failed}")
            }
        }
    }

    // ---------- Сохранение из редактора ----------

    fun saveEdited(export: EditorExport) {
        viewModelScope.launch {
            try {
                val caption = repo.caption(export.sourceUri)
                if (export.type == MediaType.PHOTO) savePhoto(export, caption) else saveVideo(export, caption)
                if (export.fromGallery) {
                    message("Сохранено в галерею")
                    back()
                } else {
                    finishSession()
                }
            } catch (e: OutOfMemoryError) {
                message("Недостаточно памяти для обработки. Попробуйте ещё раз.")
            } catch (e: Exception) {
                message("Не удалось сохранить: ${e.message ?: "неизвестная ошибка"}")
            } finally {
                _progress.value = null
            }
        }
    }

    private suspend fun loadBackground(choice: BackgroundChoice, maxDim: Int) =
        if (choice is BackgroundChoice.Image) BitmapLoader.loadImage(ctx, choice.uri, maxDim) else null

    private suspend fun savePhoto(export: EditorExport, caption: String) {
        _progress.value = ProgressState("Сохраняем фото…", null)
        val photo = BitmapLoader.loadImage(ctx, export.sourceUri, MAX_EXPORT_DIM)
            ?: throw IOException("не удалось открыть фото")
        val bg = loadBackground(export.background, MAX_EXPORT_DIM)
        val spec = RenderSpec(
            template = TemplateCatalog.byId(export.templateId),
            texts = export.texts,
            media = photo,
            mediaFillsCanvas = false,
            backgroundImage = bg,
            backgroundColors = (export.background as? BackgroundChoice.Gradient)?.colors,
        )
        val result = withContext(Dispatchers.Default) {
            container.renderer.renderToBitmap(photo.width, photo.height, spec)
        }
        photo.recycle()
        bg?.recycle()
        try {
            repo.savePhoto(result, caption)
        } finally {
            result.recycle()
        }
    }

    private suspend fun saveVideo(export: EditorExport, caption: String) {
        _progress.value = ProgressState("Подготовка видео…", 0)
        val frame = BitmapLoader.loadVideoFrame(ctx, export.sourceUri, 8192)
        val size = if (frame != null) frame.width to frame.height else BitmapLoader.videoDisplaySize(ctx, export.sourceUri)
        frame?.recycle()
        if (size == null) throw IOException("не удалось прочитать видео")
        val (w, h) = size
        val bg = loadBackground(export.background, max(w, h))
        val spec = RenderSpec(
            template = TemplateCatalog.byId(export.templateId),
            texts = export.texts,
            media = null,
            mediaFillsCanvas = true,
            backgroundImage = bg,
            backgroundColors = (export.background as? BackgroundChoice.Gradient)?.colors,
        )
        val overlay = withContext(Dispatchers.Default) { container.renderer.renderToBitmap(w, h, spec) }
        bg?.recycle()
        if (repo.freeSpaceBytes() < 300L * 1024 * 1024) throw IOException("мало свободной памяти на устройстве")
        _progress.value = ProgressState("Обработка видео…", 0)
        val file = try {
            container.videoExporter.export(export.sourceUri, overlay) { p ->
                _progress.value = ProgressState("Обработка видео…", p)
            }
        } finally {
            overlay.recycle()
        }
        _progress.value = ProgressState("Сохраняем видео…", null)
        repo.saveVideo(file, caption)
    }

    companion object {
        private const val MAX_EXPORT_DIM = 4096

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(container) }
        }
    }
}
