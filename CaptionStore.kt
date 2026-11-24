package com.toylek.studio.data.media

import android.content.Context
import org.json.JSONObject
import java.io.File

/** Подписи к файлам. Хранятся локально в JSON, без интернета. Запись атомарная. */
class CaptionStore(context: Context) {
    private val file = File(context.filesDir, "captions.json")
    private val lock = Any()
    private var cache: MutableMap<String, String>? = null

    private fun load(): MutableMap<String, String> {
        cache?.let { return it }
        val map = HashMap<String, String>()
        if (file.exists()) {
            try {
                val json = JSONObject(file.readText())
                val keys = json.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    map[k] = json.optString(k)
                }
            } catch (_: Exception) {
                // Повреждённый файл — начинаем с пустого списка, а не падаем.
            }
        }
        cache = map
        return map
    }

    fun get(key: String): String = synchronized(lock) { load()[key].orEmpty() }

    fun set(key: String, value: String) = synchronized(lock) {
        val map = load()
        if (value.isBlank()) map.remove(key) else map[key] = value.trim()
        persist(map)
    }

    fun remove(key: String) = synchronized(lock) {
        val map = load()
        if (map.remove(key) != null) persist(map)
    }

    private fun persist(map: Map<String, String>) {
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(JSONObject(map).toString())
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) {
        }
    }
}
