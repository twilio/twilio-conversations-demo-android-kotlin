package com.twilio.conversations.app.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.manager.ConnectivityMonitor
import com.twilio.conversations.app.manager.LoginManager
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginManager: LoginManager,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    val isLoading = MutableLiveData(false)

    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onSignInError = SingleLiveEvent<ConversationsError>()

    val onSignInSuccess = SingleLiveEvent<Unit>()

    fun signIn(identity: String, password: String) {
        if (isLoading.value == true) return
        Timber.d("signIn in viewModel")

        if (isNetworkAvailable.value == false) {
            Timber.d("no internet connection")
            onSignInError.value = ConversationsError.NO_INTERNET_CONNECTION
            return
        }

        val credentialError = validateSignInDetails(identity, password)

        if (credentialError != ConversationsError.NO_ERROR) {
            Timber.d("credentials are not valid")
            onSignInError.value = credentialError
            return
        }

        Timber.d("credentials are valid")
        isLoading.value = true
        viewModelScope.launch {
            try {
                loginManager.signIn(identity, password)
                onSignInSuccess.call()
            } catch (e: ConversationsException) {
                isLoading.value = false
                onSignInError.value = e.error
            }
        }
    }

    private fun validateSignInDetails(identity: String, password: String): ConversationsError {
        Timber.d("validateSignInDetails")
        return when {
            identity.isBlank() && password.isBlank() -> ConversationsError.EMPTY_USERNAME_AND_PASSWORD
            identity.isBlank() -> ConversationsError.EMPTY_USERNAME
            password.isBlank() -> ConversationsError.EMPTY_PASSWORD
            else -> ConversationsError.NO_ERROR
        }
    }
}
