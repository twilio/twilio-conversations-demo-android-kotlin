package com.twilio.conversations.app.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.data.models.Client
import com.twilio.conversations.app.data.models.Error
import com.twilio.conversations.app.manager.LoginManager
import kotlinx.coroutines.launch
import timber.log.Timber

class LoginViewModel(
    private val loginManager: LoginManager,
) : ViewModel() {

    val isLoading = MutableLiveData<Boolean>()
    val onSignInError = SingleLiveEvent<ConversationsError>()
    val onSignInSuccess = SingleLiveEvent<Unit>()

    init {
        Timber.d("init view model ${this.hashCode()}")
        isLoading.value = false
    }

    fun signIn(identity: String, password: String) {
        if (isLoading.value == true) return

        Timber.d("signIn in viewModel")

        val credentialError = validateSignInDetails(identity, password)

        if (credentialError != ConversationsError.NO_ERROR) {
            Timber.d("creds not valid")
            onSignInError.value = credentialError
            return
        }
        Timber.d("creds valid")
        isLoading.value = true
        viewModelScope.launch {
            when (val response = loginManager.signIn(identity, password)) {
                is Client -> onSignInSuccess.call()
                is Error -> {
                    isLoading.value = false
                    onSignInError.value = response.error
                }
            }
        }
    }

    private fun validateSignInDetails(identity: String, password: String): ConversationsError {
        Timber.d("validateSignInDetails")
        return when {
            identity.isNotEmpty() && password.isNotEmpty() -> ConversationsError.NO_ERROR
            identity.isEmpty() && password.isNotEmpty() -> ConversationsError.INVALID_USERNAME
            identity.isNotEmpty() && password.isEmpty() -> ConversationsError.INVALID_PASSWORD
            else -> ConversationsError.INVALID_USERNAME_AND_PASSWORD
        }
    }
}
