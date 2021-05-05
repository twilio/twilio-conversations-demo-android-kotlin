package com.twilio.conversations.app.manager

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.common.extensions.registerFCMToken
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.testUtil.whenCall
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    FCMManager::class,
    ConversationsClientWrapper::class,
    CredentialStorage::class,
    ConversationsClient::class
)
class FCMManagerTest {

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var context: Context
    @MockK
    private lateinit var conversationsClientWrapper: ConversationsClientWrapper
    @Mock
    private lateinit var credentialStorage: CredentialStorage

    private lateinit var conversationsClient: ConversationsClient
    private lateinit var fcmManager: FCMManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        mockkStatic("com.twilio.conversations.app.common.extensions.TwilioExtensionsKt")
        whenCall(application.applicationContext).thenReturn(context)
        conversationsClient = PowerMockito.mock(ConversationsClient::class.java)
        coEvery { conversationsClientWrapper.getConversationsClient() } returns conversationsClient
        coEvery { conversationsClientWrapper.isClientCreated } returns true

        fcmManager = FCMManagerImpl(application, conversationsClientWrapper, credentialStorage)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onNewToken - token saved in credentials if registered`() = runBlocking {
        val token = "fcm_token"
        coEvery { conversationsClient.registerFCMToken(token) } returns Unit
        fcmManager.onNewToken(token)
        verify(credentialStorage, times(1)).fcmToken = token
    }

    @Test
    fun `onNewToken - token not saved in credentials if registration failed`() = runBlocking {
        val token = "fcm_token"
        coEvery { conversationsClient.registerFCMToken(token) } throws ConversationsException(ConversationsError.TOKEN_ERROR)
        fcmManager.onNewToken(token)
        verify(credentialStorage, times(0)).fcmToken = token
    }
}
