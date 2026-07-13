package icu.takeneko.nekomsl.ui

sealed interface RootBackBehavior {
    data object NoOp : RootBackBehavior

    data object Exit : RootBackBehavior

    data object Throw : RootBackBehavior
}
