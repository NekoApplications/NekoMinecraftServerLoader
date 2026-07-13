package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.Composable
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ActivityManagerTest {

    @Test
    fun initialActivityIsCreatedOnce() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)

        val manager = ActivityManager(initial)

        assertSame(initial, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("initial:create"), events)
    }

    @Test
    fun navigateToDispatchesOutgoingThenCreatesTarget() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        val next = RecordingActivity("next", events)
        val manager = ActivityManager(initial)
        events.clear()

        manager.navigateTo(next)

        assertSame(next, manager.currentActivity)
        assertEquals(2, manager.backStackSize)
        assertEquals(listOf("initial:navigateTo", "next:create"), events)
    }

    @Test
    fun navigateBackDestroysCurrentThenRestoresPrevious() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        val next = RecordingActivity("next", events)
        val manager = ActivityManager(initial)
        manager.navigateTo(next)
        events.clear()

        val handled = manager.navigateBack()

        assertTrue(handled)
        assertSame(initial, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("next:destroy", "initial:navigatedBack"), events)
    }

    @Test
    fun setCurrentAsRootDestroysOlderActivitiesNearestFirst() {
        val events = mutableListOf<String>()
        val root = RecordingActivity("root", events)
        val middle = RecordingActivity("middle", events)
        val current = RecordingActivity("current", events)
        val manager = ActivityManager(root)
        manager.navigateTo(middle)
        manager.navigateTo(current)
        events.clear()

        manager.setCurrentAsRoot()

        assertSame(current, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("middle:destroy", "root:destroy"), events)
    }

    @Test
    fun disposeDestroysStackTopDownAndIsIdempotent() {
        val events = mutableListOf<String>()
        val root = RecordingActivity("root", events)
        val middle = RecordingActivity("middle", events)
        val current = RecordingActivity("current", events)
        val manager = ActivityManager(root)
        manager.navigateTo(middle)
        manager.navigateTo(current)
        events.clear()

        manager.dispose()
        manager.dispose()

        assertEquals(
            listOf("current:destroy", "middle:destroy", "root:destroy"),
            events
        )
        assertFailsWith<IllegalStateException> { manager.currentActivity }
    }

    @Test
    fun rootBackBehaviorsDoNotDispatchLifecycleEvents() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        var exitRequests = 0
        val manager = ActivityManager(initial) { exitRequests++ }
        events.clear()

        assertFalse(manager.navigateBack())

        val exitToken = manager.registerRootBackBehavior(initial, RootBackBehavior.Exit)
        assertTrue(manager.navigateBack())
        assertEquals(1, exitRequests)
        manager.unregisterRootBackBehavior(initial, exitToken)

        manager.registerRootBackBehavior(initial, RootBackBehavior.Throw)
        val exception = assertFailsWith<IllegalStateException> {
            manager.navigateBack()
        }
        assertContains(exception.message.orEmpty(), RecordingActivity::class.qualifiedName.orEmpty())
        assertEquals(emptyList(), events)
    }

    @Test
    fun throwRootBehaviorNamesAnonymousActivity() {
        val activity = object : ComposeActivity() {
            @Composable
            override fun Content() = Unit
        }
        val manager = ActivityManager(activity)
        manager.registerRootBackBehavior(activity, RootBackBehavior.Throw)

        val exception = assertFailsWith<IllegalStateException> {
            manager.navigateBack()
        }

        assertContains(exception.message.orEmpty(), activity.javaClass.name)
    }

    @Test
    fun duplicateAndDestroyedTargetsAreRejectedBeforeOutgoingCallback() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        val next = RecordingActivity("next", events)
        val manager = ActivityManager(initial)
        manager.navigateTo(next)
        manager.navigateBack()
        events.clear()

        assertFailsWith<IllegalStateException> { manager.navigateTo(initial) }
        assertEquals(emptyList(), events)

        assertFailsWith<IllegalStateException> { manager.navigateTo(next) }
        assertEquals(emptyList(), events)
    }

    @Test
    fun lifecycleCallbackCannotReenterNavigation() {
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        lateinit var manager: ActivityManager
        val next = RecordingActivity(
            name = "next",
            events = events,
            createAction = { manager.navigateBack() }
        )
        manager = ActivityManager(initial)
        events.clear()

        val exception = assertFailsWith<IllegalStateException> {
            manager.navigateTo(next)
        }

        assertContains(exception.message.orEmpty(), "lifecycle callback")
        assertSame(initial, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
    }

    @Test
    fun navigateToPropagatesOutgoingFailureWithoutChangingStack() {
        val failure = IllegalStateException("navigateTo failed")
        val events = mutableListOf<String>()
        val initial = RecordingActivity(
            name = "initial",
            events = events,
            navigateToAction = { throw failure }
        )
        val target = RecordingActivity("target", events)
        val manager = ActivityManager(initial)
        events.clear()

        val thrown = assertFailsWith<IllegalStateException> {
            manager.navigateTo(target)
        }

        assertSame(failure, thrown)
        assertSame(initial, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("initial:navigateTo"), events)
        assertSame(target, ActivityManager(target).currentActivity)
    }

    @Test
    fun navigateToPropagatesCreateFailureAndDoesNotPushTarget() {
        val failure = IllegalStateException("create failed")
        val events = mutableListOf<String>()
        val initial = RecordingActivity("initial", events)
        val target = RecordingActivity(
            name = "target",
            events = events,
            createAction = { throw failure }
        )
        val manager = ActivityManager(initial)
        events.clear()

        val thrown = assertFailsWith<IllegalStateException> {
            manager.navigateTo(target)
        }

        assertSame(failure, thrown)
        assertSame(initial, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("initial:navigateTo", "target:create"), events)
        assertFailsWith<IllegalStateException> { ActivityManager(target) }
    }

    @Test
    fun navigateBackKeepsCommittedPopWhenCallbacksFail() {
        val destroyFailure = IllegalStateException("destroy failed")
        val destroyEvents = mutableListOf<String>()
        val destroyRoot = RecordingActivity("root", destroyEvents)
        val destroyTarget = RecordingActivity(
            name = "target",
            events = destroyEvents,
            destroyAction = { throw destroyFailure }
        )
        val destroyManager = ActivityManager(destroyRoot)
        destroyManager.navigateTo(destroyTarget)
        destroyEvents.clear()

        assertSame(
            destroyFailure,
            assertFailsWith<IllegalStateException> { destroyManager.navigateBack() }
        )
        assertSame(destroyRoot, destroyManager.currentActivity)
        assertEquals(1, destroyManager.backStackSize)
        assertEquals(listOf("target:destroy"), destroyEvents)

        val restoreFailure = IllegalStateException("restore failed")
        val restoreEvents = mutableListOf<String>()
        val restoreRoot = RecordingActivity(
            name = "root",
            events = restoreEvents,
            navigatedBackAction = { throw restoreFailure }
        )
        val restoreTarget = RecordingActivity("target", restoreEvents)
        val restoreManager = ActivityManager(restoreRoot)
        restoreManager.navigateTo(restoreTarget)
        restoreEvents.clear()

        assertSame(
            restoreFailure,
            assertFailsWith<IllegalStateException> { restoreManager.navigateBack() }
        )
        assertSame(restoreRoot, restoreManager.currentActivity)
        assertEquals(1, restoreManager.backStackSize)
        assertEquals(listOf("target:destroy", "root:navigatedBack"), restoreEvents)
    }

    @Test
    fun setCurrentAsRootContinuesDestroyingAfterCallbackFailure() {
        val failure = IllegalStateException("middle destroy failed")
        val events = mutableListOf<String>()
        val root = RecordingActivity("root", events)
        val middle = RecordingActivity(
            name = "middle",
            events = events,
            destroyAction = { throw failure }
        )
        val current = RecordingActivity("current", events)
        val manager = ActivityManager(root)
        manager.navigateTo(middle)
        manager.navigateTo(current)
        events.clear()

        val thrown = assertFailsWith<IllegalStateException> {
            manager.setCurrentAsRoot()
        }

        assertSame(failure, thrown)
        assertSame(current, manager.currentActivity)
        assertEquals(1, manager.backStackSize)
        assertEquals(listOf("middle:destroy", "root:destroy"), events)
    }

    @Test
    fun disposedManagerRejectsFurtherNavigation() {
        val manager = ActivityManager(RecordingActivity("initial", mutableListOf()))
        manager.dispose()

        assertFailsWith<IllegalStateException> { manager.navigateBack() }
        assertFailsWith<IllegalStateException> {
            manager.navigateTo(RecordingActivity("next", mutableListOf()))
        }
        assertFailsWith<IllegalStateException> { manager.setCurrentAsRoot() }
    }

    @Test
    fun batchDestroyContinuesAndPropagatesCallbackFailures() {
        val events = mutableListOf<String>()
        val firstFailure = IllegalStateException("current destroy failed")
        val secondFailure = IllegalArgumentException("middle destroy failed")
        val root = RecordingActivity("root", events)
        val middle = RecordingActivity(
            name = "middle",
            events = events,
            destroyAction = { throw secondFailure }
        )
        val current = RecordingActivity(
            name = "current",
            events = events,
            destroyAction = { throw firstFailure }
        )
        val manager = ActivityManager(root)
        manager.navigateTo(middle)
        manager.navigateTo(current)
        events.clear()

        val thrown = assertFailsWith<IllegalStateException> { manager.dispose() }

        assertSame(firstFailure, thrown)
        assertEquals(listOf(secondFailure), thrown.suppressed.toList())
        assertEquals(
            listOf("current:destroy", "middle:destroy", "root:destroy"),
            events
        )
    }

    private class RecordingActivity(
        private val name: String,
        private val events: MutableList<String>,
        private val createAction: () -> Unit = {},
        private val navigateToAction: () -> Unit = {},
        private val navigatedBackAction: () -> Unit = {},
        private val destroyAction: () -> Unit = {}
    ) : ComposeActivity() {

        @Composable
        override fun Content() = Unit

        override fun onCreate() {
            events += "$name:create"
            createAction()
        }

        override fun onNavigateTo() {
            events += "$name:navigateTo"
            navigateToAction()
        }

        override fun onNavigatedBack() {
            events += "$name:navigatedBack"
            navigatedBackAction()
        }

        override fun onDestroy() {
            events += "$name:destroy"
            destroyAction()
        }
    }
}
