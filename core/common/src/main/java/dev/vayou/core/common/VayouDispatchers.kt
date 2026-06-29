package dev.vayou.core.common

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val niaDispatcher: VayouDispatchers)

enum class VayouDispatchers {
    Default,
    IO,
}
