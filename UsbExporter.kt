package com.toylek.studio.data.export

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.toylek.studio.data.media.MediaStoreRepository
import com.toylek.studio.domain.model.ExportResult
import com.toylek.studio.domain.model.MediaItem
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.domain.repository.UsbExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Копирование на USB-флешку (или любую папку) через системный выбор папки (Storage Access Framework).
 * Пользователь выбирает флешку в системном файловом менеджере, приложение создаёт там папку
 * «TOYLEK STUDIO/<дата>» и копирует файлы вместе с файлом подписей.
 */
class UsbExporter(private val context: Context) : UsbExportRepository {

    private val resolver get() = context.contentResolver

    override suspend fun export(
        treeUri: Uri,
        items: List<MediaItem>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): ExportResult = withContext(Dispatchers.IO) {
        val sessionName = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(Date())
        try {
            val rootId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDoc = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
            val appDir = findOrCreateDir(treeUri, rootDoc, rootId, MediaStoreRepository.FOLDER)
            val appDirId = DocumentsContract.getDocumentId(appDir)
            val sessionDir = findOrCreateDir(treeUri, appDir, appDirId, sessionName)
            val sessionId = DocumentsContract.getDocumentId(sessionDir)
            val photosDir = if (items.any { it.type == MediaType.PHOTO }) findOrCreateDir(treeUri, sessionDir, sessionId, "Фото") else null
            val videosDir = if (items.any { it.type == MediaType.VIDEO }) findOrCreateDir(treeUri, sessionDir, sessionId, "Видео") else null

            var copied = 0
            var failed = 0
            val captionsText = StringBuilder()
            items.forEachIndexed { index, item ->
                ensureActive()
                onProgress(index, items.size)
                try {
                    val parent = if (item.type == MediaType.PHOTO) photosDir!! else videosDir!!
                    val mime = if (item.type == MediaType.PHOTO) "image/jpeg" else "video/mp4"
                    val target = DocumentsContract.createDocument(resolver, parent, mime, item.displayName)
                        ?: throw IOException("Не удалось создать файл")
                    resolver.openOutputStream(target)?.use { out ->
                        resolver.openInputStream(item.uri)?.use { input -> input.copyTo(out, 512 * 1024) }
                            ?: throw IOException("Исходный файл недоступен")
                    } ?: throw IOException("Нет доступа к флешке")
                    copied++
                    if (item.caption.isNotBlank()) captionsText.append(item.displayName).append(" — ").append(item.caption).append('\n')
                } catch (e: Exception) {
                    failed++
                }
            }
            if (captionsText.isNotEmpty()) {
                try {
                    DocumentsContract.createDocument(resolver, sessionDir, "text/plain", "Подписи.txt")?.let { doc ->
                        resolver.openOutputStream(doc)?.use { it.write(captionsText.toString().toByteArray(Charsets.UTF_8)) }
                    }
                } catch (_: Exception) {
                }
            }
            onProgress(items.size, items.size)
            ExportResult(copied, failed, "${MediaStoreRepository.FOLDER}/$sessionName")
        } catch (e: Exception) {
            ExportResult(0, items.size, sessionName, e.message ?: "Ошибка доступа к накопителю")
        }
    }

    private fun findOrCreateDir(treeUri: Uri, parentDoc: Uri, parentId: String, name: String): Uri {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null, null, null,
        )?.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val docName = c.getString(1)
                val mime = c.getString(2)
                if (docName == name && mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                }
            }
        }
        return DocumentsContract.createDocument(resolver, parentDoc, DocumentsContract.Document.MIME_TYPE_DIR, name)
            ?: throw IOException("Не удалось создать папку «$name»")
    }
}
