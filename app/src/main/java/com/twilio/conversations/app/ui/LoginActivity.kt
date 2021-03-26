package com.twilio.conversations.app.ui

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.lazyViewModel
import com.twilio.conversations.app.common.extensions.showSnackbar
import com.twilio.conversations.app.common.injector
import kotlinx.android.synthetic.main.activity_login.*
import timber.log.Timber

class LoginActivity : AppCompatActivity() {

    val loginViewModel by lazyViewModel { injector.createLoginViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginViewModel.isLoading.observe(this, { isLoading ->
            changeLoading(isLoading)
        })

        loginViewModel.onSignInError.observe(this, { signInError ->
            signInFailed(signInError)
        })

        loginViewModel.onSignInSuccess.observe(this, {
            signInSucceeded()
        })

        signInBtn.setOnClickListener { signInPressed() }
    }

    override fun onDestroy() {
        Timber.d("onDestroy")
        super.onDestroy()
    }

    private fun signInPressed() {
        Timber.d("signInPressed")
        val identity = usernameTv.text.toString()
        val password = passwordTv.text.toString()
        loginViewModel.signIn(identity, password)
    }

    private fun startLoading() {
        Timber.d("startLoading")
        loginProgressBar.visibility = View.VISIBLE
        loginLayout.visibility = View.GONE
    }

    private fun stopLoading() {
        Timber.d("stopLoading")
        loginProgressBar.visibility = View.GONE
        loginLayout.visibility = View.VISIBLE
    }

    private fun goToConversationListScreen() {
        Timber.d("go to next screen")
        Timber.d("startActivity ConversationListActivity")
        startActivity(Intent(this, ConversationListActivity::class.java))
    }

    private fun signInSucceeded() = goToConversationListScreen()

    private fun signInFailed(error: ConversationsError) {
        val errorMessage = when (error) {
            ConversationsError.TOKEN_ERROR -> resources.getString(R.string.token_error)
            else -> resources.getString(R.string.sign_in_error)
        }
        loginCoordinatorLayout.showSnackbar(errorMessage)
    }

    private fun changeLoading(isLoading: Boolean) {
        if (isLoading) {
            startLoading()
        } else {
            stopLoading()
        }
    }

    companion object {

        fun start(context: Context) {
            val intent = Intent(context, LoginActivity::class.java)

            // Should always start LoginActivity with these flags
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TASK or FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
