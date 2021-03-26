package com.twilio.conversations.app

import androidx.lifecycle.Observer
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.data.models.ReactionAttributes
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

private fun <T> SingleLiveEvent<T>.getOrAwaitValue(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }

    this.observeForever(observer)

    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
        throw TimeoutException("LiveData value was never set.")
    }

    @Suppress("UNCHECKED_CAST")
    return data as T
}

fun <T> SingleLiveEvent<T>.verifyCalled(
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
) = assert(getOrAwaitValue(time, timeUnit) == null)

fun <T> SingleLiveEvent<T>.awaitValue(
    value: T,
    time: Long = 2,
    timeUnit: TimeUnit = TimeUnit.SECONDS
) = assert(getOrAwaitValue(time, timeUnit) == value)

fun List<Reaction>.getExpectedReactions(participantSids: List<String>): ReactionAttributes {
    val attributes: MutableMap<String, Set<String>> = mutableMapOf()
    forEach { reaction ->
        attributes[reaction.value] = participantSids.toSet()
    }
    return ReactionAttributes(attributes)
}
