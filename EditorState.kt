package com.toylek.studio.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.toylek.studio.data.render.FontCatalog
import com.toylek.studio.data.render.TemplateCatalog
import com.toylek.studio.domain.model.BackgroundChoice
import com.toylek.studio.domain.model.Template
import com.toylek.studio.domain.model.TextLayer

/** Состояние редактора оформления. */
class EditorState {
    var template: Template by mutableStateOf(TemplateCatalog.byId("none"))
        private set
    val texts = mutableStateListOf<TextLayer>()
    var selectedId: Long? by mutableStateOf(null)
    var background: BackgroundChoice by mutableStateOf(BackgroundChoice.FromTemplate)
    private var userEditedText = false
    private var nextId = 100L

    val selected: TextLayer? get() = texts.firstOrNull { it.id == selectedId }

    private fun newId(): Long = nextId++

    fun applyTemplate(t: Template) {
        val previous = texts.toList()
        template = t
        background = BackgroundChoice.FromTemplate
        if (!userEditedText || previous.isEmpty()) {
            texts.clear()
            t.defaultTexts.forEach { texts.add(it.copy(id = newId())) }
        } else {
            // Сохраняем текст гостя, но берём стиль из шаблона.
            val style = t.defaultTexts.firstOrNull()
            if (style != null) {
                val first = previous.first()
                val idx = texts.indexOfFirst { it.id == first.id }
                if (idx >= 0) {
                    texts[idx] = first.copy(
                        fontId = style.fontId,
                        color = style.color,
                        size = style.size,
                        x = style.x,
                        y = style.y,
                        shadow = style.shadow,
                        outline = style.outline,
                    )
                }
            }
        }
        selectedId = texts.firstOrNull()?.id
    }

    fun addText(text: String = "Поздравляем!"): TextLayer {
        val layer = TextLayer(
            id = newId(),
            text = text,
            fontId = selected?.fontId ?: FontCatalog.default.id,
            color = selected?.color ?: 0xFFFFFFFF.toInt(),
            size = 0.08f,
            x = 0.5f,
            y = (0.5f + texts.size * 0.12f).coerceAtMost(0.88f),
            shadow = true,
        )
        texts.add(layer)
        selectedId = layer.id
        userEditedText = true
        return layer
    }

    fun update(id: Long, transform: (TextLayer) -> TextLayer) {
        val idx = texts.indexOfFirst { it.id == id }
        if (idx >= 0) texts[idx] = transform(texts[idx])
    }

    fun updateSelected(transform: (TextLayer) -> TextLayer) {
        selectedId?.let { update(it, transform) }
    }

    fun setSelectedText(value: String) {
        userEditedText = true
        updateSelected { it.copy(text = value) }
    }

    fun removeSelected() {
        val id = selectedId ?: return
        texts.removeAll { it.id == id }
        selectedId = texts.lastOrNull()?.id
    }

    /** Если ничего не выбрано — выбрать первую надпись или создать новую. */
    fun ensureSelection() {
        if (selected == null) {
            selectedId = texts.firstOrNull()?.id ?: addText().id
        }
    }
}
