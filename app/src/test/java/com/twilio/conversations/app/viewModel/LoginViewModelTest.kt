package com.twilio.conversations.app.viewModel

import android.app.Application
import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.app.data.models.Client
import com.twilio.conversations.app.data.models.Error
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.testUtil.INVALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.VALID_CREDENTIAL
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitValue
import com.twilio.conversations.app.testUtil.whenCall
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
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
        Client::class,
        Error::class
)
class LoginViewModelTest {

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var loginViewModel: LoginViewModel

    @Mock
    private lateinit var application: Application
    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var client: Client
    @Mock
    private lateinit var error: Error
    @Mock
    private lateinit var loginManager: LoginManager

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)

        loginViewModel = LoginViewModel(loginManager, application)

        whenCall(application.applicationContext).thenReturn(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Should attempt sign in when not loading`() = runBlockingTest {
        loginViewModel.isLoading.value = false
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(loginManager, times(1)).signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `Should not attempt sign in when loading`() = runBlockingTest {
        loginViewModel.isLoading.value = true
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        verify(loginManager, times(0)).signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `Should not attempt sign in with invalid credentials`() = runBlockingTest {
        loginViewModel.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        verify(loginManager, times(0)).signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)
    }

    @Test
    fun `Should set isLoading to true while attempting sign in and unchanged when done`() = runBlocking {
            whenCall(loginManager.signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(client)

            assertEquals(false, loginViewModel.isLoading.waitValue())

            GlobalScope.launch {
                assertEquals(true, loginViewModel.isLoading.waitValue())
            }

            loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)

            assertEquals(true, loginViewModel.isLoading.waitValue())
        }

    @Test
    fun `Should call onSignInSuccess when sign in successful`() = runBlockingTest {
        whenCall(loginManager.signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(client)
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertTrue(loginViewModel.onSignInSuccess.waitCalled())
    }

    @Test
    fun `Should not call onSignInError when sign in successful`() = runBlockingTest {
        whenCall(loginManager.signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(client)
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertFalse(loginViewModel.onSignInError.waitCalled())
    }

    @Test
    fun `Should call onSignInError when sign in fails`() = runBlockingTest {
        whenCall(loginManager.signIn(context, INVALID_CREDENTIAL, INVALID_CREDENTIAL)).thenReturn(error)
        loginViewModel.signIn(INVALID_CREDENTIAL, INVALID_CREDENTIAL)
        assertTrue(loginViewModel.onSignInError.waitCalled())
    }

    @Test
    fun `Should not call onSignInSuccess when sign in fails`() = runBlockingTest {
        whenCall(loginManager.signIn(context, VALID_CREDENTIAL, VALID_CREDENTIAL)).thenReturn(error)
        loginViewModel.signIn(VALID_CREDENTIAL, VALID_CREDENTIAL)
        assertFalse(loginViewModel.onSignInSuccess.waitCalled())
    }
}
