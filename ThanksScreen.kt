package com.toylek.studio.ui.thanks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.toylek.studio.ui.MainViewModel
import com.toylek.studio.ui.components.BigButton
import com.toylek.studio.ui.components.ButtonStyle
import com.toylek.studio.ui.theme.Toylek
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val THANKS_SECONDS = 6

@Composable
fun ThanksScreen(vm: MainViewModel) {
    var left by remember { mutableIntStateOf(THANKS_SECONDS) }
    val heart = remember { Animatable(0.4f) }

    LaunchedEffect(Unit) {
        launch { heart.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        while (left > 0) {
            delay(1000)
            left--
        }
        vm.goHome()
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Icon(Icons.Filled.Favorite, null, tint = Toylek.Gold, modifier = Modifier.size(140.dp).scale(heart.value))
            Text("Спасибо!", style = MaterialTheme.typography.displayLarge, color = Toylek.GoldLight)
            Text(
                "Ваше поздравление сохранено.\nВиновники торжества обязательно его увидят!",
                style = MaterialTheme.typography.titleLarge,
                color = Toylek.TextSecondary,
                textAlign = TextAlign.Center,
            )
            BigButton(
                text = "На главный экран ($left)",
                icon = Icons.Filled.Home,
                style = ButtonStyle.GOLD,
                height = 84.dp,
                onClick = { vm.goHome() },
            )
        }
    }
}
