package com.twilio.conversations.app.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.common.FirebaseTokenManager
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.INVALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.OUTDATED_CREDENTIAL
import com.twilio.conversations.app.testUtil.VALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.credentialStorageEmpty
import com.twilio.conversations.app.testUtil.credentialStorageNotEmpty
import com.twilio.conversations.app.testUtil.whenCall
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.CompletableDeferred
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
)
class LoginManagerTest {
    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val mainThreadSurrogate = newSingleThreadContext("UI thread")

    private lateinit var loginManager: LoginManager

    @MockK
    private lateinit var firebaseTokenManager: FirebaseTokenManager
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
            credentialStorage, firebaseTokenManager)

        coEvery { firebaseTokenManager.deleteToken() } returns CompletableDeferred(true)
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
        runCatching { loginManager.signInUsingStoredCredentials() }
        verify(conversationsClientWrapper, times(0)).create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
    }

    @Test
    fun `signIn() should attempt to store credentials when client is created`() = runBlockingTest {
        loginManager.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(credentialStorage, times(1)).storeCredentials(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signIn() should not attempt to clear credentials when fatal error occurred`() = runBlockingTest {
        val error = ConversationsError.TOKEN_ACCESS_DENIED
        whenCall(conversationsClientWrapper.create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)).then { throw ConversationsException(error) }
        runCatching { loginManager.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL) }
        verify(credentialStorage, times(0)).clearCredentials()
    }

    @Test
    fun `signIn() should not attempt to store credentials when error occurred`() = runBlockingTest {
        val error = ConversationsError.GENERIC_ERROR
        whenCall(conversationsClientWrapper.create(INVALID_CREDENTIAL, INVALID_CREDENTIAL)).then { throw ConversationsException(error) }
        runCatching { loginManager.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL) }
        verify(credentialStorage, times(0)).storeCredentials(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should not attempt to store credentials`() = runBlockingTest {
        loginManager.signInUsingStoredCredentials()
        verify(credentialStorage, times(0)).storeCredentials(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `signInUsingStoredCredentials() should attempt to clear credentials when fatal error occurred`() = runBlockingTest {
        credentialStorageNotEmpty(credentialStorage, OUTDATED_CREDENTIAL)
        val error = ConversationsError.TOKEN_ACCESS_DENIED
        whenCall(conversationsClientWrapper.create(OUTDATED_CREDENTIAL, OUTDATED_CREDENTIAL)).then { throw ConversationsException(error) }
        runCatching { loginManager.signInUsingStoredCredentials() }
        verify(credentialStorage, times(1)).clearCredentials()
    }

    @Test
    fun `signOut should clear credentials`() = runBlockingTest {
        credentialStorageNotEmpty(credentialStorage, OUTDATED_CREDENTIAL)
        loginManager.signOut()

        coVerify { firebaseTokenManager.deleteToken() }
        verify(credentialStorage, times(1)).clearCredentials()
        verify(conversationsRepository, times(1)).clear()
        verify(conversationsClientWrapper, times(1)).shutdown()
    }
}
