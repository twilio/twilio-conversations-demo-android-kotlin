package com.twilio.conversations.app.testUtil

import androidx.lifecycle.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.mockito.stubbing.OngoingStubbing
import org.powermock.api.mockito.PowerMockito

const val MAX_WAIT_TIME = 1000L

suspend fun <T> LiveData<T>.waitValue(timeout: Long = MAX_WAIT_TIME) = withTimeout(timeout) {
    val deferred = CompletableDeferred<T>()
    val observer = Observer<T> { deferred.complete(it) }
    observeForever(observer)
    val result = deferred.await()
    removeObserver(observer)
    return@withTimeout result
}

suspend fun <T> LiveData<T>.waitValue(expected: T, timeout: Long = MAX_WAIT_TIME) = null != withTimeoutOrNull(timeout) {
    val deferred = CompletableDeferred<T>()
    val observer = Observer<T> { it.takeIf { it == expected }?.let { expected -> deferred.complete(expected) } }
    observeForever(observer)
    deferred.await()
    removeObserver(observer)
}

suspend fun <T> LiveData<T>.waitCalled(timeout: Long = MAX_WAIT_TIME) = null != withTimeoutOrNull(timeout) {
    val deferred = CompletableDeferred<T>()
    val observer = Observer<T> { deferred.complete(it) }
    observeForever(observer)
    deferred.await()
    removeObserver(observer)
}

suspend fun  <T> LiveData<T>.waitNotCalled(timeout: Long = MAX_WAIT_TIME) = !waitCalled(timeout)

fun <T : Any?> whenCall(methodCall: T): OngoingStubbing<T> = PowerMockito.`when`(methodCall)
