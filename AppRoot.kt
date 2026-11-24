package com.toylek.studio.ui

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.toylek.studio.ui.camera.CameraScreen
import com.toylek.studio.ui.components.ProgressDialog
import com.toylek.studio.ui.editor.EditorScreen
import com.toylek.studio.ui.gallery.GalleryScreen
import com.toylek.studio.ui.gallery.ViewerScreen
import com.toylek.studio.ui.home.HomeScreen
import com.toylek.studio.ui.permissions.PermissionGate
import com.toylek.studio.ui.review.ReviewScreen
import com.toylek.studio.ui.settings.SettingsScreen
import com.toylek.studio.ui.theme.Toylek
import com.toylek.studio.ui.thanks.ThanksScreen

/** Разрешения, без которых приложение не работает. */
fun requiredPermissions(): List<String> = buildList {
    add(Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}

/** Желательные разрешения (без микрофона видео пишется без звука). */
fun optionalPermissions(): List<String> = listOf(Manifest.permission.RECORD_AUDIO)

@Composable
fun AppRoot(vm: MainViewModel, onExitApp: () -> Unit) {
    val stack by vm.stack.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val progress by vm.progress.collectAsStateWithLifecycle()
    val pendingDelete by vm.pendingDelete.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        vm.onDeleteConfirmationResult(result.resultCode == Activity.RESULT_OK)
    }
    LaunchedEffect(pendingDelete) {
        val p = pendingDelete ?: return@LaunchedEffect
        try {
            deleteLauncher.launch(IntentSenderRequest.Builder(p.intentSender).build())
        } catch (e: Exception) {
            vm.onDeleteConfirmationResult(false)
        }
    }

    val current = stack.last()

    // Системная кнопка «Назад»: в режиме киоска с главного экрана выйти нельзя.
    BackHandler(enabled = stack.size > 1 || settings.kioskEnabled) {
        if (stack.size > 1) vm.back()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Toylek.BackgroundBrush)
    ) {
        PermissionGate(required = requiredPermissions(), optional = optionalPermissions()) {
            AnimatedContent(
                targetState = current,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                contentKey = { it::class },
                label = "screens",
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(vm, settings)
                    is Screen.Camera -> CameraScreen(vm, settings, screen.mode)
                    is Screen.Review -> ReviewScreen(vm, screen.media)
                    is Screen.Editor -> EditorScreen(vm, screen)
                    is Screen.Gallery -> GalleryScreen(vm, settings, screen)
                    is Screen.Viewer -> ViewerScreen(vm, settings, screen)
                    Screen.Settings -> SettingsScreen(vm, settings, onExitApp)
                    Screen.Thanks -> ThanksScreen(vm)
                }
            }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        ) { data ->
            Snackbar(
                modifier = Modifier.widthIn(max = 760.dp),
                shape = RoundedCornerShape(20.dp),
                containerColor = Toylek.SurfaceHigh,
                contentColor = Toylek.TextPrimary,
            ) {
                Text(data.visuals.message, fontSize = 20.sp)
            }
        }

        progress?.let { ProgressDialog(it.title, it.percent) }
    }
}
