package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val manager = remember {
        ActivityManager(CounterActivity(), onExitRequest = ::exitApplication)
    }
    DisposableEffect(manager) {
        onDispose {
            manager.dispose()
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Neko Minecraft Server Loader",
        state = rememberWindowState(width = 360.dp, height = 220.dp)
    ) {
        manager.Render()
    }
}
