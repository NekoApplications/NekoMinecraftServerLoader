package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

val LocalActivityManager = staticCompositionLocalOf<ActivityManager> {
    error("LocalActivityManager was read outside ActivityManager.Render()")
}

@Composable
fun RootBackHandler(behavior: RootBackBehavior) {
    val manager = LocalActivityManager.current
    val activity = manager.currentActivity

    DisposableEffect(manager, activity, behavior) {
        val token = manager.registerRootBackBehavior(activity, behavior)
        onDispose {
            manager.unregisterRootBackBehavior(activity, token)
        }
    }
}
