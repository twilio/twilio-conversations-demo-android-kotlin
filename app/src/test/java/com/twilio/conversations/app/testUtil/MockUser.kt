package com.twilio.conversations.app.testUtil

import com.twilio.conversations.User
import org.powermock.api.mockito.PowerMockito

fun createUserMock(friendlyName: String = "", identity: String = ""): User {
    val user = PowerMockito.mock(User::class.java)
    whenCall(user.identity).thenReturn(identity)
    whenCall(user.friendlyName).thenReturn(friendlyName)
    return user
}
