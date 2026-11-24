package com.toylek.studio.ui.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.toylek.studio.data.camera.CaptureMode
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.Screen
import com.toylek.studio.ui.components.PinDialog
import com.toylek.studio.ui.theme.Toylek

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(vm: MainViewModel, settings: AppSettings) {
    var showPin by remember { mutableStateOf(false) }
    var logoTaps by remember { mutableIntStateOf(0) }
    var firstTapAt by remember { mutableLongStateOf(0L) }
    val lowStorage = remember { vm.freeSpaceBytes() < 1024L * 1024 * 1024 }

    fun openSettings() {
        if (settings.kioskEnabled) showPin = true else vm.navigate(Screen.Settings)
    }

    val glow = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glowAlpha",
    )

    Box(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 32.dp)) {
        // Логотип: 5 нажатий или долгое нажатие — вход в настройки (защищено PIN в режиме киоска)
        Text(
            "TOYLEK STUDIO",
            style = MaterialTheme.typography.titleMedium,
            color = Toylek.Gold,
            letterSpacing = 6.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = { openSettings() },
                    onClick = {
                        val now = System.currentTimeMillis()
                        if (now - firstTapAt > 3000) {
                            firstTapAt = now
                            logoTaps = 1
                        } else {
                            logoTaps++
                            if (logoTaps >= 5) {
                                logoTaps = 0
                                openSettings()
                            }
                        }
                    },
                )
                .padding(8.dp),
        )

        if (!settings.kioskEnabled) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
                    .clickable { openSettings() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Settings, "Настройки", tint = Toylek.TextSecondary, modifier = Modifier.size(34.dp))
            }
        }

        Column(
            Modifier.fillMaxSize().padding(top = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                settings.eventTitle,
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 54.sp),
                color = Toylek.GoldLight,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            if (settings.eventSubtitle.isNotBlank()) {
                Text(
                    settings.eventSubtitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = Toylek.TextSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(40.dp))

            Row(
                Modifier.fillMaxWidth().height(320.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                HomeCard(
                    title = "Видео-\nпоздравление",
                    subtitle = "до ${settings.maxVideoSeconds / 60} мин",
                    icon = Icons.Filled.Videocam,
                    primary = true,
                    modifier = Modifier.weight(1.35f).fillMaxHeight(),
                    onClick = { vm.navigate(Screen.Camera(CaptureMode.VIDEO)) },
                )
                HomeCard(
                    title = "Фото",
                    subtitle = "сделать снимок",
                    icon = Icons.Filled.PhotoCamera,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { vm.navigate(Screen.Camera(CaptureMode.PHOTO)) },
                )
                HomeCard(
                    title = "Галерея",
                    subtitle = "фото и видео",
                    icon = Icons.Filled.PhotoLibrary,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { vm.navigate(Screen.Gallery(MediaType.PHOTO)) },
                )
                HomeCard(
                    title = "Оформление",
                    subtitle = "текст и шаблоны",
                    icon = Icons.Filled.AutoAwesome,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { vm.navigate(Screen.Gallery(MediaType.PHOTO, pickForEdit = true)) },
                )
            }

            Spacer(Modifier.height(32.dp))
            Text(
                "Нажмите на большую кнопку, чтобы оставить поздравление",
                style = MaterialTheme.typography.bodyLarge,
                color = Toylek.TextSecondary,
                modifier = Modifier.alpha(glowAlpha),
            )
            if (lowStorage) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Warning, null, tint = Toylek.Rec)
                    Spacer(Modifier.size(8.dp))
                    Text("Мало свободной памяти на устройстве", color = Toylek.Rec, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showPin) {
        PinDialog(
            title = "Введите PIN-код администратора",
            expectedPin = settings.pin,
            onSuccess = {
                showPin = false
                vm.navigate(Screen.Settings)
            },
            onDismiss = { showPin = false },
        )
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "cardScale")
    val shape = RoundedCornerShape(36.dp)
    val bg = if (primary) {
        Toylek.GoldBrush
    } else {
        Brush.linearGradient(listOf(Toylek.SurfaceHigh, Toylek.Surface))
    }
    val fg = if (primary) Color(0xFF241A08) else Toylek.TextPrimary
    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(bg)
            .border(1.dp, if (primary) Color.Transparent else Toylek.Outline, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(28.dp),
    ) {
        Box(
            Modifier
                .size(if (primary) 110.dp else 92.dp)
                .clip(CircleShape)
                .background(if (primary) Color(0x33FFFFFF) else Color(0x22D4AF6A)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = if (primary) fg else Toylek.Gold, modifier = Modifier.size(if (primary) 64.dp else 52.dp))
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(title, color = fg, style = MaterialTheme.typography.headlineMedium.copy(fontSize = if (primary) 34.sp else 28.sp, lineHeight = 38.sp))
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = fg.copy(alpha = 0.75f), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
