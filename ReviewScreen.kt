package com.toylek.studio.ui.review

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import com.toylek.studio.domain.model.CapturedMedia
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.Screen
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.ConfirmDialog
import com.toylek.studio.ui.components.TextInputDialog
import com.toylek.studio.ui.components.VideoPlayer
import com.toylek.studio.ui.theme.Toylek

@Composable
fun ReviewScreen(vm: MainViewModel, media: CapturedMedia) {
    var showCaption by remember { mutableStateOf(false) }
    var confirmRetake by remember { mutableStateOf(false) }
    var caption by remember(media.uri) { mutableStateOf(vm.captionOf(media.uri)) }

    Row(Modifier.fillMaxSize().padding(28.dp), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (media.type == MediaType.PHOTO) {
                AsyncImage(
                    model = media.uri,
                    contentDescription = "Снятое фото",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                VideoPlayer(uri = media.uri, modifier = Modifier.fillMaxSize(), autoPlay = true, loop = true)
            }
        }

        Column(
            Modifier.width(420.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Toylek.Success, modifier = Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Text("Сохранено!", style = MaterialTheme.typography.headlineMedium, color = Toylek.TextPrimary)
            }
            Text(
                if (media.type == MediaType.PHOTO) "Фото уже в галерее устройства." else "Видео уже в галерее устройства.",
                style = MaterialTheme.typography.bodyLarge,
                color = Toylek.TextSecondary,
            )
            if (caption.isNotBlank()) {
                Text("«$caption»", style = MaterialTheme.typography.bodyLarge, color = Toylek.GoldLight)
            }
            Spacer(Modifier.height(8.dp))
            BigButton(
                text = "Добавить текст и оформление",
                icon = Icons.Filled.AutoAwesome,
                style = ButtonStyle.GOLD,
                height = 84.dp,
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.navigate(Screen.Editor(media.uri, media.type, fromGallery = false)) },
            )
            BigButton(
                text = if (caption.isBlank()) "Добавить подпись" else "Изменить подпись",
                icon = Icons.Filled.EditNote,
                modifier = Modifier.fillMaxWidth(),
                onClick = { showCaption = true },
            )
            BigButton(
                text = "Готово",
                icon = Icons.Filled.Done,
                modifier = Modifier.fillMaxWidth(),
                onClick = { vm.finishSession() },
            )
            BigButton(
                text = "Переснять",
                icon = Icons.Filled.Replay,
                style = ButtonStyle.GHOST,
                modifier = Modifier.fillMaxWidth(),
                onClick = { confirmRetake = true },
            )
        }
    }

    if (showCaption) {
        TextInputDialog(
            title = "Подпись",
            initial = caption,
            placeholder = "Например: от семьи Ахметовых",
            onSave = {
                caption = it.trim()
                vm.setCaption(media.uri, it)
                showCaption = false
            },
            onDismiss = { showCaption = false },
        )
    }
    if (confirmRetake) {
        ConfirmDialog(
            title = "Переснять?",
            message = "Текущий материал будет удалён.",
            confirmText = "Переснять",
            danger = true,
            onConfirm = {
                confirmRetake = false
                vm.retake(media)
            },
            onDismiss = { confirmRetake = false },
        )
    }
}
