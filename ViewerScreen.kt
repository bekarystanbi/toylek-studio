package com.toylek.studio.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.Screen
import com.toylek.studio.ui.camera.formatTime
import com.toylek.studio.ui.components.BackPill
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.ConfirmDialog
import com.toylek.studio.ui.components.PinDialog
import com.toylek.studio.ui.components.RoundIconButton
import com.toylek.studio.ui.components.TextInputDialog
import com.toylek.studio.ui.components.VideoPlayer
import com.toylek.studio.ui.theme.Toylek
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViewerScreen(vm: MainViewModel, settings: AppSettings, screen: Screen.Viewer) {
    val all by vm.media.collectAsStateWithLifecycle()
    val list = remember(all, screen.type) { all.filter { it.type == screen.type } }
    val index = list.indexOfFirst { it.uri == screen.uri }
    val item = list.getOrNull(index)

    var showCaption by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pinAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null && item != null) vm.exportToUsb(uri, listOf(item))
    }

    fun requireAdmin(action: () -> Unit) {
        if (settings.kioskEnabled) pinAction = action else action()
    }

    if (item == null) {
        // Файл удалён или ещё загружается список
        LaunchedEffect(all) {
            delay(1500)
            vm.back()
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Toylek.Gold)
        }
        return
    }

    val dateText = remember(item.dateAddedMillis) {
        SimpleDateFormat("d MMMM yyyy, HH:mm", Locale.forLanguageTag("ru")).format(Date(item.dateAddedMillis))
    }

    Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (item.type == MediaType.PHOTO) {
                AsyncImage(
                    model = item.uri,
                    contentDescription = item.caption.ifBlank { item.displayName },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                VideoPlayer(uri = item.uri, modifier = Modifier.fillMaxSize(), autoPlay = true)
            }
            if (index > 0) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = null,
                    size = 76.dp,
                    onClick = { vm.replace(Screen.Viewer(list[index - 1].uri, screen.type)) },
                    modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                )
            }
            if (index < list.lastIndex) {
                RoundIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowForward,
                    label = null,
                    size = 76.dp,
                    onClick = { vm.replace(Screen.Viewer(list[index + 1].uri, screen.type)) },
                    modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp),
                )
            }
        }

        Column(
            Modifier
                .width(400.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BackPill(onClick = { vm.back() }, text = "К галерее")
            Spacer(Modifier.height(4.dp))
            Text("${index + 1} из ${list.size}", style = MaterialTheme.typography.titleMedium, color = Toylek.Gold)
            Text(dateText, style = MaterialTheme.typography.bodyLarge, color = Toylek.TextSecondary)
            if (item.type == MediaType.VIDEO) {
                Text("Длительность: ${formatTime(item.durationMs)}", style = MaterialTheme.typography.bodyLarge, color = Toylek.TextSecondary)
            }
            Text(
                if (item.caption.isBlank()) "Без подписи" else "«${item.caption}»",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp),
                color = if (item.caption.isBlank()) Toylek.Outline else Toylek.GoldLight,
            )
            Spacer(Modifier.height(8.dp))
            BigButton(
                if (item.caption.isBlank()) "Добавить подпись" else "Изменить подпись",
                { showCaption = true },
                icon = Icons.Filled.EditNote,
                modifier = Modifier.fillMaxWidth(),
            )
            BigButton(
                "Оформить",
                { vm.navigate(Screen.Editor(item.uri, item.type, fromGallery = true)) },
                icon = Icons.Filled.AutoAwesome,
                style = ButtonStyle.GOLD,
                modifier = Modifier.fillMaxWidth(),
            )
            BigButton(
                "На флешку",
                {
                    requireAdmin {
                        try {
                            treeLauncher.launch(null)
                        } catch (e: Exception) {
                            vm.message("На устройстве нет системного файлового менеджера")
                        }
                    }
                },
                icon = Icons.Filled.Usb,
                modifier = Modifier.fillMaxWidth(),
            )
            BigButton(
                "Удалить",
                { requireAdmin { confirmDelete = true } },
                icon = Icons.Filled.Delete,
                style = ButtonStyle.DANGER,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showCaption) {
        TextInputDialog(
            title = "Подпись",
            initial = item.caption,
            placeholder = "Например: от друзей жениха",
            onSave = {
                vm.setCaption(item.uri, it)
                showCaption = false
            },
            onDismiss = { showCaption = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Удалить файл?",
            message = "Файл будет удалён и из галереи устройства. Это действие нельзя отменить.",
            confirmText = "Удалить",
            danger = true,
            onConfirm = {
                confirmDelete = false
                vm.delete(listOf(item)) { vm.back() }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    pinAction?.let { action ->
        PinDialog(
            title = "Действие доступно администратору",
            expectedPin = settings.pin,
            onSuccess = {
                pinAction = null
                action()
            },
            onDismiss = { pinAction = null },
        )
    }
}

