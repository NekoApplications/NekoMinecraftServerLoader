package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.Composable

abstract class ComposeActivity {
    private var lifecycleState = LifecycleState.INITIALIZED

    internal val typeName: String
        get() = this::class.qualifiedName ?: javaClass.name

    @Composable
    abstract fun Content()

    protected open fun onCreate() = Unit

    protected open fun onNavigateTo() = Unit

    protected open fun onNavigatedBack() = Unit

    protected open fun onDestroy() = Unit

    internal fun checkCanCreate() {
        check(lifecycleState == LifecycleState.INITIALIZED) {
            "$typeName cannot be created from state $lifecycleState"
        }
    }

    internal fun dispatchCreate() {
        checkCanCreate()
        lifecycleState = LifecycleState.CREATED
        onCreate()
    }

    internal fun dispatchNavigateTo() {
        check(lifecycleState == LifecycleState.CREATED) {
            "$typeName is not active"
        }
        onNavigateTo()
    }

    internal fun dispatchNavigatedBack() {
        check(lifecycleState == LifecycleState.CREATED) {
            "$typeName is not active"
        }
        onNavigatedBack()
    }

    internal fun dispatchDestroy() {
        check(lifecycleState == LifecycleState.CREATED) {
            "$typeName is not active"
        }
        lifecycleState = LifecycleState.DESTROYED
        onDestroy()
    }

    private enum class LifecycleState {
        INITIALIZED,
        CREATED,
        DESTROYED
    }
}
