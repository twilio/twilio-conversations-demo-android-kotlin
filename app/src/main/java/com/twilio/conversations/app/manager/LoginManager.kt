package com.twilio.conversations.app.manager

import com.twilio.conversations.app.common.FirebaseTokenManager
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.ConversationsError.NO_STORED_CREDENTIALS
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.common.extensions.registerFCMToken
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
    private val firebaseTokenManager: FirebaseTokenManager,
) : LoginManager {

    override suspend fun registerForFcm() {
        try {
            val token = firebaseTokenManager.retrieveToken()
            credentialStorage.fcmToken = token
            Timber.d("Registering for FCM: $token")
            conversationsClient.getConversationsClient().registerFCMToken(token)
        } catch (e: Exception) {
            Timber.d(e, "Failed to register FCM")
        }
    }

    override suspend fun unregisterFromFcm() {
        // We don't call `conversationsClient.getConversationsClient().unregisterFCMToken(token)` here
        // because it fails with commandTimeout (60s by default) if device is offline or token is expired.
        // Instead we try to delete token on FCM async. Which leads to the same result if device is online,
        // but we can shutdown `conversationsClient`immediately without waiting a result.
        firebaseTokenManager.deleteToken()
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
        if (credentialStorage.isEmpty()) throw ConversationsException(NO_STORED_CREDENTIALS)

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
