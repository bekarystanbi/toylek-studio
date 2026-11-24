package com.toylek.studio.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.toylek.studio.data.camera.CaptureMode
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.MediaItem
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.Screen
import com.toylek.studio.ui.camera.formatTime
import com.toylek.studio.ui.components.BackPill
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.ConfirmDialog
import com.toylek.studio.ui.components.PinDialog
import com.toylek.studio.ui.theme.Toylek

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(vm: MainViewModel, settings: AppSettings, screen: Screen.Gallery) {
    val all by vm.media.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(screen.tab) }
    val items = remember(all, tab) { all.filter { it.type == tab } }
    val photoCount = remember(all) { all.count { it.type == MediaType.PHOTO } }
    val videoCount = remember(all) { all.count { it.type == MediaType.VIDEO } }

    var selectionMode by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var pinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var confirmDelete by remember { mutableStateOf<List<MediaItem>?>(null) }
    var pendingExport by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.exportToUsb(uri, pendingExport)
        else vm.message("Папка не выбрана")
    }

    fun requireAdmin(action: () -> Unit) {
        if (settings.kioskEnabled) pinAction = action else action()
    }

    fun startExport(list: List<MediaItem>) {
        if (list.isEmpty()) {
            vm.message("Нет файлов для экспорта")
            return
        }
        requireAdmin {
            pendingExport = list
            vm.message("Подключите флешку и выберите её в списке слева")
            try {
                treeLauncher.launch(null)
            } catch (e: Exception) {
                vm.message("На устройстве нет системного файлового менеджера")
            }
        }
    }

    fun exitSelection() {
        selectionMode = false
        selected = emptySet()
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 20.dp)) {
        // Верхняя панель
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BackPill(onClick = { vm.back() })
            Text(
                if (screen.pickForEdit) "Выберите, что оформить" else "Галерея",
                style = MaterialTheme.typography.headlineMedium,
                color = Toylek.TextPrimary,
            )
            Spacer(Modifier.weight(1f))
            if (!screen.pickForEdit && !selectionMode) {
                BigButton("Выбрать", { selectionMode = true }, icon = Icons.Filled.Checklist, height = 64.dp)
                BigButton("Всё на флешку", { startExport(items) }, icon = Icons.Filled.Usb, style = ButtonStyle.GOLD, height = 64.dp)
            }
        }
        Spacer(Modifier.height(16.dp))
        TabSwitch(
            tab = tab,
            photoCount = photoCount,
            videoCount = videoCount,
            onChange = {
                tab = it
                exitSelection()
            },
        )
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            EmptyGallery(
                tab = tab,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                onShoot = {
                    vm.replace(Screen.Camera(if (tab == MediaType.PHOTO) CaptureMode.PHOTO else CaptureMode.VIDEO))
                },
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(260.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(items, key = { it.uri.toString() }) { item ->
                    val key = item.uri.toString()
                    MediaTile(
                        item = item,
                        selectionMode = selectionMode,
                        isSelected = key in selected,
                        onClick = {
                            when {
                                selectionMode -> selected = if (key in selected) selected - key else selected + key
                                screen.pickForEdit -> vm.navigate(Screen.Editor(item.uri, item.type, fromGallery = true))
                                else -> vm.navigate(Screen.Viewer(item.uri, item.type))
                            }
                        },
                        onLongClick = {
                            if (!screen.pickForEdit) {
                                selectionMode = true
                                selected = selected + key
                            }
                        },
                    )
                }
            }
        }

        if (selectionMode) {
            val chosen = items.filter { it.uri.toString() in selected }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(Toylek.Surface)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Выбрано: ${chosen.size}", style = MaterialTheme.typography.titleLarge, color = Toylek.TextPrimary)
                Spacer(Modifier.weight(1f))
                BigButton("Все", { selected = items.map { it.uri.toString() }.toSet() }, icon = Icons.Filled.SelectAll, height = 64.dp)
                BigButton("На флешку", { startExport(chosen) }, icon = Icons.Filled.Usb, style = ButtonStyle.GOLD, height = 64.dp, enabled = chosen.isNotEmpty())
                BigButton(
                    "Удалить", { if (chosen.isNotEmpty()) requireAdmin { confirmDelete = chosen } },
                    icon = Icons.Filled.Delete, style = ButtonStyle.DANGER, height = 64.dp, enabled = chosen.isNotEmpty(),
                )
                BigButton("Отмена", { exitSelection() }, icon = Icons.Filled.Close, height = 64.dp)
            }
        }
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

    confirmDelete?.let { list ->
        ConfirmDialog(
            title = "Удалить файлы?",
            message = "Будет удалено: ${list.size}. Файлы исчезнут и из галереи устройства. Это действие нельзя отменить.",
            confirmText = "Удалить",
            danger = true,
            onConfirm = {
                confirmDelete = null
                vm.delete(list) { exitSelection() }
            },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
fun TabSwitch(tab: MediaType, photoCount: Int, videoCount: Int, onChange: (MediaType) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(Toylek.Surface)
            .border(1.dp, Toylek.Outline, RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            Triple(MediaType.PHOTO, "Фото ($photoCount)", Icons.Filled.PhotoCamera),
            Triple(MediaType.VIDEO, "Видео ($videoCount)", Icons.Filled.Videocam),
        ).forEach { (type, label, icon) ->
            val sel = type == tab
            Row(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(if (sel) Toylek.Gold else Color.Transparent)
                    .clickable { onChange(type) }
                    .padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon, null, tint = if (sel) Color(0xFF241A08) else Toylek.TextPrimary)
                Spacer(Modifier.width(8.dp))
                Text(label, fontSize = 20.sp, color = if (sel) Color(0xFF241A08) else Toylek.TextPrimary)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MediaTile(
    item: MediaItem,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(shape)
            .background(Toylek.SurfaceHigh)
            .border(if (isSelected) 4.dp else 1.dp, if (isSelected) Toylek.Gold else Toylek.Outline, shape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        AsyncImage(
            model = item.uri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (item.type == MediaType.VIDEO) {
            Icon(
                Icons.Filled.PlayCircle,
                null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.align(Alignment.Center).size(64.dp),
            )
            Text(
                formatTime(item.durationMs),
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xAA000000))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        if (item.caption.isNotBlank()) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000))))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(item.caption, color = Color.White, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (selectionMode) {
            Icon(
                if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                null,
                tint = if (isSelected) Toylek.Gold else Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x88000000)),
            )
        }
    }
}

@Composable
private fun EmptyGallery(tab: MediaType, modifier: Modifier, onShoot: () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Icon(Icons.Filled.PhotoLibrary, null, tint = Toylek.Outline, modifier = Modifier.size(110.dp))
            Text(
                if (tab == MediaType.PHOTO) "Фотографий пока нет" else "Видео пока нет",
                style = MaterialTheme.typography.headlineMedium,
                color = Toylek.TextSecondary,
            )
            BigButton(
                if (tab == MediaType.PHOTO) "Сделать фото" else "Записать видео",
                onShoot,
                icon = if (tab == MediaType.PHOTO) Icons.Filled.PhotoCamera else Icons.Filled.Videocam,
                style = ButtonStyle.GOLD,
            )
        }
    }
}
