package com.toylek.studio.domain.repository

import android.graphics.Bitmap
import android.net.Uri
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.DeleteResult
import com.toylek.studio.domain.model.ExportResult
import com.toylek.studio.domain.model.MediaItem
import com.toylek.studio.domain.model.MediaType
import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface MediaRepository {
    val items: StateFlow<List<MediaItem>>
    suspend fun refresh()
    suspend fun find(uri: Uri): MediaItem?
    suspend fun savePhoto(bitmap: Bitmap, caption: String): Uri
    suspend fun saveVideo(file: File, caption: String): Uri
    suspend fun delete(items: List<MediaItem>): DeleteResult
    /** Вызывается после того, как пользователь подтвердил удаление в системном окне. */
    suspend fun onDeleteConfirmed(items: List<MediaItem>)
    suspend fun setCaption(uri: Uri, caption: String)
    fun caption(uri: Uri): String
    fun freeSpaceBytes(): Long
    fun newFileName(type: MediaType): String
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun update(transform: (AppSettings) -> AppSettings)
}

interface UsbExportRepository {
    suspend fun export(
        treeUri: Uri,
        items: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ExportResult
}
