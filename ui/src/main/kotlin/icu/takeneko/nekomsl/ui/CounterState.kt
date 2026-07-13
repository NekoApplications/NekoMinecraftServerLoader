package icu.takeneko.nekomsl.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

internal class CounterState {
    var count by mutableIntStateOf(0)
        private set

    fun increment() {
        count++
    }
}
