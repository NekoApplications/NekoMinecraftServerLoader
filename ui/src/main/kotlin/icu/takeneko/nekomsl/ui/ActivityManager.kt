package icu.takeneko.nekomsl.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import java.util.IdentityHashMap

class ActivityManager(
    initialActivity: ComposeActivity,
    private val onExitRequest: () -> Unit = {}
) {
    private val backStack = mutableStateListOf(initialActivity)
    private val rootBackRegistrations = IdentityHashMap<ComposeActivity, RootBackRegistration>()
    private var disposed = false
    private var dispatchingLifecycle = false

    init {
        dispatchLifecycle(initialActivity::dispatchCreate)
    }

    internal val currentActivity: ComposeActivity
        get() {
            ensureActive()
            return backStack.last()
        }

    internal val backStackSize: Int
        get() {
            ensureActive()
            return backStack.size
        }

    @Composable
    fun Render() {
        ensureActive()
        CompositionLocalProvider(LocalActivityManager provides this) {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    currentActivity.Content()
                }
            }
        }
    }

    fun navigateTo(activity: ComposeActivity) {
        ensureNavigationAllowed()
        activity.checkCanCreate()
        dispatchLifecycle(currentActivity::dispatchNavigateTo)
        dispatchLifecycle(activity::dispatchCreate)
        backStack += activity
    }

    fun navigateBack(): Boolean {
        ensureNavigationAllowed()
        if (backStack.size == 1) {
            return when (rootBackRegistrations[currentActivity]?.behavior ?: RootBackBehavior.NoOp) {
                RootBackBehavior.NoOp -> false
                RootBackBehavior.Exit -> {
                    onExitRequest()
                    true
                }

                RootBackBehavior.Throw -> error(
                    "Cannot navigate back from root activity ${currentActivity.typeName}"
                )
            }
        }

        val removedActivity = backStack.removeAt(backStack.lastIndex)
        rootBackRegistrations.remove(removedActivity)
        dispatchLifecycle(removedActivity::dispatchDestroy)
        dispatchLifecycle(currentActivity::dispatchNavigatedBack)
        return true
    }

    internal fun registerRootBackBehavior(
        activity: ComposeActivity,
        behavior: RootBackBehavior
    ): Any {
        ensureActive()
        val token = Any()
        rootBackRegistrations[activity] = RootBackRegistration(token, behavior)
        return token
    }

    internal fun unregisterRootBackBehavior(activity: ComposeActivity, token: Any) {
        val registration = rootBackRegistrations[activity]
        if (registration?.token === token) {
            rootBackRegistrations.remove(activity)
        }
    }

    fun setCurrentAsRoot() {
        ensureNavigationAllowed()
        if (backStack.size == 1) {
            return
        }

        val removedActivities = backStack.dropLast(1).asReversed()
        while (backStack.size > 1) {
            backStack.removeAt(0)
        }
        removedActivities.forEach(rootBackRegistrations::remove)
        destroyActivities(removedActivities)
    }

    fun dispose() {
        if (disposed) {
            return
        }
        check(!dispatchingLifecycle) {
            "ActivityManager navigation is not allowed from a lifecycle callback"
        }

        disposed = true
        val removedActivities = backStack.asReversed().toList()
        backStack.clear()
        rootBackRegistrations.clear()
        destroyActivities(removedActivities)
    }

    private fun ensureActive() {
        check(!disposed) { "ActivityManager has been disposed" }
    }

    private fun ensureNavigationAllowed() {
        ensureActive()
        check(!dispatchingLifecycle) {
            "ActivityManager navigation is not allowed from a lifecycle callback"
        }
    }

    private inline fun <T> dispatchLifecycle(block: () -> T): T {
        check(!dispatchingLifecycle) { "Lifecycle callback dispatch is already in progress" }
        dispatchingLifecycle = true
        return try {
            block()
        } finally {
            dispatchingLifecycle = false
        }
    }

    private fun destroyActivities(activities: List<ComposeActivity>) {
        var failure: Throwable? = null
        activities.forEach { activity ->
            try {
                dispatchLifecycle(activity::dispatchDestroy)
            } catch (exception: Throwable) {
                if (failure == null) {
                    failure = exception
                } else {
                    failure.addSuppressed(exception)
                }
            }
        }
        failure?.let { throw it }
    }

    private data class RootBackRegistration(
        val token: Any,
        val behavior: RootBackBehavior
    )
}
