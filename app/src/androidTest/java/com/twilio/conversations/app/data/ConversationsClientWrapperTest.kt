package com.twilio.conversations.app.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.twilio.conversations.app.INVALID_IDENTITY
import com.twilio.conversations.app.INVALID_PASSWORD
import com.twilio.conversations.app.common.extensions.ConversationsException
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationsClientWrapperTest {

    private lateinit var context: Context
    private lateinit var conversationsClientWrapper: ConversationsClientWrapper

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext

        ConversationsClientWrapper.recreateInstance(context)
        conversationsClientWrapper = ConversationsClientWrapper.INSTANCE
    }

    @Test(expected = ConversationsException::class)
    fun create_withInvalidCredentials_returnsError() = runBlocking {
        conversationsClientWrapper.create(INVALID_IDENTITY, INVALID_PASSWORD)
    }
}
