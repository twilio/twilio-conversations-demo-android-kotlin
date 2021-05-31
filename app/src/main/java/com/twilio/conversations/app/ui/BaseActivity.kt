package com.twilio.conversations.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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

        splashViewModel.onShowLoginScreen.observe(this) {
            Timber.d("onShowLoginScreen")
            LoginActivity.start(this)
        }
    }
}
