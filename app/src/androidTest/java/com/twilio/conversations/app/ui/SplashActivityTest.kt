package com.twilio.conversations.app.ui

import android.content.Intent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.testUtil.setInvalidCredentials
import com.twilio.conversations.app.testUtil.setValidCredentials
import com.twilio.conversations.app.testUtil.verifyActivityVisible
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SplashActivityTest {

    @get:Rule
    var activityRule = IntentsTestRule(SplashActivity::class.java, true, false)

    private lateinit var credentialStorage: CredentialStorage

    @Before
    fun setUp() {
        ConversationsClientWrapper.recreateInstance()
        credentialStorage = CredentialStorage(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    @After
    fun tearDown() {
        if(this::credentialStorage.isInitialized) {
            credentialStorage.clearCredentials()
        }
    }

    @Test
    fun successfulSignin_sendsIntentToStartConversationsListActivity() {
        // Given valid user credentials are provided
        setValidCredentials(credentialStorage)

        // When app is opened
        activityRule.launchActivity(Intent())

        // Then Conversation List Activity is launched
        verifyActivityVisible<ConversationListActivity>()
    }

    @Test
    fun signinWithInvalidCredentials_sendsIntentToStartLoginActivity() {
        // Given invalid user credentials are provided
        setInvalidCredentials(credentialStorage)

        // When app is opened
        activityRule.launchActivity(Intent())

        // Then Login Activity is launched
        verifyActivityVisible<LoginActivity>()
    }
}
