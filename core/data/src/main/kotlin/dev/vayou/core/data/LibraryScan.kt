package dev.vayou.core.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether anything has looked at the device's videos yet.
 *
 * The library is kept in a table, and a table nobody has filled reads exactly like a phone with no
 * films on it -- an empty list, either way. A screen has to tell those apart, because one of them
 * is "there is nothing here" and the other is "I have not looked", and only one of the two is
 * honest to put on screen. The table itself cannot say which it is holding.
 *
 * It lives in this module because the two sides that need it cannot see each other: the scan runs
 * in the media module, the question is asked in the domain module, and this is what both depend on.
 *
 * Deliberately forgotten when the app closes. What a screen actually needs to know is whether the
 * rows in front of it are an answer, and from the second launch onward they always are -- the table
 * outlives the process, so there are films to draw before any scan has said a word.
 */
@Singleton
class LibraryScan @Inject constructor() {

    private val _hasRun = MutableStateFlow(false)

    val hasRun: StateFlow<Boolean> = _hasRun.asStateFlow()

    /** Said by whoever has finished writing down what a pass over the device's videos found. */
    fun record() {
        _hasRun.value = true
    }
}
