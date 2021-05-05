package com.twilio.conversations.app.viewModel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.ConversationsError.EMPTY_CREDENTIALS
import com.twilio.conversations.app.common.enums.ConversationsError.TOKEN_ACCESS_DENIED
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.manager.LoginManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SplashViewModel(
    private val loginManager: LoginManager,
    private val application: Application
) : ViewModel() {

    val onDisplayError = SingleLiveEvent<ConversationsError>()
    val onShowLoginScreen = SingleLiveEvent<Unit>()
    val onShowSplashScreen = SingleLiveEvent<Unit>()
    val onCloseSplashScreen = SingleLiveEvent<Unit>()

    val statusText = MutableLiveData<String>()
    val isRetryVisible = MutableLiveData<Boolean>()
    val isSignOutVisible = MutableLiveData<Boolean>()
    val isProgressVisible = MutableLiveData<Boolean>()

    private val startTime = SystemClock.uptimeMillis()

    init {
        Timber.d("init view model ${this.hashCode()}")
    }

    fun initialize() {
        Timber.d("initialize")
        signInOrLaunchSignInActivity()
    }

    fun signInOrLaunchSignInActivity() {
        Timber.d("signInOrLaunchSignInActivity")
        if (loginManager.isLoggedIn()) {
            Timber.d("client already created")
            return
        }
        Timber.d("client not created")

        if (isProgressVisible.value == true) return

        statusText.value = application.getString(R.string.splash_connecting)
        isRetryVisible.value = false
        isSignOutVisible.value = false
        isProgressVisible.value = true
        onShowSplashScreen.call()

        viewModelScope.launch {
            try {
                loginManager.signInUsingStoredCredentials()
                onCloseSplashScreen.call()
            } catch (e: ConversationsException) {
                handleError(e.error)
            }
        }
    }

    private suspend fun handleError(error: ConversationsError) {
        Timber.d("handleError")
        when (error) {
            TOKEN_ACCESS_DENIED -> { // Fatal error
                Timber.d("handleError - fatal error")
                delayAndShowLoginScreen()
            }
            EMPTY_CREDENTIALS -> { // Fatal error
                Timber.d("handleError - empty credentials")
                delayAndShowLoginScreen()
            }
            else -> { // Non-fatal error
                Timber.d("handleError - connection error")
                isRetryVisible.value = true
                isSignOutVisible.value = true
                isProgressVisible.value = false
                statusText.value = application.getString(R.string.splash_connection_error)
                onDisplayError.value = error
            }
        }
    }

    private suspend fun delayAndShowLoginScreen() {
        val elapsedTime = SystemClock.uptimeMillis() - startTime
        delay(1000 - elapsedTime) // Delay to avoid UI blinking
        onShowLoginScreen.call()
    }

    fun signOut() {
        Timber.d("signOut")
        loginManager.clearCredentials()
        onShowLoginScreen.call()
    }
}
