package com.twilio.conversations.app.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.manager.ConnectivityMonitor
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.testUtil.INVALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.VALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitValue
import com.twilio.conversations.app.testUtil.whenCall
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.launch
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
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
@RunWith(PowerMockRunner::class)
@PrepareForTest(
        LoginViewModel::class,
        LoginManager::class,
)
class LoginViewModelTest {

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var loginViewModel: LoginViewModel

    @Mock
    private lateinit var loginManager: LoginManager

    @RelaxedMockK
    private lateinit var connectivityMonitor: ConnectivityMonitor

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Unconfined)

        loginViewModel = LoginViewModel(loginManager, connectivityMonitor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Should attempt sign in when not loading`() = runBlockingTest {
        loginViewModel.isLoading.value = false
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(loginManager, times(1)).signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `Should not attempt sign in when loading`() = runBlockingTest {
        loginViewModel.isLoading.value = true
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(loginManager, times(0)).signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `Should not attempt sign in with invalid credentials`() = runBlockingTest {
        loginViewModel.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        verify(loginManager, times(0)).signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @DelicateCoroutinesApi
    @Test
    fun `Should set isLoading to true while attempting sign in and unchanged when done`() = runBlocking {
            assertEquals(false, loginViewModel.isLoading.waitValue())

            GlobalScope.launch {
                assertEquals(true, loginViewModel.isLoading.waitValue())
            }

            loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)

            assertEquals(true, loginViewModel.isLoading.waitValue())
        }

    @Test
    fun `Should call onSignInSuccess when sign in successful`() = runBlockingTest {
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertTrue(loginViewModel.onSignInSuccess.waitCalled())
    }

    @Test
    fun `Should not call onSignInError when sign in successful`() = runBlockingTest {
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertFalse(loginViewModel.onSignInError.waitCalled())
    }

    @Test
    fun `Should call onSignInError when sign in fails`() = runBlockingTest {
        val error = ConversationsError.TOKEN_ACCESS_DENIED
        whenCall(loginManager.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)).then { throw ConversationsException(error) }
        loginViewModel.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        assertTrue(loginViewModel.onSignInError.waitCalled())
    }

    @Test
    fun `Should not call onSignInSuccess when sign in fails`() = runBlockingTest {
        val error = ConversationsError.TOKEN_ERROR
        whenCall(loginManager.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)).then { throw ConversationsException(error) }
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertFalse(loginViewModel.onSignInSuccess.waitCalled())
    }
}
