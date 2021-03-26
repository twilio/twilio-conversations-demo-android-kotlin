package com.twilio.conversations.app.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.ConversationsError.TOKEN_ACCESS_DENIED
import com.twilio.conversations.app.common.extensions.lazyViewModel
import com.twilio.conversations.app.common.extensions.showSnackbar
import com.twilio.conversations.app.common.injector
import kotlinx.android.synthetic.main.activity_splash_screen.*
import timber.log.Timber

class SplashActivity : AppCompatActivity() {

    private val splashViewModel by lazyViewModel { injector.createSplashViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)

        splashViewModel.onDisplayError.observe(this, { error ->
            displayError(error)
        })

        splashViewModel.onCloseSplashScreen.observe(this, {
            Timber.d("onCloseSplashScreen")
            closeSplashScreen()
        })

        splashViewModel.onShowLoginScreen.observe(this, {
            goToLoginScreen()
        })

        splashViewModel.statusText.observe(this, { statusText ->
            statusTextTv.text = statusText
        })

        splashViewModel.isRetryVisible.observe(this, { isVisible ->
            retryBtn.isEnabled = isVisible
        })

        splashViewModel.isSignOutVisible.observe(this, { isVisible ->
            signOutBtn.isEnabled = isVisible
        })

        splashViewModel.isProgressVisible.observe(this, { isVisible ->
            changeLoading(isVisible)
        })

        retryBtn.setOnClickListener { retryPressed() }

        signOutBtn.setOnClickListener { signOutPressed() }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    override fun onBackPressed() {
        // We don't want to allow user to move to next activity in backstack until SplashActivity finishes
        moveTaskToBack(true)
    }

    private fun retryPressed() {
        Timber.d("retryPressed")
        splashViewModel.signInOrLaunchSignInActivity()
    }

    private fun signOutPressed() {
        Timber.d("signOutPressed")
        splashViewModel.signOut()
    }

    private fun changeLoading(isLoading: Boolean) {
        Timber.d("changeLoading")
        if (isLoading) {
            startLoading()
        } else {
            stopLoading()
        }
    }

    private fun startLoading() {
        Timber.d("startLoading")
        splashProgressBar.visibility = View.VISIBLE
    }

    private fun stopLoading() {
        Timber.d("stopLoading")
        splashProgressBar.visibility = View.GONE
    }

    private fun displayError(error: ConversationsError) {
        val message = when (error) {
            TOKEN_ACCESS_DENIED -> getString(R.string.splash_invalid_credentials)
            else -> getString(R.string.sign_in_error)
        }
        splashCoordinatorLayout.showSnackbar(message)
    }

    private fun closeSplashScreen() {
        if (isTaskRoot) {
            goToConversationListScreen()
            return
        }

        // Just finish in order to show next activity in the activity stack
        Timber.d("finish")
        finish()
    }

    private fun goToConversationListScreen() {
        Timber.d("goToConversationListScreen")
        ConversationListActivity.start(this)
    }

    private fun goToLoginScreen() {
        Timber.d("goToLoginScreen")
        LoginActivity.start(this)
    }
}
