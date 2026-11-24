package com.toylek.studio.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toylek.studio.ToylekApp
import com.toylek.studio.data.render.CompositionRenderer
import com.toylek.studio.data.render.FontCatalog
import com.toylek.studio.data.render.RenderSpec
import com.toylek.studio.data.render.TemplateCatalog
import com.toylek.studio.domain.model.BackgroundChoice
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.domain.model.Template
import com.toylek.studio.ui.EditorExport
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.Screen
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.SectionLabel
import com.toylek.studio.ui.theme.Toylek
import com.toylek.studio.util.BitmapLoader

private enum class EditorTab(val title: String, val icon: ImageVector) {
    TEMPLATES("Шаблоны", Icons.Filled.AutoAwesome),
    TEXT("Текст", Icons.Filled.TextFields),
    FONT("Шрифт", Icons.Filled.FontDownload),
    COLOR("Цвет", Icons.Filled.Palette),
    BACKGROUND("Фон", Icons.Filled.Wallpaper),
}

@Composable
fun EditorScreen(vm: MainViewModel, screen: Screen.Editor) {
    val context = LocalContext.current
    val renderer = remember { (context.applicationContext as ToylekApp).container.renderer }
    val state = remember(screen.uri) { EditorState() }
    var tab by remember { mutableStateOf(EditorTab.TEMPLATES) }
    var mediaBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var bgBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val isVideo = screen.type == MediaType.VIDEO

    LaunchedEffect(screen.uri) {
        val bmp = if (isVideo) {
            BitmapLoader.loadVideoFrame(context, screen.uri, PREVIEW_DIM)
        } else {
            BitmapLoader.loadImage(context, screen.uri, PREVIEW_DIM)
        }
        if (bmp == null) loadError = true else mediaBitmap = bmp
    }

    val bgChoice = state.background
    LaunchedEffect(bgChoice) {
        if (bgChoice is BackgroundChoice.Image) {
            val b = BitmapLoader.loadImage(context, bgChoice.uri, PREVIEW_DIM)
            if (b == null) {
                vm.message("Не удалось открыть изображение")
                state.background = BackgroundChoice.FromTemplate
            }
            bgBitmap = b
        } else {
            bgBitmap = null
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) state.background = BackgroundChoice.Image(uri)
    }

    fun spec(media: Bitmap?, template: Template = state.template, texts: List<com.toylek.studio.domain.model.TextLayer> = state.texts.toList()) =
        RenderSpec(
            template = template,
            texts = texts,
            media = media,
            mediaFillsCanvas = isVideo,
            backgroundImage = bgBitmap,
            backgroundColors = (state.background as? BackgroundChoice.Gradient)?.colors,
        )

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        // Верхняя панель
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            BigButton("Отмена", { vm.back() }, icon = Icons.Filled.Close, style = ButtonStyle.GHOST, height = 64.dp)
            Column(Modifier.weight(1f)) {
                Text("Оформление", style = MaterialTheme.typography.headlineMedium, color = Toylek.TextPrimary)
                Text(
                    if (isVideo) "Надписи и рамка будут наложены на всё видео" else "Перетаскивайте надписи пальцем",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Toylek.TextSecondary,
                )
            }
            BigButton(
                "Сохранить",
                {
                    vm.saveEdited(
                        EditorExport(
                            sourceUri = screen.uri,
                            type = screen.type,
                            templateId = state.template.id,
                            texts = state.texts.toList(),
                            background = state.background,
                            fromGallery = screen.fromGallery,
                        )
                    )
                },
                icon = Icons.Filled.Check,
                style = ButtonStyle.GOLD,
                enabled = mediaBitmap != null,
                height = 72.dp,
            )
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            // Холст предпросмотра
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF08060A)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = mediaBitmap
                when {
                    loadError -> Text("Не удалось открыть файл", color = Toylek.Rec, style = MaterialTheme.typography.titleLarge)
                    bmp == null -> CircularProgressIndicator(color = Toylek.Gold)
                    else -> {
                        val ratio = bmp.width.toFloat() / bmp.height
                        val boxRatio = maxWidth / maxHeight
                        val (w, h) = if (ratio > boxRatio) maxWidth to maxWidth / ratio else maxHeight * ratio to maxHeight
                        EditorCanvas(
                            renderer = renderer,
                            state = state,
                            spec = spec(bmp),
                            onInteraction = { vm.touch() },
                            modifier = Modifier.size(w, h),
                        )
                    }
                }
            }

            // Панель инструментов
            Column(
                Modifier
                    .width(470.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Toylek.Surface)
                    .padding(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    EditorTab.entries.forEach { t ->
                        val sel = t == tab
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (sel) Toylek.Gold else Toylek.SurfaceHigh)
                                .clickable {
                                    tab = t
                                    if (t == EditorTab.FONT || t == EditorTab.COLOR) state.ensureSelection()
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(t.icon, null, tint = if (sel) Color(0xFF241A08) else Toylek.TextPrimary)
                            Text(t.title, fontSize = 14.sp, color = if (sel) Color(0xFF241A08) else Toylek.TextPrimary, maxLines = 1)
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        EditorTab.TEMPLATES -> TemplatesTab(
                            renderer = renderer,
                            mediaBitmap = mediaBitmap,
                            selectedId = state.template.id,
                            specFor = { t -> spec(mediaBitmap, t, t.defaultTexts) },
                            onSelect = { state.applyTemplate(it) },
                        )
                        EditorTab.TEXT -> TextTab(state, onInteraction = { vm.touch() })
                        EditorTab.FONT -> FontTab(state)
                        EditorTab.COLOR -> ColorTab(state)
                        EditorTab.BACKGROUND -> BackgroundTab(
                            state = state,
                            isVideo = isVideo,
                            onPick = {
                                try {
                                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } catch (e: Exception) {
                                    vm.message("Галерея недоступна на этом устройстве")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val PREVIEW_DIM = 1600

@Composable
private fun EditorCanvas(
    renderer: CompositionRenderer,
    state: EditorState,
    spec: RenderSpec,
    onInteraction: () -> Unit,
    modifier: Modifier,
) {
    var dragId by remember { mutableStateOf<Long?>(null) }

    fun hitTest(pos: Offset, w: Float, h: Float): Long? =
        state.texts.lastOrNull { renderer.textBounds(it, w, h).contains(pos.x, pos.y) }?.id

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    onInteraction()
                    state.selectedId = hitTest(pos, size.width.toFloat(), size.height.toFloat())
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos ->
                        onInteraction()
                        val hit = hitTest(pos, size.width.toFloat(), size.height.toFloat())
                        if (hit != null) state.selectedId = hit
                        dragId = hit ?: state.selectedId
                    },
                    onDragEnd = { dragId = null },
                    onDragCancel = { dragId = null },
                    onDrag = { change, amount ->
                        change.consume()
                        onInteraction()
                        val id = dragId ?: return@detectDragGestures
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        state.update(id) {
                            it.copy(
                                x = (it.x + amount.x / w).coerceIn(0.03f, 0.97f),
                                y = (it.y + amount.y / h).coerceIn(0.03f, 0.97f),
                            )
                        }
                    },
                )
            }
    ) {
        val w = size.width
        val h = size.height
        drawIntoCanvas { canvas ->
            renderer.draw(canvas.nativeCanvas, w.toInt(), h.toInt(), spec)
        }
        state.selected?.let { layer ->
            val r = renderer.textBounds(layer, w, h)
            drawRect(
                color = Toylek.Gold,
                topLeft = Offset(r.left, r.top),
                size = Size(r.width(), r.height()),
                style = Stroke(width = 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
            )
        }
    }
}

@Composable
private fun TemplatesTab(
    renderer: CompositionRenderer,
    mediaBitmap: Bitmap?,
    selectedId: String,
    specFor: (Template) -> RenderSpec,
    onSelect: (Template) -> Unit,
) {
    val ratio = mediaBitmap?.let { it.width.toFloat() / it.height } ?: (16f / 9f)
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 12.dp),
    ) {
        items(TemplateCatalog.templates, key = { it.id }) { t ->
            val sel = t.id == selectedId
            Column(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(if (sel) 3.dp else 1.dp, if (sel) Toylek.Gold else Toylek.Outline, RoundedCornerShape(16.dp))
                    .clickable { onSelect(t) }
                    .padding(8.dp),
            ) {
                val spec = specFor(t)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2A2430))
                ) {
                    drawIntoCanvas { renderer.draw(it.nativeCanvas, size.width.toInt(), size.height.toInt(), spec) }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    t.title,
                    color = if (sel) Toylek.Gold else Toylek.TextPrimary,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TextTab(state: EditorState, onInteraction: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Надписи")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.texts.forEachIndexed { i, layer ->
                Chip(
                    text = layer.text.lineSequence().firstOrNull()?.take(14)?.ifBlank { null } ?: "Надпись ${i + 1}",
                    selected = layer.id == state.selectedId,
                    onClick = { state.selectedId = layer.id },
                )
            }
            Chip(text = "+ Добавить", selected = false, accent = true, onClick = { state.addText() })
        }

        val sel = state.selected
        if (sel == null) {
            Text(
                "Нажмите на надпись на картинке или добавьте новую.",
                color = Toylek.TextSecondary,
                style = MaterialTheme.typography.bodyLarge,
            )
            BigButton("Добавить надпись", { state.addText() }, icon = Icons.Filled.Add, style = ButtonStyle.GOLD, modifier = Modifier.fillMaxWidth())
        } else {
            SelectedTextControls(state, sel, onInteraction)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectedTextControls(state: EditorState, sel: com.toylek.studio.domain.model.TextLayer, onInteraction: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = sel.text,
            onValueChange = {
                if (it.length <= 120) state.setSelectedText(it)
                onInteraction()
            },
            label = { Text("Текст надписи") },
            minLines = 2,
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, color = Toylek.TextPrimary),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Toylek.Gold,
                unfocusedBorderColor = Toylek.Outline,
                cursorColor = Toylek.Gold,
                focusedLabelColor = Toylek.Gold,
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Быстрые фразы")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TemplateCatalog.quickPhrases.forEach { phrase ->
                Chip(text = phrase, selected = false, onClick = { state.setSelectedText(phrase) })
            }
        }

        SizeSlider(state)

        ToggleRow("Тень", sel.shadow) { v -> state.updateSelected { it.copy(shadow = v) } }
        ToggleRow("Контур", sel.outline) { v -> state.updateSelected { it.copy(outline = v) } }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BigButton(
                "По центру", { state.updateSelected { it.copy(x = 0.5f) } },
                icon = Icons.Filled.FormatAlignCenter, height = 60.dp, modifier = Modifier.weight(1f),
            )
            BigButton(
                "Удалить", { state.removeSelected() },
                icon = Icons.Filled.Delete, style = ButtonStyle.DANGER, height = 60.dp, modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SizeSlider(state: EditorState) {
    val sel = state.selected ?: return
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("Размер текста")
            Spacer(Modifier.weight(1f))
            Text("${(sel.size * 1000).toInt()}", color = Toylek.TextSecondary, fontSize = 18.sp)
        }
        Slider(
            value = sel.size,
            onValueChange = { v -> state.updateSelected { it.copy(size = v) } },
            valueRange = 0.03f..0.25f,
            colors = SliderDefaults.colors(thumbColor = Toylek.Gold, activeTrackColor = Toylek.Gold, inactiveTrackColor = Toylek.Outline),
        )
    }
}

@Composable
private fun FontTab(state: EditorState) {
    val sel = state.selected
    Column(Modifier.fillMaxSize()) {
        SizeSlider(state)
        Spacer(Modifier.height(6.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FontCatalog.fonts, key = { it.id }) { font ->
                val isSel = sel?.fontId == font.id
                val family = remember(font.id) { FontFamily(Font(font.resId)) }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSel) Color(0x33D4AF6A) else Toylek.SurfaceHigh)
                        .border(if (isSel) 2.dp else 0.dp, if (isSel) Toylek.Gold else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { state.updateSelected { it.copy(fontId = font.id) } }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        sel?.text?.lineSequence()?.firstOrNull()?.ifBlank { null } ?: "Поздравляем!",
                        fontFamily = family,
                        fontSize = 30.sp,
                        color = Toylek.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(font.title, fontSize = 15.sp, color = if (isSel) Toylek.Gold else Toylek.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun ColorTab(state: EditorState) {
    val sel = state.selected
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Цвет текста")
        TemplateCatalog.textColors.chunked(6).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { argb ->
                    val isSel = sel?.color == argb
                    Box(
                        Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(argb))
                            .border(if (isSel) 4.dp else 1.dp, if (isSel) Toylek.Gold else Color(0x55FFFFFF), CircleShape)
                            .clickable { state.updateSelected { it.copy(color = argb) } },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) {
                            Icon(
                                Icons.Filled.Check, null,
                                tint = if (Color(argb).luminanceCompat() > 0.5f) Color.Black else Color.White,
                            )
                        }
                    }
                }
            }
        }
        SizeSlider(state)
        ToggleRow("Тень", sel?.shadow ?: false) { v -> state.updateSelected { it.copy(shadow = v) } }
        ToggleRow("Контур", sel?.outline ?: false) { v -> state.updateSelected { it.copy(outline = v) } }
    }
}

private fun Color.luminanceCompat(): Float = 0.299f * red + 0.587f * green + 0.114f * blue

@Composable
private fun BackgroundTab(state: EditorState, isVideo: Boolean, onPick: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (isVideo) "Фон будет виден вокруг видео. Если у шаблона нет полей, видео будет обрамлено фоном."
            else "Фон будет виден вокруг фотографии.",
            color = Toylek.TextSecondary,
            style = MaterialTheme.typography.bodyMedium,
        )
        BigButton("Выбрать из галереи", onPick, icon = Icons.Filled.Image, style = ButtonStyle.GOLD, modifier = Modifier.fillMaxWidth())
        BigButton(
            "Как в шаблоне",
            { state.background = BackgroundChoice.FromTemplate },
            icon = Icons.Filled.AutoAwesome,
            modifier = Modifier.fillMaxWidth(),
            style = if (state.background is BackgroundChoice.FromTemplate) ButtonStyle.DARK else ButtonStyle.GHOST,
        )
        if (state.background is BackgroundChoice.Image) {
            Text("Выбрано изображение из галереи ✓", color = Toylek.Gold, style = MaterialTheme.typography.bodyLarge)
        }
        SectionLabel("Готовые фоны")
        TemplateCatalog.backgroundPresets.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { colors ->
                    val current = state.background
                    val isSel = current is BackgroundChoice.Gradient && current.colors == colors
                    Box(
                        Modifier
                            .weight(1f)
                            .height(84.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (colors.size == 1) Brush.linearGradient(listOf(Color(colors[0]), Color(colors[0])))
                                else Brush.linearGradient(colors.map { Color(it) })
                            )
                            .border(if (isSel) 4.dp else 1.dp, if (isSel) Toylek.Gold else Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                            .clickable { state.background = BackgroundChoice.Gradient(colors) },
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit, accent: Boolean = false) {
    Text(
        text,
        color = when {
            selected -> Color(0xFF241A08)
            accent -> Toylek.Gold
            else -> Toylek.TextPrimary
        },
        fontSize = 18.sp,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Toylek.Gold else Toylek.SurfaceHigh)
            .border(1.dp, if (accent) Toylek.Gold else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Toylek.SurfaceHigh)
            .clickable { onChange(!checked) }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Toylek.TextPrimary, fontSize = 20.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Toylek.Gold, checkedThumbColor = Color.White),
        )
    }
}
