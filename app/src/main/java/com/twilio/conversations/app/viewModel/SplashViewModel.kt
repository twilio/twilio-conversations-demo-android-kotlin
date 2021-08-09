package com.twilio.conversations.app.viewModel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.manager.LoginManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class SplashViewModel(
    private val loginManager: LoginManager,
) : ViewModel() {

    val onShowLoginScreen = SingleLiveEvent<ConversationsError>()
    val onShowSplashScreen = SingleLiveEvent<Unit>()
    val onCloseSplashScreen = SingleLiveEvent<Unit>()

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

        onShowSplashScreen.call()

        viewModelScope.launch {
            try {
                loginManager.signInUsingStoredCredentials()
                onCloseSplashScreen.call()
            } catch (e: ConversationsException) {
                delayAndShowLoginScreen(e.error)
            }
        }
    }

    private suspend fun delayAndShowLoginScreen(error: ConversationsError) {
        val elapsedTime = SystemClock.uptimeMillis() - startTime
        delay(3000 - elapsedTime) // Delay to avoid UI blinking
        onShowLoginScreen.call(error)
    }
}
