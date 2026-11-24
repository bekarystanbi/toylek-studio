package com.toylek.studio.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.toylek.studio.ToylekApp
import com.toylek.studio.data.camera.CameraService
import com.toylek.studio.data.camera.CaptureMode
import com.toylek.studio.data.camera.RecordingEvent
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.CapturedMedia
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.components.BackPill
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.RoundIconButton
import com.toylek.studio.ui.theme.Toylek
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private const val MIN_FREE_BYTES_FOR_VIDEO = 200L * 1024 * 1024

@Composable
fun CameraScreen(vm: MainViewModel, settings: AppSettings, initialMode: CaptureMode) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val container = (context.applicationContext as ToylekApp).container
    val camera = remember { CameraService(context.applicationContext, container.mediaRepository) }
    val alive = remember { AtomicBoolean(true) }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }

    var mode by remember { mutableStateOf(initialMode) }
    var front by remember { mutableStateOf(settings.defaultFrontCamera) }
    var lenses by remember { mutableStateOf(true to true) }
    var retryKey by remember { mutableStateOf(0) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraReady by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var capturing by remember { mutableStateOf(false) }
    val flash = remember { Animatable(0f) }
    var micGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }
    val maxMs = settings.maxVideoSeconds.coerceIn(10, AppSettings.MAX_VIDEO_SECONDS) * 1000L

    LaunchedEffect(mode, front, retryKey) {
        cameraError = null
        cameraReady = false
        try {
            val (hasFront, hasBack) = camera.availableLenses()
            lenses = hasFront to hasBack
            if (!hasFront && !hasBack) {
                cameraError = "Камера не найдена на этом устройстве"
                return@LaunchedEffect
            }
            val useFront = when {
                front && !hasFront -> false
                !front && !hasBack -> true
                else -> front
            }
            if (useFront != front) {
                front = useFront
                return@LaunchedEffect
            }
            val rotation = view.display?.rotation ?: android.view.Surface.ROTATION_0
            camera.bind(lifecycleOwner, previewView, front, mode, rotation)
            cameraReady = true
        } catch (e: Exception) {
            cameraError = "Не удалось открыть камеру. Закройте другие приложения, использующие камеру, и нажмите «Повторить»."
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            alive.set(false)
            camera.release()
            vm.setBusy("recording", false)
        }
    }

    fun beginRecording() {
        if (vm.freeSpaceBytes() < MIN_FREE_BYTES_FOR_VIDEO) {
            vm.message("Недостаточно свободной памяти для записи видео")
            return
        }
        elapsedMs = 0L
        val started = camera.startRecording(maxMs, micGranted) { event ->
            if (!alive.get()) return@startRecording
            when (event) {
                is RecordingEvent.Progress -> {
                    elapsedMs = event.elapsedMs
                    vm.touch()
                }
                is RecordingEvent.Finished -> {
                    isRecording = false
                    vm.setBusy("recording", false)
                    if (event.limitReached) vm.message("Достигнута максимальная длительность видео")
                    vm.onCaptured(CapturedMedia(event.uri, MediaType.VIDEO))
                }
                is RecordingEvent.Failed -> {
                    isRecording = false
                    vm.setBusy("recording", false)
                    vm.message(event.message)
                }
            }
        }
        if (started) {
            isRecording = true
            vm.setBusy("recording", true)
        }
    }

    fun takePhoto() {
        if (capturing || !cameraReady) return
        capturing = true
        vm.setBusy("photo", true)
        scope.launch {
            for (i in settings.photoCountdownSeconds downTo 1) {
                countdown = i
                delay(1000)
            }
            countdown = null
            launch {
                flash.snapTo(1f)
                flash.animateTo(0f, tween(450))
            }
            camera.takePhoto(
                onPhotoSaved = { uri ->
                    capturing = false
                    vm.setBusy("photo", false)
                    if (alive.get()) vm.onCaptured(CapturedMedia(uri, MediaType.PHOTO))
                },
                onFailure = { msg ->
                    capturing = false
                    vm.setBusy("photo", false)
                    vm.message(msg)
                },
            )
        }
    }

    // Во время записи «Назад» останавливает запись (видео сохраняется), а не теряет её.
    BackHandler(enabled = isRecording || capturing) {
        if (isRecording) camera.stopRecording()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Верхняя панель
        if (!isRecording && !capturing) {
            BackPill(onClick = { vm.back() }, modifier = Modifier.align(Alignment.TopStart).padding(24.dp))
        }

        if (mode == CaptureMode.VIDEO) {
            RecordingTimer(
                isRecording = isRecording,
                elapsedMs = elapsedMs,
                maxMs = maxMs,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp),
            )
            if (!micGranted && !isRecording) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xCC000000))
                        .clickable { micLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MicOff, null, tint = Toylek.Rec)
                    Spacer(Modifier.width(10.dp))
                    Text("Видео без звука — нажмите, чтобы разрешить микрофон", color = Color.White, fontSize = 18.sp)
                }
            }
        }

        // Правая панель управления
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(Color(0x66000000))
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            ModeSwitch(
                mode = mode,
                enabled = !isRecording && !capturing,
                onChange = { mode = it },
            )
            ShutterButton(
                mode = mode,
                isRecording = isRecording,
                enabled = cameraReady && !capturing,
                onClick = {
                    if (mode == CaptureMode.PHOTO) takePhoto()
                    else if (isRecording) camera.stopRecording()
                    else beginRecording()
                },
            )
            if (lenses.first && lenses.second) {
                RoundIconButton(
                    icon = Icons.Filled.Cameraswitch,
                    label = "Камера",
                    enabled = !isRecording && !capturing,
                    onClick = { front = !front },
                )
            }
        }

        // Подсказка для гостей
        if (!isRecording && !capturing && cameraError == null) {
            Text(
                if (mode == CaptureMode.PHOTO) "Улыбнитесь и нажмите белую кнопку" else "Нажмите красную кнопку и скажите поздравление",
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 28.dp, vertical = 12.dp),
            )
        }

        // Обратный отсчёт
        AnimatedVisibility(
            visible = countdown != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center),
        ) {
            Box(
                Modifier.size(220.dp).clip(CircleShape).background(Color(0x88000000)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${countdown ?: ""}", color = Color.White, fontSize = 140.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Вспышка
        if (flash.value > 0f) {
            Box(Modifier.fillMaxSize().alpha(flash.value).background(Color.White))
        }

        // Ошибка камеры
        cameraError?.let { error ->
            Box(Modifier.fillMaxSize().background(Color(0xEE0E0B10)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    Icon(Icons.Filled.VideocamOff, null, tint = Toylek.Rec, modifier = Modifier.size(96.dp))
                    Text(
                        error,
                        color = Toylek.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 80.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        BigButton("Назад", { vm.back() })
                        BigButton("Повторить", { retryKey++ }, icon = Icons.Filled.Refresh, style = ButtonStyle.GOLD)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSwitch(mode: CaptureMode, enabled: Boolean, onChange: (CaptureMode) -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0x55000000))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(32.dp))
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(CaptureMode.VIDEO to "Видео", CaptureMode.PHOTO to "Фото").forEach { (m, label) ->
            val selected = m == mode
            Row(
                Modifier
                    .width(150.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (selected) Toylek.Gold else Color.Transparent)
                    .clickable(enabled = enabled && !selected) { onChange(m) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (m == CaptureMode.VIDEO) Icons.Filled.Videocam else Icons.Filled.PhotoCamera,
                    null,
                    tint = if (selected) Color(0xFF241A08) else Color.White,
                )
                Spacer(Modifier.width(8.dp))
                Text(label, color = if (selected) Color(0xFF241A08) else Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ShutterButton(mode: CaptureMode, isRecording: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(136.dp)
                .clip(CircleShape)
                .border(6.dp, Color.White, CircleShape)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            when {
                mode == CaptureMode.PHOTO -> Box(
                    Modifier.size(106.dp).clip(CircleShape).background(if (enabled) Color.White else Color.Gray)
                )
                isRecording -> Box(
                    Modifier
                        .size((56 * pulseScale).dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Toylek.Rec)
                )
                else -> Box(Modifier.size(104.dp).clip(CircleShape).background(if (enabled) Toylek.Rec else Color.Gray))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            when {
                mode == CaptureMode.PHOTO -> "Снять"
                isRecording -> "Стоп"
                else -> "Запись"
            },
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RecordingTimer(isRecording: Boolean, elapsedMs: Long, maxMs: Long, modifier: Modifier = Modifier) {
    val blink = rememberInfiniteTransition(label = "blink")
    val dotAlpha by blink.animateFloat(1f, 0.2f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot")
    val remaining = (maxMs - elapsedMs).coerceAtLeast(0L)
    val warning = isRecording && remaining <= 15_000L
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(if (warning) Color(0xDD8B1A24) else Color(0xAA000000))
            .padding(horizontal = 28.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(18.dp)
                    .alpha(if (isRecording) dotAlpha else 0.35f)
                    .clip(CircleShape)
                    .background(Toylek.Rec)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${formatTime(elapsedMs)} / ${formatTime(maxMs)}",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (isRecording) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (elapsedMs.toFloat() / maxMs).coerceIn(0f, 1f) },
                color = if (warning) Color.White else Toylek.Rec,
                trackColor = Color(0x44FFFFFF),
                modifier = Modifier.width(260.dp).height(6.dp).clip(RoundedCornerShape(3.dp)),
            )
            if (warning) {
                Spacer(Modifier.height(4.dp))
                Text("Осталось ${remaining / 1000} с", color = Color.White, fontSize = 18.sp)
            }
        }
    }
}

fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}
