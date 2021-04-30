package com.twilio.conversations.app.data

import android.content.Context
import android.net.Uri
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.app.BuildConfig
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.createAndSyncClient
import com.twilio.conversations.app.data.models.Client
import com.twilio.conversations.app.data.models.Error
import com.twilio.conversations.app.data.models.Response
import com.twilio.conversations.app.data.models.Token
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.FileNotFoundException
import java.net.URL

class ConversationsClientWrapper(private val applicationContext: Context) {

    private var deferredClient = CompletableDeferred<ConversationsClient>()

    private lateinit var identity: String
    private lateinit var password: String

    val isClientCreated get() = deferredClient.isCompleted && !deferredClient.isCancelled

    suspend fun getConversationsClient() = deferredClient.await() // Business logic will wait until conversationsClient created

    /**
     * Get token and call createClient if token is not null
     */
    suspend fun create(identity: String, password: String): Response {
        Timber.d("create")
        return when (val tokenResponse = getToken(identity, password)) {
            is Error -> tokenResponse
            is Token -> {
                Timber.d("token: ${tokenResponse.token}")
                val createClientResponse = createAndSyncClient(applicationContext, tokenResponse.token)
                if (createClientResponse is Client) {
                    this@ConversationsClientWrapper.identity = identity
                    this@ConversationsClientWrapper.password = password
                    this@ConversationsClientWrapper.deferredClient.complete(createClientResponse.conversationsClient)
                }
                createClientResponse
            }
            else -> getGenericError()
        }
    }

    suspend fun shutdown() {
        getConversationsClient().shutdown()
        deferredClient = CompletableDeferred()
    }

    /**
     * Fetch Twilio access token and return it, if token is non-null, otherwise return error
     */
    private suspend fun getToken(userName: String, password: String): Response {
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(TOKEN_URL)
                    .buildUpon()
                    .appendQueryParameter(QUERY_IDENTITY, userName)
                    .appendQueryParameter(QUERY_PASSWORD, password)
                    .build()
                    .toString()
                Token(URL(uri).readText())
            } catch (e: FileNotFoundException) {
                getTokenAccessDeniedError()
            } catch (e: Exception) {
                getTokenError()
            }
        }
    }

    /**
     * Construct generic token error
     */
    private fun getTokenError() = Error(ConversationsError.TOKEN_ERROR)

    /**
     * Construct token access denied error
     */
    private fun getTokenAccessDeniedError() = Error(ConversationsError.TOKEN_ACCESS_DENIED)

    /**
     * Construct generic error
     */
    private fun getGenericError() = Error(ConversationsError.GENERIC_ERROR)

    companion object {
        private const val TOKEN_URL = BuildConfig.ACCESS_TOKEN_SERVICE_URL
        private const val QUERY_IDENTITY = "identity"
        private const val QUERY_PASSWORD = "password"

        val INSTANCE get() = _instance ?: error("call ConversationsClientWrapper.createInstance() first")

        private var _instance: ConversationsClientWrapper? = null

        fun createInstance(applicationContext: Context) {
            check(_instance == null) { "ConversationsClientWrapper singleton instance has been already created" }
            _instance = ConversationsClientWrapper(applicationContext)
        }

        @RestrictTo(Scope.TESTS)
        fun recreateInstance(applicationContext: Context) {
            _instance?.let { instance ->
                // Shutdown old client if it will ever be created
                GlobalScope.launch { instance.getConversationsClient().shutdown() }
            }

            _instance = null
            createInstance(applicationContext)
        }
    }
}
