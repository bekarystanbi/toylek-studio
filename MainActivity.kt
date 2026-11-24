package com.toylek.studio

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.toylek.studio.kiosk.KioskController
import com.toylek.studio.ui.AppRoot
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.components.LocalInteraction
import com.toylek.studio.ui.theme.ToylekTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container get() = (application as ToylekApp).container
    private val vm: MainViewModel by viewModels { MainViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            ToylekTheme {
                CompositionLocalProvider(LocalInteraction provides { vm.touch() }) {
                    AppRoot(vm = vm, onExitApp = ::exitApp)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.settings.map { it.kioskEnabled }.distinctUntilChanged().collect { enabled ->
                    if (enabled) KioskController.enter(this@MainActivity) else KioskController.exit(this@MainActivity)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemBars()
        vm.refreshMedia()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        vm.touch()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun exitApp() {
        KioskController.exit(this)
        finishAndRemoveTask()
    }
}
