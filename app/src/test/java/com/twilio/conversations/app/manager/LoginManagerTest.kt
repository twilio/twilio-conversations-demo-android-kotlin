package com.twilio.conversations.app.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.common.FirebaseTokenRetriever
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.data.models.Client
import com.twilio.conversations.app.data.models.Error
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.*
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    LoginManager::class,
    ConversationsClientWrapper::class,
    CredentialStorage::class,
    ConversationsRepository::class,
    Client::class,
    Error::class
)
class LoginManagerTest {
    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    private lateinit var loginManager: LoginManager
    private lateinit var error: Error

    @MockK
    private lateinit var firebaseTokenRetriever: FirebaseTokenRetriever
    @Mock
    private lateinit var client: Client
    @Mock
    private lateinit var conversationsClientWrapper: ConversationsClientWrapper
    @Mock
    private lateinit var credentialStorage: CredentialStorage
    @Mock
    private lateinit var conversationsRepository: ConversationsRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(mainThreadSurrogate)

        loginManager = LoginManagerImpl(conversationsClientWrapper, conversationsRepository,
            credentialStorage, firebaseTokenRetriever)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `signIn() should attempt sign in`() = runBlockingTest {
        loginManager.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(conversationsClientWrapper, times(1)).create(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should attempt sign in`() = runBlockingTest {
        credentialStorageNotEmpty(credentialStorage, VALID_CREDENTIAL)
        loginManager.signInUsingStoredCredentials()
        verify(conversationsClientWrapper, times(1)).create(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should not attempt sign in when credential storage is empty`() = runBlockingTest {
        credentialStorageEmpty(credentialStorage)
        loginManager.signInUsingStoredCredentials()
        verify(conversationsClientWrapper, times(0)).create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
    }

    @Test
    fun `signIn() should attempt to store credentials when response is Client`() = runBlockingTest {
        whenCall(conversationsClientWrapper.create(VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(client)
        loginManager.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(credentialStorage, times(1)).storeCredentials(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signIn() should not attempt to clear credentials when response is fatal error`() = runBlockingTest {
        error = Error(ConversationsError.TOKEN_ACCESS_DENIED)
        whenCall(conversationsClientWrapper.create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)).thenReturn(error)
        loginManager.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        verify(credentialStorage, times(0)).clearCredentials()
    }

    @Test
    fun `signIn() should not attempt to store credentials when response is not Client`() = runBlockingTest {
        error = Error(ConversationsError.GENERIC_ERROR)
        whenCall(conversationsClientWrapper.create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)).thenReturn(error)
        loginManager.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        verify(credentialStorage, times(0)).storeCredentials(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should not attempt to store credentials`() = runBlockingTest {
        whenCall(conversationsClientWrapper.create(VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(client)
        loginManager.signInUsingStoredCredentials()
        verify(credentialStorage, times(0)).storeCredentials(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should attempt to clear credentials when response is fatal error`() = runBlockingTest {
        credentialStorageNotEmpty(credentialStorage, OUTDATED_CREDENTIAL)
        error = Error(ConversationsError.TOKEN_ACCESS_DENIED)
        whenCall(conversationsClientWrapper.create(OUTDATED_CREDENTIAL, OUTDATED_CREDENTIAL)).thenReturn(error)
        loginManager.signInUsingStoredCredentials()
        verify(credentialStorage, times(1)).clearCredentials()
    }

    @Test
    fun `signOut should clear credentials`() = runBlockingTest {
        credentialStorageNotEmpty(credentialStorage, OUTDATED_CREDENTIAL)
        loginManager.signOut()
        verify(credentialStorage, times(1)).clearCredentials()
        verify(conversationsRepository, times(1)).clear()
        verify(conversationsClientWrapper, times(1)).shutdown()
    }
}
