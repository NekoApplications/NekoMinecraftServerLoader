package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ActivityCompositionTest {

    @Test
    fun localActivityManagerFailsClearlyOutsideProvider() {
        val failure = assertFailsWith<IllegalStateException> {
            runDesktopComposeUiTest {
                setContent {
                    LocalActivityManager.current
                }
                waitForIdle()
            }
        }

        assertContains(failure.message.orEmpty(), "ActivityManager")
    }

    @Test
    fun renderProvidesManagerToActivityContent() = runDesktopComposeUiTest {
        var providedManager: ActivityManager? = null
        val activity = TestActivity {
            providedManager = LocalActivityManager.current
        }
        val manager = ActivityManager(activity)

        setContent { manager.Render() }
        waitForIdle()

        assertSame(manager, providedManager)
    }

    @Test
    fun rootBackHandlerUnregistersAndRegistersWithComposition() = runDesktopComposeUiTest {
        val showHandler = mutableStateOf(true)
        var exitRequests = 0
        val activity = TestActivity {
            if (showHandler.value) {
                RootBackHandler(RootBackBehavior.Exit)
            }
        }
        val manager = ActivityManager(activity) { exitRequests++ }

        setContent { manager.Render() }
        waitForIdle()

        runOnIdle { showHandler.value = false }
        waitForIdle()
        var handled = true
        runOnIdle { handled = manager.navigateBack() }
        assertFalse(handled)

        runOnIdle { showHandler.value = true }
        waitForIdle()
        runOnIdle { handled = manager.navigateBack() }
        assertTrue(handled)
        assertEquals(1, exitRequests)
    }

    @Test
    fun rootBackHandlerRegistersAgainAfterActivityNavigation() = runDesktopComposeUiTest {
        var renderedActivity = ""
        var exitRequests = 0
        val root = TestActivity {
            renderedActivity = "root"
            RootBackHandler(RootBackBehavior.Exit)
        }
        val next = TestActivity {
            renderedActivity = "next"
        }
        val manager = ActivityManager(root) { exitRequests++ }

        setContent { manager.Render() }
        waitForIdle()
        assertEquals("root", renderedActivity)

        runOnIdle { manager.navigateTo(next) }
        waitForIdle()
        assertEquals("next", renderedActivity)

        runOnIdle { manager.navigateBack() }
        waitForIdle()
        assertEquals("root", renderedActivity)

        var handled = false
        runOnIdle { handled = manager.navigateBack() }
        assertTrue(handled)
        assertEquals(1, exitRequests)
    }

    private class TestActivity(
        private val content: @Composable () -> Unit = {}
    ) : ComposeActivity() {
        @Composable
        override fun Content() = content()
    }
}
