package com.toylek.studio.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.theme.Toylek

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun PermissionGate(
    required: List<String>,
    optional: List<String>,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var granted by rememberSaveable { mutableStateOf(required.all { context.isGranted(it) }) }
    var asked by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        asked = true
        granted = required.all { p -> context.isGranted(p) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        granted = required.all { context.isGranted(it) }
    }

    if (granted) {
        content()
        return
    }

    val activity = context.findActivity()
    val permanentlyDenied = asked && activity != null && required.any {
        !context.isGranted(it) && !ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.widthIn(max = 900.dp).padding(40.dp),
        ) {
            Icon(Icons.Filled.VerifiedUser, null, tint = Toylek.Gold, modifier = Modifier.size(88.dp))
            Text("TOYLEK STUDIO", style = MaterialTheme.typography.displayMedium, color = Toylek.Gold)
            Text(
                "Чтобы гости могли записывать поздравления, приложению нужен доступ:",
                style = MaterialTheme.typography.titleLarge,
                color = Toylek.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                PermissionItem(Icons.Filled.PhotoCamera, "Камера", "для фото и видео")
                PermissionItem(Icons.Filled.Mic, "Микрофон", "для звука в видео")
                PermissionItem(Icons.Filled.PhotoLibrary, "Галерея", "для сохранения")
            }
            if (permanentlyDenied) {
                Text(
                    "Доступ запрещён в настройках. Откройте настройки приложения и включите разрешения «Камера» и «Микрофон».",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Toylek.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                BigButton(
                    text = "Открыть настройки",
                    icon = Icons.Filled.Settings,
                    style = ButtonStyle.GOLD,
                    height = 84.dp,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            .setData(Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    },
                )
            } else {
                BigButton(
                    text = "Разрешить доступ",
                    style = ButtonStyle.GOLD,
                    height = 84.dp,
                    onClick = { launcher.launch((required + optional).distinct().toTypedArray()) },
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(icon: ImageVector, title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(icon, null, tint = Toylek.GoldLight, modifier = Modifier.size(56.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Toylek.TextPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Toylek.TextSecondary)
    }
}
