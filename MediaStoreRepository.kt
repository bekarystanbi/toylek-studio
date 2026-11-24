package com.toylek.studio.data.media

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.toylek.studio.domain.model.DeleteResult
import com.toylek.studio.domain.model.MediaItem
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.domain.repository.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Хранение в общей галерее Android: Pictures/TOYLEK STUDIO и Movies/TOYLEK STUDIO.
 * Файлы видны в «Галерее»/«Google Фото» и остаются после удаления приложения.
 */
class MediaStoreRepository(
    private val context: Context,
    private val captions: CaptionStore,
) : MediaRepository {

    private val resolver = context.contentResolver
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    override val items: StateFlow<List<MediaItem>> = _items

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        val list = try {
            query(MediaType.PHOTO) + query(MediaType.VIDEO)
        } catch (e: Exception) {
            emptyList()
        }
        _items.value = list.sortedByDescending { it.dateAddedMillis }
    }

    override suspend fun find(uri: Uri): MediaItem? {
        _items.value.firstOrNull { it.uri == uri }?.let { return it }
        refresh()
        return _items.value.firstOrNull { it.uri == uri }
    }

    @Suppress("DEPRECATION")
    private fun query(type: MediaType): List<MediaItem> {
        val collection = collectionUri(type)
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.SIZE,
        )
        if (type == MediaType.VIDEO) projection += MediaStore.Video.VideoColumns.DURATION

        val selection: String
        val args: Array<String>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
            args = arrayOf("%$FOLDER%")
        } else {
            selection = "${MediaStore.MediaColumns.DATA} LIKE ?"
            args = arrayOf("%/$FOLDER/%")
        }

        val result = ArrayList<MediaItem>()
        resolver.query(collection, projection.toTypedArray(), selection, args, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
            ?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val durCol = if (type == MediaType.VIDEO) c.getColumnIndex(MediaStore.Video.VideoColumns.DURATION) else -1
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id)
                    val size = c.getLong(sizeCol)
                    if (size <= 0L) continue // незавершённая запись
                    result += MediaItem(
                        id = id,
                        uri = uri,
                        type = type,
                        displayName = c.getString(nameCol) ?: "файл",
                        dateAddedMillis = c.getLong(dateCol) * 1000L,
                        durationMs = if (durCol >= 0) c.getLong(durCol) else 0L,
                        sizeBytes = size,
                        caption = captions.get(uri.toString()),
                    )
                }
            }
        return result
    }

    override suspend fun savePhoto(bitmap: Bitmap, caption: String): Uri = withContext(Dispatchers.IO) {
        val name = newFileName(MediaType.PHOTO)
        val values = newContentValues(MediaType.PHOTO, name, pending = true)
        val uri = resolver.insert(collectionUri(MediaType.PHOTO), values)
            ?: throw IOException("Не удалось создать файл в галерее")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) throw IOException("Ошибка сжатия JPEG")
            } ?: throw IOException("Нет доступа к файлу")
            finishPending(uri)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
        if (caption.isNotBlank()) captions.set(uri.toString(), caption)
        refresh()
        uri
    }

    override suspend fun saveVideo(file: File, caption: String): Uri = withContext(Dispatchers.IO) {
        val name = newFileName(MediaType.VIDEO)
        val values = newContentValues(MediaType.VIDEO, name, pending = true)
        val uri = resolver.insert(collectionUri(MediaType.VIDEO), values)
            ?: throw IOException("Не удалось создать файл в галерее")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out, 256 * 1024) }
            } ?: throw IOException("Нет доступа к файлу")
            finishPending(uri)
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        } finally {
            file.delete()
        }
        if (caption.isNotBlank()) captions.set(uri.toString(), caption)
        refresh()
        uri
    }

    private fun finishPending(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
        }
    }

    override suspend fun delete(items: List<MediaItem>): DeleteResult = withContext(Dispatchers.IO) {
        val needConfirm = ArrayList<MediaItem>()
        var recoverable: RecoverableSecurityException? = null
        var lastError: String? = null
        for (item in items) {
            try {
                resolver.delete(item.uri, null, null)
                captions.remove(item.uri.toString())
            } catch (e: SecurityException) {
                needConfirm += item
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is RecoverableSecurityException) recoverable = e
            } catch (e: Exception) {
                lastError = e.message
            }
        }
        refresh()
        when {
            needConfirm.isEmpty() && lastError == null -> DeleteResult.Deleted
            needConfirm.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> DeleteResult.NeedsConfirmation(
                MediaStore.createDeleteRequest(resolver, needConfirm.map { it.uri }).intentSender
            )
            recoverable != null -> DeleteResult.NeedsConfirmation(recoverable.userAction.actionIntent.intentSender)
            else -> DeleteResult.Failed(lastError ?: "Нет прав на удаление файла")
        }
    }

    override suspend fun onDeleteConfirmed(items: List<MediaItem>) {
        withContext(Dispatchers.IO) {
            items.forEach { captions.remove(it.uri.toString()) }
        }
        refresh()
    }

    override suspend fun setCaption(uri: Uri, caption: String) {
        withContext(Dispatchers.IO) { captions.set(uri.toString(), caption) }
        _items.value = _items.value.map { if (it.uri == uri) it.copy(caption = caption.trim()) else it }
    }

    override fun caption(uri: Uri): String = captions.get(uri.toString())

    override fun freeSpaceBytes(): Long = try {
        StatFs(Environment.getExternalStorageDirectory().path).availableBytes
    } catch (e: Exception) {
        Long.MAX_VALUE
    }

    override fun newFileName(type: MediaType): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return if (type == MediaType.PHOTO) "TOYLEK_$stamp.jpg" else "TOYLEK_$stamp.mp4"
    }

    /** Параметры нового файла. Используются и камерой (CameraX), и редактором. */
    @Suppress("DEPRECATION")
    fun newContentValues(type: MediaType, name: String, pending: Boolean): ContentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, if (type == MediaType.PHOTO) "image/jpeg" else "video/mp4")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir(type))
            if (pending) put(MediaStore.MediaColumns.IS_PENDING, 1)
        } else {
            val base = Environment.getExternalStoragePublicDirectory(
                if (type == MediaType.PHOTO) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES
            )
            val dir = File(base, FOLDER)
            if (!dir.exists()) dir.mkdirs()
            put(MediaStore.MediaColumns.DATA, File(dir, name).absolutePath)
        }
    }

    fun collectionUri(type: MediaType): Uri = if (type == MediaType.PHOTO) {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    }

    private fun relativeDir(type: MediaType): String =
        (if (type == MediaType.PHOTO) Environment.DIRECTORY_PICTURES else Environment.DIRECTORY_MOVIES) + "/" + FOLDER

    companion object {
        const val FOLDER = "TOYLEK STUDIO"
    }
}
