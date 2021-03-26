package com.twilio.conversations.app.testUtil.mockito

import org.mockito.Mockito
import org.mockito.stubbing.OngoingStubbing

fun <T> whenCall(methodCall: T): OngoingStubbing<T> = Mockito.`when`(methodCall)

inline fun <reified T> mock(init: T.() -> Unit = {}): T = Mockito.mock(T::class.java).apply(init)
