package com.toylek.studio.domain.model

import android.net.Uri

enum class MediaType { PHOTO, VIDEO }

/** Фото или видео, сохранённое приложением в галерею Android. */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val type: MediaType,
    val displayName: String,
    val dateAddedMillis: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val caption: String,
)

/** Только что снятый материал (ещё до оформления). */
data class CapturedMedia(
    val uri: Uri,
    val type: MediaType,
)

sealed interface DeleteResult {
    data object Deleted : DeleteResult
    /** Системе нужно подтверждение пользователя (файлы созданы до переустановки приложения). */
    data class NeedsConfirmation(val intentSender: android.content.IntentSender) : DeleteResult
    data class Failed(val message: String) : DeleteResult
}

data class ExportResult(
    val copied: Int,
    val failed: Int,
    val folderName: String,
    val errorMessage: String? = null,
)
