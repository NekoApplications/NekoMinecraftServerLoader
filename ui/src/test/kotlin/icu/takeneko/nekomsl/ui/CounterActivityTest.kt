package icu.takeneko.nekomsl.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class CounterActivityTest {

    @Test
    fun incrementIncreasesCountByOne() {
        val state = CounterActivity()

        state.increment()

        assertEquals(1, state.count)
    }

    @Test
    fun repeatedIncrementsAccumulate() {
        val state = CounterActivity()

        repeat(3) {
            state.increment()
        }

        assertEquals(3, state.count)
    }
}
