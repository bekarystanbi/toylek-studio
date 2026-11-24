package com.toylek.studio.data.render

import com.toylek.studio.domain.model.Decoration
import com.toylek.studio.domain.model.Inset
import com.toylek.studio.domain.model.Ribbon
import com.toylek.studio.domain.model.Template
import com.toylek.studio.domain.model.TextLayer

private fun c(hex: Long): Int = hex.toInt()

private const val GOLD = 0xFFD4AF6AL
private const val IVORY = 0xFFFFF8EBL
private const val WHITE = 0xFFFFFFFFL

private fun text(
    id: Long,
    text: String,
    font: String,
    color: Long,
    size: Float,
    y: Float,
    x: Float = 0.5f,
    shadow: Boolean = true,
    outline: Boolean = false,
) = TextLayer(id, text, font, c(color), size, x, y, shadow, outline)

object TemplateCatalog {

    val templates: List<Template> = listOf(
        Template(
            id = "none",
            title = "Без оформления",
            background = emptyList(),
            inset = Inset.ZERO,
            cornerRadius = 0f,
            frameColor = null,
            frameWidth = 0f,
            doubleFrame = false,
            decoration = Decoration.NONE,
            decorationColor = c(WHITE),
            ribbon = null,
            defaultTexts = emptyList(),
        ),
        Template(
            id = "wedding_gold",
            title = "Свадьба · Золото",
            background = listOf(c(0xFFFFF8EB), c(0xFFF1E0C0)),
            inset = Inset(0.07f, 0.07f, 0.07f, 0.20f),
            cornerRadius = 0.02f,
            frameColor = c(GOLD),
            frameWidth = 0.006f,
            doubleFrame = true,
            decoration = Decoration.CORNERS,
            decorationColor = c(0xFFB8924A),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "С днём свадьбы!", "lora_italic", 0xFF8A6A2E, 0.085f, 0.9f, shadow = false),
            ),
        ),
        Template(
            id = "tender",
            title = "Нежность",
            background = listOf(c(0xFFFDE2E4), c(0xFFF7C5CC)),
            inset = Inset(0.06f, 0.06f, 0.06f, 0.19f),
            cornerRadius = 0.04f,
            frameColor = c(WHITE),
            frameWidth = 0.012f,
            doubleFrame = false,
            decoration = Decoration.HEARTS,
            decorationColor = c(0xFFE56B88),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "Любви и счастья!", "poiret", 0xFFB23A5A, 0.085f, 0.905f, shadow = false),
            ),
        ),
        Template(
            id = "birthday",
            title = "День рождения",
            background = listOf(c(0xFF6A3DE8), c(0xFFE84393), c(0xFFFFA62B)),
            inset = Inset(0.06f, 0.18f, 0.06f, 0.06f),
            cornerRadius = 0.03f,
            frameColor = c(WHITE),
            frameWidth = 0.008f,
            doubleFrame = false,
            decoration = Decoration.CONFETTI,
            decorationColor = c(WHITE),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "С днём рождения!", "fira_black", WHITE, 0.1f, 0.095f),
            ),
        ),
        Template(
            id = "balloons",
            title = "Праздник",
            background = listOf(c(0xFF4FC3F7), c(0xFF81D4FA), c(0xFFE1F5FE)),
            inset = Inset(0.16f, 0.07f, 0.16f, 0.07f),
            cornerRadius = 0.05f,
            frameColor = c(WHITE),
            frameWidth = 0.01f,
            doubleFrame = false,
            decoration = Decoration.BALLOONS,
            decorationColor = c(0xFFFF5C8A),
            ribbon = Ribbon(c(0xCCFFFFFF), 0.88f, 0.13f),
            defaultTexts = listOf(
                text(1, "Поздравляем!", "comic", 0xFF1E5AA8, 0.085f, 0.885f, shadow = false),
            ),
        ),
        Template(
            id = "jubilee",
            title = "Юбилей",
            background = listOf(c(0xFF4A0E1C), c(0xFF7B1E33)),
            inset = Inset(0.08f, 0.08f, 0.08f, 0.21f),
            cornerRadius = 0.01f,
            frameColor = c(GOLD),
            frameWidth = 0.007f,
            doubleFrame = true,
            decoration = Decoration.STARS,
            decorationColor = c(GOLD),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "С юбилеем!", "serif_bold_italic", GOLD, 0.095f, 0.895f),
            ),
        ),
        Template(
            id = "classic",
            title = "Классика",
            background = listOf(c(0xFF111111), c(0xFF2A2A2A)),
            inset = Inset(0.05f, 0.05f, 0.05f, 0.17f),
            cornerRadius = 0f,
            frameColor = c(WHITE),
            frameWidth = 0.003f,
            doubleFrame = false,
            decoration = Decoration.NONE,
            decorationColor = c(WHITE),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "ПОЗДРАВЛЯЕМ", "arsenal_sc", IVORY, 0.075f, 0.915f, shadow = false),
            ),
        ),
        Template(
            id = "emerald",
            title = "Изумруд",
            background = listOf(c(0xFF0B3D2E), c(0xFF145A43)),
            inset = Inset.all(0.075f),
            cornerRadius = 0.02f,
            frameColor = c(GOLD),
            frameWidth = 0.006f,
            doubleFrame = true,
            decoration = Decoration.ORNAMENT,
            decorationColor = c(GOLD),
            ribbon = Ribbon(c(0xB30B3D2E), 0.86f, 0.14f),
            defaultTexts = listOf(
                text(1, "Счастья молодым!", "lora_italic", GOLD, 0.08f, 0.86f),
            ),
        ),
        Template(
            id = "night_sky",
            title = "Ночное небо",
            background = listOf(c(0xFF0B1033), c(0xFF27306B)),
            inset = Inset(0.07f, 0.17f, 0.07f, 0.07f),
            cornerRadius = 0.03f,
            frameColor = c(0xFFBFD4FF),
            frameWidth = 0.004f,
            doubleFrame = false,
            decoration = Decoration.STARS,
            decorationColor = c(0xFFFFF2B3),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "Мечтайте смело!", "plex_italic", 0xFFFFF2B3, 0.085f, 0.09f),
            ),
        ),
        Template(
            id = "film",
            title = "Кинолента",
            background = listOf(c(0xFF050505)),
            inset = Inset(0f, 0.13f, 0f, 0.13f),
            cornerRadius = 0f,
            frameColor = null,
            frameWidth = 0f,
            doubleFrame = false,
            decoration = Decoration.FILM,
            decorationColor = c(0xFFEDEDED),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "Наша история", "jura", WHITE, 0.07f, 0.935f, shadow = false),
            ),
        ),
        Template(
            id = "polaroid",
            title = "Полароид",
            background = listOf(c(0xFFFAFAF7)),
            inset = Inset(0.05f, 0.06f, 0.05f, 0.22f),
            cornerRadius = 0f,
            frameColor = null,
            frameWidth = 0f,
            doubleFrame = false,
            decoration = Decoration.NONE,
            decorationColor = c(WHITE),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "Лучший день!", "comic", 0xFF333333, 0.09f, 0.89f, shadow = false),
            ),
        ),
        Template(
            id = "ribbon_gold",
            title = "Золотая лента",
            background = emptyList(),
            inset = Inset.ZERO,
            cornerRadius = 0f,
            frameColor = c(GOLD),
            frameWidth = 0.006f,
            doubleFrame = true,
            decoration = Decoration.CORNERS,
            decorationColor = c(GOLD),
            ribbon = Ribbon(c(0x99000000), 0.86f, 0.15f),
            defaultTexts = listOf(
                text(1, "Поздравляем!", "lora_italic", GOLD, 0.09f, 0.86f),
            ),
        ),
        Template(
            id = "neon",
            title = "Вечеринка",
            background = emptyList(),
            inset = Inset.ZERO,
            cornerRadius = 0f,
            frameColor = c(0xFFFF4FD8),
            frameWidth = 0.008f,
            doubleFrame = false,
            decoration = Decoration.CONFETTI,
            decorationColor = c(0xFF00E5FF),
            ribbon = null,
            defaultTexts = listOf(
                text(1, "Отрываемся!", "tektur", 0xFF00E5FF, 0.1f, 0.13f, outline = true),
            ),
        ),
    )

    fun byId(id: String): Template = templates.firstOrNull { it.id == id } ?: templates.first()

    /** Готовые градиенты для вкладки «Фон». */
    val backgroundPresets: List<List<Int>> = listOf(
        listOf(c(0xFFFFF8EB), c(0xFFF1E0C0)),
        listOf(c(0xFFFDE2E4), c(0xFFF7C5CC)),
        listOf(c(0xFF4A0E1C), c(0xFF7B1E33)),
        listOf(c(0xFF0B3D2E), c(0xFF145A43)),
        listOf(c(0xFF0B1033), c(0xFF27306B)),
        listOf(c(0xFF6A3DE8), c(0xFFE84393), c(0xFFFFA62B)),
        listOf(c(0xFF111111), c(0xFF2A2A2A)),
        listOf(c(0xFFFAFAF7), c(0xFFE9E4DA)),
        listOf(c(0xFF00796B), c(0xFF4DB6AC)),
        listOf(c(0xFFFF7E5F), c(0xFFFEB47B)),
    )

    /** Палитра цветов текста. */
    val textColors: List<Int> = listOf(
        WHITE, IVORY, GOLD, 0xFFB8924AL, 0xFF8A6A2EL, 0xFF000000L,
        0xFF333333L, 0xFFE53935L, 0xFFB23A5AL, 0xFFFF5C8AL, 0xFFF48FB1L, 0xFFFFA62BL,
        0xFFFFEB3BL, 0xFF8BC34AL, 0xFF2E7D32L, 0xFF00BFA5L, 0xFF00E5FFL, 0xFF29B6F6L,
        0xFF1E5AA8L, 0xFF3949ABL, 0xFF7E57C2L, 0xFFFF4FD8L, 0xFF795548L, 0xFFBFD4FFL,
    ).map { c(it) }

    val quickPhrases: List<String> = listOf(
        "Поздравляем!",
        "С днём свадьбы!",
        "Счастья молодым!",
        "Горько!",
        "С днём рождения!",
        "С юбилеем!",
        "Любви и счастья!",
        "Будьте счастливы!",
        "Совет да любовь!",
        "Спасибо за праздник!",
    )
}
