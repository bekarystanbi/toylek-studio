package com.toylek.studio.ui.settings

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toylek.studio.BuildConfig
import com.toylek.studio.domain.model.AppSettings
import com.toylek.studio.domain.model.MediaType
import com.toylek.studio.kiosk.KioskController
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.components.BackPill
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.components.PinDialog
import com.toylek.studio.ui.components.SectionLabel
import com.toylek.studio.ui.components.TextInputDialog
import com.toylek.studio.ui.theme.Toylek

@Composable
fun SettingsScreen(vm: MainViewModel, settings: AppSettings, onExitApp: () -> Unit) {
    val context = LocalContext.current
    val media by vm.media.collectAsStateWithLifecycle()
    var editTitle by remember { mutableStateOf(false) }
    var editSubtitle by remember { mutableStateOf(false) }
    var newPinStep by remember { mutableStateOf(0) } // 0 — нет, 1 — ввод нового, 2 — повтор
    var firstPin by remember { mutableStateOf("") }
    val isOwner = remember { KioskController.isDeviceOwner(context) }
    val freeGb = remember { vm.freeSpaceBytes() / (1024.0 * 1024 * 1024) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp, vertical = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BackPill(onClick = { vm.back() })
            Text("Настройки", style = MaterialTheme.typography.headlineMedium, color = Toylek.TextPrimary)
        }
        Spacer(Modifier.height(16.dp))

        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionLabel("Мероприятие")
                ValueRow("Заголовок на главном экране", settings.eventTitle) { editTitle = true }
                ValueRow("Подзаголовок", settings.eventSubtitle.ifBlank { "—" }) { editSubtitle = true }

                SectionLabel("Съёмка")
                OptionRow(
                    title = "Максимальная длина видео",
                    options = listOf(60 to "1 мин", 120 to "2 мин", 180 to "3 мин", 300 to "5 мин"),
                    selected = settings.maxVideoSeconds,
                    onSelect = { v -> vm.updateSettings { it.copy(maxVideoSeconds = v) } },
                )
                OptionRow(
                    title = "Обратный отсчёт перед фото",
                    options = listOf(0 to "Нет", 3 to "3 с", 5 to "5 с", 10 to "10 с"),
                    selected = settings.photoCountdownSeconds,
                    onSelect = { v -> vm.updateSettings { it.copy(photoCountdownSeconds = v) } },
                )
                OptionRow(
                    title = "Камера по умолчанию",
                    options = listOf(1 to "Передняя", 0 to "Задняя"),
                    selected = if (settings.defaultFrontCamera) 1 else 0,
                    onSelect = { v -> vm.updateSettings { it.copy(defaultFrontCamera = v == 1) } },
                )

                SectionLabel("Режим мероприятия")
                SwitchRow(
                    title = "Режим киоска",
                    subtitle = "Гости не смогут выйти из приложения. Настройки и удаление — только по PIN-коду.",
                    checked = settings.kioskEnabled,
                    onChange = { v -> vm.updateSettings { it.copy(kioskEnabled = v) } },
                )
                OptionRow(
                    title = "Автовозврат на главный экран",
                    options = listOf(0 to "Выкл", 30 to "30 с", 60 to "1 мин", 90 to "1,5 мин", 180 to "3 мин", 300 to "5 мин"),
                    selected = settings.autoReturnSeconds,
                    onSelect = { v -> vm.updateSettings { it.copy(autoReturnSeconds = v) } },
                )
                SwitchRow(
                    title = "Экран «Спасибо» после съёмки",
                    subtitle = "Показывается 6 секунд, затем приложение возвращается на главный экран.",
                    checked = settings.showThanksScreen,
                    onChange = { v -> vm.updateSettings { it.copy(showThanksScreen = v) } },
                )
                BigButton(
                    "Сменить PIN-код",
                    { newPinStep = 1 },
                    icon = Icons.Filled.Lock,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (settings.pin == AppSettings.DEFAULT_PIN) {
                    Text(
                        "Сейчас установлен PIN по умолчанию: ${AppSettings.DEFAULT_PIN}. Рекомендуем сменить его перед мероприятием.",
                        color = Toylek.Rec,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            Column(
                Modifier.widthIn(max = 460.dp).weight(0.8f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionLabel("Состояние")
                InfoCard(
                    listOf(
                        "Фотографий" to "${media.count { it.type == MediaType.PHOTO }}",
                        "Видео" to "${media.count { it.type == MediaType.VIDEO }}",
                        "Свободно памяти" to "%.1f ГБ".format(freeGb),
                        "Папка в галерее" to "Pictures и Movies / TOYLEK STUDIO",
                        "Полный киоск (владелец устройства)" to if (isOwner) "включён" else "нет",
                        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                        "Версия приложения" to BuildConfig.VERSION_NAME,
                    )
                )
                if (!isOwner) {
                    Text(
                        "Для полной блокировки без подтверждений назначьте приложение владельцем устройства " +
                            "(на сброшенном планшете без аккаунтов):\n" +
                            "adb shell dpm set-device-owner com.toylek.studio/.kiosk.KioskAdminReceiver",
                        color = Toylek.TextSecondary,
                        fontSize = 15.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                BigButton(
                    "Выйти из приложения",
                    onExitApp,
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    style = ButtonStyle.DANGER,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (editTitle) {
        TextInputDialog(
            title = "Заголовок на главном экране",
            initial = settings.eventTitle,
            placeholder = "Например: Свадьба Айдоса и Айгерим",
            singleLine = true,
            maxLength = 60,
            onSave = { v ->
                vm.updateSettings { it.copy(eventTitle = v.trim().ifBlank { AppSettings().eventTitle }) }
                editTitle = false
            },
            onDismiss = { editTitle = false },
        )
    }
    if (editSubtitle) {
        TextInputDialog(
            title = "Подзаголовок",
            initial = settings.eventSubtitle,
            placeholder = "Например: 24 ноября 2026",
            singleLine = true,
            maxLength = 80,
            onSave = { v ->
                vm.updateSettings { it.copy(eventSubtitle = v.trim()) }
                editSubtitle = false
            },
            onDismiss = { editSubtitle = false },
        )
    }
    when (newPinStep) {
        1 -> PinDialog(
            title = "Новый PIN-код (4 цифры)",
            expectedPin = null,
            onSuccess = {
                firstPin = it
                newPinStep = 2
            },
            onDismiss = { newPinStep = 0 },
        )
        2 -> PinDialog(
            title = "Повторите новый PIN-код",
            expectedPin = firstPin,
            onSuccess = { pin ->
                vm.updateSettings { it.copy(pin = pin) }
                vm.message("PIN-код изменён")
                newPinStep = 0
            },
            onDismiss = { newPinStep = 0 },
        )
    }
}

@Composable
private fun ValueRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Toylek.Surface)
            .border(1.dp, Toylek.Outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Toylek.TextSecondary, fontSize = 17.sp)
            Text(value, color = Toylek.TextPrimary, fontSize = 24.sp)
        }
        androidx.compose.material3.Icon(Icons.Filled.Edit, "Изменить", tint = Toylek.Gold)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Toylek.Surface)
            .border(1.dp, Toylek.Outline, RoundedCornerShape(20.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Toylek.TextPrimary, fontSize = 22.sp)
            Text(subtitle, color = Toylek.TextSecondary, fontSize = 16.sp)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = Toylek.Gold, checkedThumbColor = Color.White),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OptionRow(title: String, options: List<Pair<Int, String>>, selected: Int, onSelect: (Int) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Toylek.Surface)
            .border(1.dp, Toylek.Outline, RoundedCornerShape(20.dp))
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, color = Toylek.TextPrimary, fontSize = 22.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { (value, label) ->
                val sel = value == selected
                Text(
                    label,
                    color = if (sel) Color(0xFF241A08) else Toylek.TextPrimary,
                    fontSize = 19.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (sel) Toylek.Gold else Toylek.SurfaceHigh)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun InfoCard(rows: List<Pair<String, String>>) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Toylek.Surface)
            .border(1.dp, Toylek.Outline, RoundedCornerShape(20.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { (k, v) ->
            Row {
                Text(k, color = Toylek.TextSecondary, fontSize = 17.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text(v, color = Toylek.TextPrimary, fontSize = 17.sp)
            }
        }
    }
}
