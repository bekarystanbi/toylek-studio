package com.toylek.studio.data.render

import android.content.Context
import android.graphics.Typeface
import androidx.annotation.FontRes
import androidx.core.content.res.ResourcesCompat
import com.toylek.studio.R

data class FontSpec(val id: String, val title: String, @FontRes val resId: Int)

/** 14 встроенных шрифтов с поддержкой кириллицы — работают без интернета. */
object FontCatalog {
    val fonts: List<FontSpec> = listOf(
        FontSpec("lora_italic", "Элегантный курсив", R.font.lora_italic),
        FontSpec("poiret", "Ар-деко", R.font.poiret_one),
        FontSpec("serif_bold_italic", "Торжественный", R.font.free_serif_bold_italic),
        FontSpec("lora_bold", "Классика", R.font.lora_bold),
        FontSpec("plex_italic", "Книжный курсив", R.font.plex_serif_italic),
        FontSpec("arsenal_sc", "Капитель", R.font.arsenal_sc),
        FontSpec("montserrat", "Современный", R.font.montserrat),
        FontSpec("fira_black", "Плакатный", R.font.fira_black),
        FontSpec("fira_light", "Воздушный", R.font.fira_light),
        FontSpec("fira_semibold", "Чёткий", R.font.fira_semibold),
        FontSpec("bitter", "Брусковый", R.font.bitter),
        FontSpec("jura", "Футуризм", R.font.jura),
        FontSpec("tektur", "Техно", R.font.tektur),
        FontSpec("comic", "Весёлый", R.font.comic_relief),
    )

    val default: FontSpec get() = fonts.first()

    fun byId(id: String): FontSpec = fonts.firstOrNull { it.id == id } ?: default
}

/** Кэш Typeface для рисования на Canvas. */
class FontProvider(private val context: Context) {
    private val cache = HashMap<String, Typeface>()

    @Synchronized
    fun typeface(fontId: String): Typeface = cache.getOrPut(fontId) {
        val spec = FontCatalog.byId(fontId)
        try {
            ResourcesCompat.getFont(context, spec.resId) ?: Typeface.DEFAULT
        } catch (e: Exception) {
            Typeface.DEFAULT
        }
    }
}
