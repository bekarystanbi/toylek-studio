package com.toylek.studio.domain.model

/**
 * Текстовый слой. Координаты и размер — относительные (0..1), поэтому одно и то же
 * оформление одинаково выглядит в предпросмотре и в итоговом файле любого разрешения.
 */
data class TextLayer(
    val id: Long,
    val text: String,
    val fontId: String,
    val color: Int,
    /** Высота шрифта как доля высоты кадра. */
    val size: Float,
    /** Центр текста по X (0..1). */
    val x: Float,
    /** Центр текста по Y (0..1). */
    val y: Float,
    val shadow: Boolean = true,
    val outline: Boolean = false,
)

/** Отступы области с фото/видео от краёв кадра (доли ширины/высоты). */
data class Inset(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val isZero: Boolean get() = left == 0f && top == 0f && right == 0f && bottom == 0f

    companion object {
        val ZERO = Inset(0f, 0f, 0f, 0f)
        fun all(v: Float) = Inset(v, v, v, v)
    }
}

enum class Decoration { NONE, HEARTS, CONFETTI, STARS, CORNERS, ORNAMENT, FILM, BALLOONS }

data class Ribbon(
    val color: Int,
    /** Центр ленты по Y (0..1). */
    val centerY: Float,
    /** Высота ленты (доля высоты кадра). */
    val height: Float,
)

data class Template(
    val id: String,
    val title: String,
    /** Цвета градиента фона. Пустой список — фона нет. */
    val background: List<Int>,
    val inset: Inset,
    /** Радиус скругления области с фото (доля меньшей стороны). */
    val cornerRadius: Float,
    val frameColor: Int?,
    /** Толщина рамки (доля меньшей стороны). */
    val frameWidth: Float,
    val doubleFrame: Boolean,
    val decoration: Decoration,
    val decorationColor: Int,
    val ribbon: Ribbon?,
    val defaultTexts: List<TextLayer>,
)

/** Что использовать как фон за фото/видео. */
sealed interface BackgroundChoice {
    data object FromTemplate : BackgroundChoice
    data class Gradient(val colors: List<Int>) : BackgroundChoice
    data class Image(val uri: android.net.Uri) : BackgroundChoice
}
