package com.toylek.studio.ui.components

import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.toylek.studio.ui.theme.Toylek

/** Сообщает о касании (для таймера автовозврата на главный экран). */
val LocalInteraction = staticCompositionLocalOf<() -> Unit> { {} }

fun Modifier.reportInteraction(): Modifier = composed {
    val report = LocalInteraction.current
    pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial)
                report()
            }
        }
    }
}

enum class ButtonStyle { GOLD, DARK, DANGER, GHOST }

/** Крупная кнопка, удобная для гостей любого возраста. */
@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    style: ButtonStyle = ButtonStyle.DARK,
    enabled: Boolean = true,
    height: Dp = 72.dp,
) {
    val shape = RoundedCornerShape(height / 2)
    val (bg, fg, border) = when (style) {
        ButtonStyle.GOLD -> Triple<Brush, Color, Color>(Toylek.GoldBrush, Color(0xFF241A08), Color.Transparent)
        ButtonStyle.DARK -> Triple<Brush, Color, Color>(Brush.linearGradient(listOf(Toylek.SurfaceHigh, Toylek.Surface)), Toylek.TextPrimary, Toylek.Outline)
        ButtonStyle.DANGER -> Triple<Brush, Color, Color>(Brush.linearGradient(listOf(Color(0xFF7A1C24), Color(0xFF55121A))), Color.White, Color(0xFF9C2A35))
        ButtonStyle.GHOST -> Triple<Brush, Color, Color>(Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))), Toylek.TextPrimary, Color(0x44FFFFFF))
    }
    Row(
        modifier = modifier
            .heightIn(min = height)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .then(if (enabled) Modifier else Modifier.background(Color(0x66000000))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/** Круглая кнопка с иконкой и подписью под ней. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    background: Color = Color(0x99000000),
    tint: Color = Color.White,
    enabled: Boolean = true,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .border(1.5.dp, Color(0x55FFFFFF), CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (enabled) tint else tint.copy(alpha = 0.4f), modifier = Modifier.size(size * 0.45f))
        }
        if (label != null) {
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BackPill(onClick: () -> Unit, modifier: Modifier = Modifier, text: String = "Назад") {
    BigButton(
        text = text,
        onClick = onClick,
        icon = Icons.AutoMirrored.Filled.ArrowBack,
        style = ButtonStyle.GHOST,
        modifier = modifier,
        height = 64.dp,
    )
}

@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    dismissible: Boolean = true,
    width: Dp = 560.dp,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = { if (dismissible) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = dismissible,
            dismissOnClickOutside = dismissible,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Toylek.Surface,
            modifier = Modifier
                .widthIn(max = width)
                .fillMaxWidth(0.9f)
                .border(1.dp, Toylek.Outline, RoundedCornerShape(28.dp))
                .reportInteraction(),
        ) {
            Box(Modifier.padding(32.dp)) { content() }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    AppDialog(onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Toylek.TextPrimary)
            Text(message, style = MaterialTheme.typography.bodyLarge, color = Toylek.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                BigButton("Отмена", onDismiss, Modifier.weight(1f))
                BigButton(
                    confirmText, onConfirm, Modifier.weight(1f),
                    style = if (danger) ButtonStyle.DANGER else ButtonStyle.GOLD,
                )
            }
        }
    }
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    singleLine: Boolean = false,
    maxLength: Int = 200,
) {
    var value by remember { mutableStateOf(initial) }
    val report = LocalInteraction.current
    AppDialog(onDismiss = onDismiss, width = 720.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Toylek.TextPrimary)
            OutlinedTextField(
                value = value,
                onValueChange = { if (it.length <= maxLength) value = it; report() },
                placeholder = { Text(placeholder, fontSize = 20.sp) },
                singleLine = singleLine,
                minLines = if (singleLine) 1 else 3,
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 24.sp, color = Toylek.TextPrimary),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Toylek.Gold,
                    unfocusedBorderColor = Toylek.Outline,
                    cursorColor = Toylek.Gold,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                BigButton("Отмена", onDismiss, Modifier.weight(1f))
                BigButton("Сохранить", { onSave(value) }, Modifier.weight(1f), style = ButtonStyle.GOLD)
            }
        }
    }
}

/** Ввод PIN-кода крупной цифровой клавиатурой. */
@Composable
fun PinDialog(
    title: String,
    expectedPin: String?,
    onSuccess: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var entered by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AppDialog(onDismiss = onDismiss, width = 460.dp) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = Toylek.TextPrimary, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (i < entered.length) Toylek.Gold else Toylek.Outline)
                    )
                }
            }
            Text(
                if (error) "Неверный PIN-код" else " ",
                color = Toylek.Rec,
                style = MaterialTheme.typography.bodyMedium,
            )
            val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "<")
            keys.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .background(Toylek.SurfaceHigh)
                                .clickable {
                                    error = false
                                    when (key) {
                                        "C" -> entered = ""
                                        "<" -> entered = entered.dropLast(1)
                                        else -> if (entered.length < 4) {
                                            entered += key
                                            if (entered.length == 4) {
                                                if (expectedPin == null || entered == expectedPin) {
                                                    onSuccess(entered)
                                                } else {
                                                    error = true
                                                    entered = ""
                                                }
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (key == "<") {
                                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Стереть", tint = Toylek.TextPrimary)
                            } else {
                                Text(key, fontSize = 32.sp, color = Toylek.TextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            BigButton("Отмена", onDismiss, Modifier.fillMaxWidth(), height = 60.dp)
        }
    }
}

@Composable
fun ProgressDialog(title: String, progress: Int?) {
    AppDialog(onDismiss = {}, dismissible = false) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
            if (progress == null) {
                CircularProgressIndicator(color = Toylek.Gold, modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = Toylek.TextPrimary, textAlign = TextAlign.Center)
            if (progress != null) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0, 100) / 100f },
                    color = Toylek.Gold,
                    trackColor = Toylek.Outline,
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                )
                Text("$progress%", style = MaterialTheme.typography.titleMedium, color = Toylek.Gold)
            }
            Text("Пожалуйста, подождите…", style = MaterialTheme.typography.bodyMedium, color = Toylek.TextSecondary)
        }
    }
}

/** Встроенный проигрыватель (системный VideoView — стабилен и не требует сети). */
@Composable
fun VideoPlayer(
    uri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = false,
    showControls: Boolean = true,
    onError: () -> Unit = {},
) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            FrameLayout(ctx).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                val vv = VideoView(ctx)
                addView(
                    vv,
                    FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.CENTER,
                    ),
                )
                if (showControls) {
                    val controller = MediaController(ctx)
                    controller.setAnchorView(this)
                    vv.setMediaController(controller)
                }
                vv.setOnErrorListener { _, _, _ -> onError(); true }
                videoView = vv
            }
        },
        update = {
            val vv = videoView ?: return@AndroidView
            if (vv.tag != uri) {
                vv.tag = uri
                vv.setOnPreparedListener { mp ->
                    mp.isLooping = loop
                    if (autoPlay) vv.start() else vv.seekTo(1)
                }
                vv.setVideoURI(uri)
            }
        },
    )
    DisposableEffect(Unit) {
        onDispose {
            try {
                videoView?.stopPlayback()
            } catch (_: Exception) {
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Toylek.Gold,
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 2.sp,
        modifier = modifier.padding(PaddingValues(vertical = 6.dp)),
    )
}
