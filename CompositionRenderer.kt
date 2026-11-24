package com.toylek.studio.data.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import com.toylek.studio.domain.model.Decoration
import com.toylek.studio.domain.model.Inset
import com.toylek.studio.domain.model.Template
import com.toylek.studio.domain.model.TextLayer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Всё, что нужно для отрисовки одного кадра оформления. */
data class RenderSpec(
    val template: Template,
    val texts: List<TextLayer>,
    /** Фото (или кадр видео для предпросмотра). null — область медиа остаётся прозрачной. */
    val media: Bitmap?,
    /**
     * true для видео: кадр растягивается на весь холст и лишь обрезается рамкой области —
     * так предпросмотр точно совпадает с итоговым наложением поверх видео.
     */
    val mediaFillsCanvas: Boolean,
    /** Пользовательская картинка фона (перекрывает градиент). */
    val backgroundImage: Bitmap?,
    /** Пользовательский градиент фона (перекрывает градиент шаблона). */
    val backgroundColors: List<Int>?,
)

/**
 * Единый отрисовщик оформления: используется и в редакторе (предпросмотр),
 * и при сохранении фото, и для построения слоя-наложения на видео.
 */
class CompositionRenderer(private val fonts: FontProvider) {

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    fun effectiveInset(spec: RenderSpec): Inset {
        val t = spec.template.inset
        val hasCustomBg = spec.backgroundImage != null || spec.backgroundColors != null
        return if (t.isZero && hasCustomBg) Inset.all(0.07f) else t
    }

    fun mediaRect(spec: RenderSpec, w: Float, h: Float): RectF {
        val i = effectiveInset(spec)
        return RectF(w * i.left, h * i.top, w * (1f - i.right), h * (1f - i.bottom))
    }

    fun draw(canvas: Canvas, width: Int, height: Int, spec: RenderSpec) {
        val w = width.toFloat()
        val h = height.toFloat()
        val minDim = min(w, h)
        val inset = effectiveInset(spec)
        val rect = mediaRect(spec, w, h)
        val radius = spec.template.cornerRadius * minDim

        // 1. Фон
        if (!inset.isZero) {
            drawBackground(canvas, w, h, spec)
        }

        // 2. Фото / кадр видео или прозрачное «окно» под видео
        val media = spec.media
        if (media != null && !media.isRecycled) {
            canvas.save()
            if (radius > 0f) {
                val clip = Path().apply { addRoundRect(rect, radius, radius, Path.Direction.CW) }
                canvas.clipPath(clip)
            } else {
                canvas.clipRect(rect)
            }
            if (spec.mediaFillsCanvas) {
                drawBitmapCenterCrop(canvas, media, RectF(0f, 0f, w, h))
            } else {
                drawBitmapCenterCrop(canvas, media, rect)
            }
            canvas.restore()
        } else if (!inset.isZero) {
            if (radius > 0f) canvas.drawRoundRect(rect, radius, radius, clearPaint)
            else canvas.drawRect(rect, clearPaint)
        }

        // 3. Рамка
        drawFrame(canvas, w, h, rect, radius, inset.isZero, spec.template)

        // 4. Декор
        drawDecoration(canvas, w, h, rect, inset.isZero, spec.template)

        // 5. Лента под надписью
        spec.template.ribbon?.let { r ->
            val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = r.color }
            val top = (r.centerY - r.height / 2f) * h
            canvas.drawRect(0f, top, w, top + r.height * h, p)
        }

        // 6. Надписи
        spec.texts.forEach { drawText(canvas, w, h, it) }
    }

    /** Рисует оформление в новый Bitmap заданного размера. */
    fun renderToBitmap(width: Int, height: Int, spec: RenderSpec): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.TRANSPARENT)
        draw(Canvas(bmp), width, height, spec)
        return bmp
    }

    // ---------- Текст ----------

    private fun textPaint(layer: TextLayer, h: Float): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = fonts.typeface(layer.fontId)
            textSize = max(4f, layer.size * h)
            textAlign = Paint.Align.CENTER
            color = layer.color
        }

    /** Прямоугольник, занимаемый надписью (для выбора и перетаскивания пальцем). */
    fun textBounds(layer: TextLayer, w: Float, h: Float): RectF {
        val paint = textPaint(layer, h)
        val lines = layer.text.ifEmpty { " " }.split('\n')
        val lineH = paint.textSize * LINE_SPACING
        val maxW = lines.maxOf { paint.measureText(it) }
        val totalH = lineH * lines.size
        val cx = layer.x * w
        val cy = layer.y * h
        val pad = paint.textSize * 0.25f
        return RectF(cx - maxW / 2f - pad, cy - totalH / 2f - pad, cx + maxW / 2f + pad, cy + totalH / 2f + pad)
    }

    private fun drawText(canvas: Canvas, w: Float, h: Float, layer: TextLayer) {
        if (layer.text.isBlank()) return
        val paint = textPaint(layer, h)
        val lines = layer.text.split('\n')
        val fm = paint.fontMetrics
        val lineH = paint.textSize * LINE_SPACING
        val totalH = lineH * lines.size
        val top = layer.y * h - totalH / 2f
        val cx = layer.x * w
        val glyphH = fm.descent - fm.ascent

        val strokePaint = if (layer.outline) Paint(paint).apply {
            style = Paint.Style.STROKE
            strokeWidth = paint.textSize * 0.09f
            strokeJoin = Paint.Join.ROUND
            color = if (luminance(layer.color) > 0.5f) Color.BLACK else Color.WHITE
        } else null

        if (layer.shadow) {
            paint.setShadowLayer(paint.textSize * 0.12f, 0f, paint.textSize * 0.05f, 0xAA000000.toInt())
        }

        lines.forEachIndexed { index, line ->
            val baseline = top + index * lineH + (lineH - glyphH) / 2f - fm.ascent
            strokePaint?.let { canvas.drawText(line, cx, baseline, it) }
            canvas.drawText(line, cx, baseline, paint)
        }
    }

    private fun luminance(color: Int): Float {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f
        return 0.299f * r + 0.587f * g + 0.114f * b
    }

    // ---------- Фон и изображения ----------

    private fun drawBackground(canvas: Canvas, w: Float, h: Float, spec: RenderSpec) {
        val image = spec.backgroundImage
        if (image != null && !image.isRecycled) {
            drawBitmapCenterCrop(canvas, image, RectF(0f, 0f, w, h))
            return
        }
        val colors = spec.backgroundColors ?: spec.template.background
        if (colors.isEmpty()) return
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        if (colors.size == 1) {
            p.color = colors[0]
        } else {
            p.shader = LinearGradient(0f, 0f, w, h, colors.toIntArray(), null, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, w, h, p)
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bmp: Bitmap, dst: RectF) {
        val scale = max(dst.width() / bmp.width, dst.height() / bmp.height)
        val sw = bmp.width * scale
        val sh = bmp.height * scale
        val m = Matrix().apply {
            postScale(scale, scale)
            postTranslate(dst.left + (dst.width() - sw) / 2f, dst.top + (dst.height() - sh) / 2f)
        }
        canvas.drawBitmap(bmp, m, bitmapPaint)
    }

    // ---------- Рамка ----------

    private fun drawFrame(
        canvas: Canvas, w: Float, h: Float, rect: RectF, radius: Float, fullBleed: Boolean, t: Template,
    ) {
        val color = t.frameColor ?: return
        val minDim = min(w, h)
        val stroke = max(1f, t.frameWidth * minDim)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            this.color = color
        }
        val frameRect = if (fullBleed) {
            val m = 0.035f * minDim
            RectF(m, m, w - m, h - m)
        } else {
            RectF(rect.left - stroke / 2f, rect.top - stroke / 2f, rect.right + stroke / 2f, rect.bottom + stroke / 2f)
        }
        val r = if (fullBleed) 0f else radius + stroke / 2f
        canvas.drawRoundRect(frameRect, r, r, p)
        if (t.doubleFrame) {
            val gap = stroke * 2.6f
            p.strokeWidth = stroke * 0.5f
            val outer = if (fullBleed) {
                RectF(frameRect.left + gap, frameRect.top + gap, frameRect.right - gap, frameRect.bottom - gap)
            } else {
                RectF(frameRect.left - gap, frameRect.top - gap, frameRect.right + gap, frameRect.bottom + gap)
            }
            val r2 = if (fullBleed) 0f else r + gap
            canvas.drawRoundRect(outer, r2, r2, p)
        }
    }

    // ---------- Декор ----------

    private fun drawDecoration(canvas: Canvas, w: Float, h: Float, rect: RectF, fullBleed: Boolean, t: Template) {
        val minDim = min(w, h)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = t.decorationColor }
        val rnd = Random(t.id.hashCode())
        when (t.decoration) {
            Decoration.NONE -> Unit
            Decoration.HEARTS -> {
                repeat(18) {
                    val pt = randomMarginPoint(rnd, w, h, rect, fullBleed)
                    val size = minDim * (0.025f + rnd.nextFloat() * 0.035f)
                    p.alpha = 140 + rnd.nextInt(115)
                    canvas.drawPath(heartPath(pt.first, pt.second, size), p)
                }
            }
            Decoration.CONFETTI -> {
                val palette = intArrayOf(
                    t.decorationColor, 0xFFFFD54F.toInt(), 0xFFFF5C8A.toInt(), 0xFF4FC3F7.toInt(), 0xFF81C784.toInt(),
                )
                repeat(70) {
                    val pt = randomMarginPoint(rnd, w, h, rect, fullBleed)
                    p.color = palette[rnd.nextInt(palette.size)]
                    p.alpha = 200
                    val s = minDim * (0.006f + rnd.nextFloat() * 0.012f)
                    if (rnd.nextBoolean()) {
                        canvas.drawCircle(pt.first, pt.second, s, p)
                    } else {
                        canvas.save()
                        canvas.rotate(rnd.nextFloat() * 180f, pt.first, pt.second)
                        canvas.drawRect(pt.first - s * 1.6f, pt.second - s * 0.6f, pt.first + s * 1.6f, pt.second + s * 0.6f, p)
                        canvas.restore()
                    }
                }
            }
            Decoration.STARS -> {
                repeat(26) {
                    val pt = randomMarginPoint(rnd, w, h, rect, fullBleed)
                    val size = minDim * (0.008f + rnd.nextFloat() * 0.022f)
                    p.alpha = 150 + rnd.nextInt(105)
                    canvas.drawPath(starPath(pt.first, pt.second, size), p)
                }
            }
            Decoration.CORNERS -> drawCorners(canvas, w, h, minDim, p)
            Decoration.ORNAMENT -> drawOrnament(canvas, w, h, minDim, rect, fullBleed, p)
            Decoration.FILM -> drawFilm(canvas, w, h, rect, p)
            Decoration.BALLOONS -> drawBalloons(canvas, w, h, minDim, rnd)
        }
    }

    /** Случайная точка в полях вокруг фото (или в верхней/нижней полосе, если фото во весь кадр). */
    private fun randomMarginPoint(rnd: Random, w: Float, h: Float, rect: RectF, fullBleed: Boolean): Pair<Float, Float> {
        if (fullBleed) {
            val band = h * 0.11f
            val y = if (rnd.nextBoolean()) rnd.nextFloat() * band else h - rnd.nextFloat() * band
            return rnd.nextFloat() * w to y
        }
        repeat(20) {
            val x = rnd.nextFloat() * w
            val y = rnd.nextFloat() * h
            if (!rect.contains(x, y)) return x to y
        }
        return rnd.nextFloat() * w to rnd.nextFloat() * rect.top
    }

    private fun heartPath(cx: Float, cy: Float, s: Float): Path = Path().apply {
        moveTo(cx, cy + s * 0.9f)
        cubicTo(cx - s * 1.4f, cy - s * 0.1f, cx - s * 0.7f, cy - s * 1.2f, cx, cy - s * 0.45f)
        cubicTo(cx + s * 0.7f, cy - s * 1.2f, cx + s * 1.4f, cy - s * 0.1f, cx, cy + s * 0.9f)
        close()
    }

    private fun starPath(cx: Float, cy: Float, r: Float): Path = Path().apply {
        val inner = r * 0.45f
        for (i in 0 until 10) {
            val angle = Math.PI / 5 * i - Math.PI / 2
            val rad = if (i % 2 == 0) r else inner
            val x = cx + (rad * cos(angle)).toFloat()
            val y = cy + (rad * sin(angle)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }

    private fun drawCorners(canvas: Canvas, w: Float, h: Float, minDim: Float, p: Paint) {
        val size = minDim * 0.13f
        val margin = minDim * 0.018f
        val stroke = Paint(p).apply {
            style = Paint.Style.STROKE
            strokeWidth = max(1.5f, minDim * 0.004f)
            strokeCap = Paint.Cap.ROUND
        }
        val corners = listOf(
            Triple(margin, margin, 0f),
            Triple(w - margin, margin, 90f),
            Triple(w - margin, h - margin, 180f),
            Triple(margin, h - margin, 270f),
        )
        for ((x, y, rot) in corners) {
            canvas.save()
            canvas.translate(x, y)
            canvas.rotate(rot)
            val path = Path().apply {
                moveTo(0f, size)
                lineTo(0f, size * 0.25f)
                quadTo(0f, 0f, size * 0.25f, 0f)
                lineTo(size, 0f)
                moveTo(size * 0.12f, size * 0.75f)
                quadTo(size * 0.12f, size * 0.12f, size * 0.75f, size * 0.12f)
                moveTo(size * 0.3f, size * 0.3f)
                cubicTo(size * 0.55f, size * 0.15f, size * 0.62f, size * 0.42f, size * 0.42f, size * 0.42f)
            }
            canvas.drawPath(path, stroke)
            canvas.drawCircle(size * 0.25f, size * 0.25f, minDim * 0.006f, p)
            canvas.drawPath(diamond(size, 0f, minDim * 0.008f), p)
            canvas.drawPath(diamond(0f, size, minDim * 0.008f), p)
            canvas.restore()
        }
    }

    private fun diamond(cx: Float, cy: Float, r: Float): Path = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r, cy)
        lineTo(cx, cy + r)
        lineTo(cx - r, cy)
        close()
    }

    private fun drawOrnament(canvas: Canvas, w: Float, h: Float, minDim: Float, rect: RectF, fullBleed: Boolean, p: Paint) {
        val r = minDim * 0.011f
        val step = r * 4.2f
        val topY = if (fullBleed) minDim * 0.06f else rect.top / 2f
        val bottomY = if (fullBleed) h - minDim * 0.06f else (rect.bottom + h) / 2f
        val count = (w / step).toInt()
        val startX = (w - (count - 1) * step) / 2f
        for (i in 0 until count) {
            val x = startX + i * step
            val rr = if (i % 2 == 0) r else r * 0.55f
            canvas.drawPath(diamond(x, topY, rr), p)
            if (!fullBleed || bottomY > 0) canvas.drawPath(diamond(x, bottomY, rr), p)
        }
        // Круглые розетки в углах
        val stroke = Paint(p).apply { style = Paint.Style.STROKE; strokeWidth = max(1.5f, minDim * 0.003f) }
        val cr = minDim * 0.03f
        listOf(cr * 1.6f to cr * 1.6f, w - cr * 1.6f to cr * 1.6f, cr * 1.6f to h - cr * 1.6f, w - cr * 1.6f to h - cr * 1.6f)
            .forEach { (x, y) ->
                canvas.drawCircle(x, y, cr, stroke)
                canvas.drawPath(starPath(x, y, cr * 0.7f), p)
            }
    }

    private fun drawFilm(canvas: Canvas, w: Float, h: Float, rect: RectF, p: Paint) {
        val bandTop = rect.top
        val bandBottom = h - rect.bottom
        val holeH = min(bandTop, bandBottom) * 0.32f
        if (holeH <= 0f) return
        val holeW = holeH * 1.4f
        val step = holeW * 2.1f
        val count = (w / step).toInt() + 1
        val radius = holeH * 0.25f
        for (i in 0 until count) {
            val x = i * step + step * 0.25f
            canvas.drawRoundRect(RectF(x, bandTop * 0.2f, x + holeW, bandTop * 0.2f + holeH), radius, radius, p)
            val by = rect.bottom + bandBottom * 0.8f - holeH
            canvas.drawRoundRect(RectF(x, by, x + holeW, by + holeH), radius, radius, p)
        }
    }

    private fun drawBalloons(canvas: Canvas, w: Float, h: Float, minDim: Float, rnd: Random) {
        val colors = intArrayOf(0xFFFF5C8A.toInt(), 0xFFFFD54F.toInt(), 0xFF7E57C2.toInt(), 0xFF26C6DA.toInt(), 0xFFFF8A65.toInt())
        val stringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x99FFFFFF.toInt()
            style = Paint.Style.STROKE
            strokeWidth = max(1f, minDim * 0.002f)
        }
        val shine = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x55FFFFFF }
        val sides = listOf(w * 0.08f, w * 0.92f)
        for (sideX in sides) {
            repeat(3) { i ->
                val bw = minDim * (0.07f + rnd.nextFloat() * 0.02f)
                val bh = bw * 1.25f
                val cx = sideX + (i - 1) * bw * 0.9f
                val cy = h * (0.22f + i * 0.17f + rnd.nextFloat() * 0.05f)
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colors[rnd.nextInt(colors.size)] }
                canvas.drawLine(cx, cy + bh / 2f, sideX, h * 0.95f, stringPaint)
                canvas.drawOval(RectF(cx - bw / 2f, cy - bh / 2f, cx + bw / 2f, cy + bh / 2f), p)
                canvas.drawOval(RectF(cx - bw * 0.3f, cy - bh * 0.38f, cx - bw * 0.08f, cy - bh * 0.12f), shine)
            }
        }
    }

    companion object {
        const val LINE_SPACING = 1.18f
    }
}
