package com.twilio.conversations.app.viewModel

import android.app.Application
import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.R
import com.twilio.conversations.app.SPLASH_TEXT_CONNECTING
import com.twilio.conversations.app.SPLASH_TEXT_OTHER
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.ConversationsError.GENERIC_ERROR
import com.twilio.conversations.app.common.enums.ConversationsError.TOKEN_ACCESS_DENIED
import com.twilio.conversations.app.common.enums.ConversationsError.TOKEN_ERROR
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.data.models.Client
import com.twilio.conversations.app.data.models.Error
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitValue
import com.twilio.conversations.app.testUtil.whenCall
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.reset
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
    SplashViewModel::class,
    LoginManager::class,
    CredentialStorage::class,
    Client::class
)
class SplashViewModelTest {

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var splashViewModel: SplashViewModel

    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var client: Client
    @Mock
    private lateinit var loginManager: LoginManager
    @Mock
    private lateinit var credentialStorage: CredentialStorage

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)

        splashViewModel = SplashViewModel(loginManager, application)

        whenCall(application.applicationContext).thenReturn(context)
        whenCall(context.applicationContext).thenReturn(context)
        whenCall(context.resources).thenReturn(mock(Resources::class.java))
        whenCall(application.getString(R.string.splash_connecting)).thenReturn(
            SPLASH_TEXT_CONNECTING
        )
    }

    @After
    fun tearDown() {
        reset(credentialStorage)
        Dispatchers.resetMain()
    }

    @Test
    fun `Should attempt sign in when client is not already created`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager).isLoggedIn()
        verify(loginManager).signInUsingStoredCredentials(context)
        assertFalse(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should attempt sign in by calling initialize() when client is not already created`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.initialize()

        verify(loginManager).isLoggedIn()
        verify(loginManager).signInUsingStoredCredentials(context)
        assertFalse(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should not attempt sign in when client is already created`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(true)
        splashViewModel.signInOrLaunchSignInActivity()
        verify(loginManager).isLoggedIn()
        verify(loginManager, times(0)).signInUsingStoredCredentials(context)
        assertTrue(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should attempt sign in when client creation not in progress`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        splashViewModel.isProgressVisible.value = false

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager, times(1)).signInUsingStoredCredentials(context)
    }

    @Test
    fun `Should not attempt sign in when client creation in progress`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        splashViewModel.isProgressVisible.value = true

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager, times(0)).signInUsingStoredCredentials(context)
    }

    @Test
    fun `Should change visibility fields on sign in`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.signInOrLaunchSignInActivity()

        assertEquals(false, splashViewModel.isRetryVisible.waitValue())
        assertEquals(false, splashViewModel.isSignOutVisible.waitValue())
        assertEquals(true, splashViewModel.isProgressVisible.waitValue())
        assertEquals(
            SPLASH_TEXT_CONNECTING,
            splashViewModel.statusText.waitValue()
        )
    }

    @Test
    fun `Should change visibility fields when sign in response is non-fatal error (GENERIC_ERROR case)`() =
        runBlockingTest {
            val err = Error(GENERIC_ERROR)
            whenCall(loginManager.isLoggedIn()).thenReturn(false)
            whenCall(application.getString(R.string.splash_connection_error)).thenReturn(
                SPLASH_TEXT_OTHER
            )

            whenCall(loginManager.signInUsingStoredCredentials(application.applicationContext)).thenReturn(
                err
            )
            splashViewModel.signInOrLaunchSignInActivity()

            assertEquals(true, splashViewModel.isRetryVisible.waitValue())
            assertEquals(true, splashViewModel.isSignOutVisible.waitValue())
            assertEquals(false, splashViewModel.isProgressVisible.waitValue())
            assertEquals(SPLASH_TEXT_OTHER, splashViewModel.statusText.waitValue())
            assertFalse(splashViewModel.onCloseSplashScreen.waitCalled())
        }

    @Test
    fun `Should change visibility fields when sign in response is non-fatal error (TOKEN_ERROR case)`() =
        runBlockingTest {
            val err = Error(TOKEN_ERROR)
            whenCall(loginManager.isLoggedIn()).thenReturn(false)
            whenCall(application.getString(R.string.splash_connection_error)).thenReturn(
                SPLASH_TEXT_OTHER
            )

            whenCall(loginManager.signInUsingStoredCredentials(application.applicationContext)).thenReturn(
                err
            )
            splashViewModel.signInOrLaunchSignInActivity()

            assertEquals(true, splashViewModel.isRetryVisible.waitValue())
            assertEquals(true, splashViewModel.isSignOutVisible.waitValue())
            assertEquals(false, splashViewModel.isProgressVisible.waitValue())
            assertEquals(SPLASH_TEXT_OTHER, splashViewModel.statusText.waitValue())
            assertFalse(splashViewModel.onCloseSplashScreen.waitCalled())
        }

    @Test
    fun `Should call onShowLoginScreen when response is fatal error`() = runBlocking {
        val err = Error(TOKEN_ACCESS_DENIED)
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        whenCall(loginManager.signInUsingStoredCredentials(application.applicationContext)).thenReturn(
            err
        )

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onShowLoginScreen.waitCalled(5000))
    }

    @Test
    fun `Should call onShowLoginScreen when response is empty credentials error`() = runBlocking {
        val err = Error(ConversationsError.EMPTY_CREDENTIALS)
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        whenCall(loginManager.signInUsingStoredCredentials(application.applicationContext)).thenReturn(
            err
        )

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onShowLoginScreen.waitCalled(5000))
    }

    @Test
    fun `Should call onCloseSplashScreen when sign in successful`() = runBlockingTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        whenCall(loginManager.signInUsingStoredCredentials(context)).thenReturn(client)

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onCloseSplashScreen.waitCalled())
    }
}
