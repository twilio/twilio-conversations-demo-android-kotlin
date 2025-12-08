package com.twilio.conversations.app.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.createTwilioException
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.whenCall
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
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
)
class SplashViewModelTest {

    @Rule
    @JvmField
    var coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var splashViewModel: SplashViewModel

    @Mock
    private lateinit var loginManager: LoginManager
    @Mock
    private lateinit var credentialStorage: CredentialStorage

    @Before
    fun setUp() {
        splashViewModel = SplashViewModel(loginManager)
    }

    @Test
    fun `Should attempt sign in when client is not already created`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager).isLoggedIn()
        verify(loginManager).signInUsingStoredCredentials()

        assertTrue(splashViewModel.onShowSplashScreen.waitCalled())
        assertTrue(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should attempt sign in by calling initialize() when client is not already created`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.initialize()

        verify(loginManager).isLoggedIn()
        verify(loginManager).signInUsingStoredCredentials()
        assertTrue(splashViewModel.onShowSplashScreen.waitCalled())
        assertTrue(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should not attempt sign in when client is already created`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(true)
        splashViewModel.signInOrLaunchSignInActivity()
        verify(loginManager).isLoggedIn()
        verify(loginManager, times(0)).signInUsingStoredCredentials()
        assertFalse(splashViewModel.onShowSplashScreen.waitCalled())
        assertFalse(splashViewModel.onCloseSplashScreen.waitCalled())
    }

    @Test
    fun `Should attempt sign in when client creation not in progress`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager, times(1)).signInUsingStoredCredentials()
    }

    @Test
    fun `Should not attempt sign in when client creation in progress`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(true)

        splashViewModel.signInOrLaunchSignInActivity()

        verify(loginManager, times(0)).signInUsingStoredCredentials()
    }

    @Test
    fun `Should call onShowLoginScreen when  error occurred`() = runTest {
        val error = ConversationsError.TOKEN_ACCESS_DENIED
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        whenCall(loginManager.signInUsingStoredCredentials()).then { throw createTwilioException(error) }

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onShowLoginScreen.waitCalled(5000))
    }

    @Test
    fun `Should call onShowLoginScreen when response is empty credentials error`() = runTest {
        val error = ConversationsError.NO_STORED_CREDENTIALS
        whenCall(loginManager.isLoggedIn()).thenReturn(false)
        whenCall(loginManager.signInUsingStoredCredentials()).then { throw createTwilioException(error) }

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onShowLoginScreen.waitCalled(5000))
    }

    @Test
    fun `Should call onCloseSplashScreen when sign in successful`() = runTest {
        whenCall(loginManager.isLoggedIn()).thenReturn(false)

        splashViewModel.signInOrLaunchSignInActivity()

        assertTrue(splashViewModel.onShowSplashScreen.waitCalled())
        assertTrue(splashViewModel.onCloseSplashScreen.waitCalled())
    }
}
