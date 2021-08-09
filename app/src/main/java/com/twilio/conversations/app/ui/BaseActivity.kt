package com.twilio.conversations.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.extensions.lazyViewModel
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.ui.fragments.SplashFragment
import timber.log.Timber

private const val TAG_SPLASH_FRAGMENT = "TAG_SPLASH_FRAGMENT"

open class BaseActivity : AppCompatActivity() {

    private val splashViewModel by lazyViewModel { injector.createSplashViewModel(application) }

    private val splashFragment by lazy {
        supportFragmentManager.findFragmentByTag(TAG_SPLASH_FRAGMENT) as? SplashFragment ?: SplashFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        restoreWindowBackground()

        splashViewModel.onShowSplashScreen.observe(this) {
            Timber.d("onShowSplashScreen")
            if (!splashFragment.isAdded) {
                splashFragment.show(supportFragmentManager, TAG_SPLASH_FRAGMENT)
            }
        }

        splashViewModel.onCloseSplashScreen.observe(this) {
            Timber.d("onCloseSplashScreen")
            splashFragment.dismissAllowingStateLoss()
        }

        splashViewModel.onShowLoginScreen.observe(this) { error ->
            Timber.d("onShowLoginScreen")
            LoginActivity.start(this, error)
        }
    }

    private fun restoreWindowBackground() {
        // We've set the window background to @color/colorPrimaryDark in the main app theme
        // in order to fill window gracefully on app startup.
        // So here we return the background back to the color from base theme.

        val a = theme.obtainStyledAttributes(
            R.style.Theme_MaterialComponents_Light,
            intArrayOf(android.R.attr.windowBackground)
        )

        val colorResId = a.getResourceId(0, 0)
        window.setBackgroundDrawableResource(colorResId)
        a.recycle()
    }
}
