package com.twilio.conversations.app.manager

import com.twilio.conversations.app.common.FirebaseTokenRetriever
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.ConversationsError.EMPTY_CREDENTIALS
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.common.extensions.registerFCMToken
import com.twilio.conversations.app.common.extensions.unregisterFCMToken
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.repository.ConversationsRepository
import timber.log.Timber

interface LoginManager {
    suspend fun signIn(identity: String, password: String)
    suspend fun signInUsingStoredCredentials()
    suspend fun signOut()
    suspend fun registerForFcm()
    suspend fun unregisterFromFcm()
    fun clearCredentials()
    fun isLoggedIn(): Boolean
}

class LoginManagerImpl(
    private val conversationsClient: ConversationsClientWrapper,
    private val conversationsRepository: ConversationsRepository,
    private val credentialStorage: CredentialStorage,
    private val firebaseTokenRetriever: FirebaseTokenRetriever
) : LoginManager {

    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenRetriever.retrieveToken()
            credentialStorage.fcmToken = token
            Timber.d("Registering for FCM: $token")
            conversationsClient.getConversationsClient().registerFCMToken(token)
        } catch (e: Exception) {
            Timber.d(e, "Failed to register FCM")
        }
    }

    override suspend fun unregisterFromFcm() {
        try {
            credentialStorage.fcmToken.takeIf { it.isNotEmpty() }?.let { token ->
                Timber.d("Unregistering from FCM")
                conversationsClient.getConversationsClient().unregisterFCMToken(token)
            }
        } catch (e: ConversationsException) {
            Timber.d(e, "Failed to unregister FCM")
        }
    }

    override suspend fun signIn(identity: String, password: String) {
        Timber.d("signIn")
        conversationsClient.create(identity, password)
        credentialStorage.storeCredentials(identity, password)
        conversationsRepository.subscribeToConversationsClientEvents()
        registerForFcm()
    }

    override suspend fun signInUsingStoredCredentials() {
        Timber.d("signInUsingStoredCredentials")
        if (credentialStorage.isEmpty()) throw ConversationsException(EMPTY_CREDENTIALS)

        val identity = credentialStorage.identity
        val password = credentialStorage.password

        try {
            conversationsClient.create(identity, password)
            conversationsRepository.subscribeToConversationsClientEvents()
            registerForFcm()
        } catch (e: ConversationsException) {
            handleError(e.error)
            throw e
        }
    }

    override suspend fun signOut() {
        unregisterFromFcm()
        clearCredentials()
        conversationsRepository.unsubscribeFromConversationsClientEvents()
        conversationsRepository.clear()
        conversationsClient.shutdown()
    }

    override fun isLoggedIn() = conversationsClient.isClientCreated && !credentialStorage.isEmpty()

    override fun clearCredentials() {
        credentialStorage.clearCredentials()
    }

    private fun handleError(error: ConversationsError) {
        Timber.d("handleError")
        if (error == ConversationsError.TOKEN_ACCESS_DENIED) {
            clearCredentials()
        }
    }
}
