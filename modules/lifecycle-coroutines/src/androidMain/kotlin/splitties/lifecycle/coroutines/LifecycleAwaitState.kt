/*
 * Copyright 2019 Louis Cognault Ayeva Derman. Use of this source code is governed by the Apache 2.0 license.
 */
package splitties.lifecycle.coroutines

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.*
import splitties.experimental.ExperimentalSplittiesApi
import kotlin.coroutines.resume

/** Returns as soon as this [Lifecycle] is in the resumed state. */
@ExperimentalSplittiesApi
suspend inline fun Lifecycle.awaitResumed() = awaitState(Lifecycle.State.RESUMED)

/** Returns as soon as this [Lifecycle] is at least in the started state. */
@ExperimentalSplittiesApi
suspend inline fun Lifecycle.awaitStarted() = awaitState(Lifecycle.State.STARTED)

/**
 * Returns as soon as this [Lifecycle] is at least in the created state.
 *
 * Can be useful for use in the init blocks or constructors of a [LifecycleOwner].
 */
@ExperimentalSplittiesApi
suspend inline fun Lifecycle.awaitCreated() = awaitState(Lifecycle.State.CREATED)

/**
 * This function returns/resumes as soon as the state of this [Lifecycle] is at least the
 * passed [state].
 *
 * [Lifecycle.State.DESTROYED] is forbidden, to avoid leaks.
 *
 * See also [awaitResumed], [awaitStarted] and [awaitCreated].
 */
@ExperimentalSplittiesApi
suspend fun Lifecycle.awaitState(state: Lifecycle.State) {
    require(state != Lifecycle.State.DESTROYED) {
        "DESTROYED is a terminal state that is forbidden for awaitState(…), to avoid leaks."
    }
    if (currentState >= state) return // Fast path
    withContext(Dispatchers.Main.immediate) {
        if (currentState == Lifecycle.State.DESTROYED) { // Fast path to cancellation
            cancel()
        } else suspendCancellableCoroutine<Unit> { c ->
            val observer = object : LifecycleEventObserver {
                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (currentState >= state) {
                        removeObserver(this)
                        c.resume(Unit)
                    } else if (currentState == Lifecycle.State.DESTROYED) {
                        c.cancel()
                    }
                }
            }
            addObserver(observer)
            c.invokeOnCancellation { removeObserver(observer) }
        }
    }
}
