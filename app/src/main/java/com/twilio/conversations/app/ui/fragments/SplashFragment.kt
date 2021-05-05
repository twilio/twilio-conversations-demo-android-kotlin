package com.twilio.conversations.app.ui.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.extensions.showSnackbar
import com.twilio.conversations.app.viewModel.SplashViewModel
import kotlinx.android.synthetic.main.fragment_splash_screen.*
import timber.log.Timber

class SplashFragment : DialogFragment() {

    private val splashViewModel by lazy {
        ViewModelProvider(activity as AppCompatActivity).get(SplashViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return object : Dialog(requireContext(), theme) {
            override fun onBackPressed() {
                activity?.moveTaskToBack(true)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_splash_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        splashViewModel.onDisplayError.observe(this, {
            displayError()
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

    private fun displayError() {
        val message = getString(R.string.sign_in_error)
        splashCoordinatorLayout.showSnackbar(message)
    }
}
