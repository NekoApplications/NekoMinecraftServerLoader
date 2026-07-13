package icu.takeneko.nekomsl.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CounterStateTest {

    @Test
    fun incrementIncreasesCountByOne() {
        val state = CounterState()

        state.increment()

        assertEquals(1, state.count)
    }

    @Test
    fun repeatedIncrementsAccumulate() {
        val state = CounterState()

        repeat(3) {
            state.increment()
        }

        assertEquals(3, state.count)
    }
}
