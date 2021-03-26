package com.twilio.conversations.app.common

import android.app.Application
import android.content.Context
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.CredentialStorage
import com.twilio.conversations.app.manager.*
import com.twilio.conversations.app.repository.ConversationsRepositoryImpl
import com.twilio.conversations.app.viewModel.*

var injector = Injector()
    private set

@RestrictTo(Scope.TESTS)
fun setupTestInjector(testInjector: Injector) {
    injector = testInjector
}

open class Injector {

    private var fcmManagerImpl: FCMManagerImpl? = null

    open fun createLoginViewModel(application: Application): LoginViewModel {
        val credentialStorage = CredentialStorage(application.applicationContext)
        val loginManager = LoginManagerImpl(ConversationsClientWrapper.INSTANCE,
            ConversationsRepositoryImpl.INSTANCE, credentialStorage, FirebaseTokenRetriever())

        return LoginViewModel(loginManager, application)
    }

    open fun createSplashViewModel(application: Application): SplashViewModel {
        val credentialStorage = CredentialStorage(application.applicationContext)
        val loginManager = LoginManagerImpl(ConversationsClientWrapper.INSTANCE,
            ConversationsRepositoryImpl.INSTANCE, credentialStorage, FirebaseTokenRetriever())

        val viewModel = SplashViewModel(loginManager, application)
        viewModel.initialize()

        return viewModel
    }

    open fun createConversationListViewModel(application: Application): ConversationListViewModel {
        val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
        val credentialStorage = CredentialStorage(application.applicationContext)
        val userManager = UserManagerImpl(ConversationsClientWrapper.INSTANCE)
        val loginManager = LoginManagerImpl(ConversationsClientWrapper.INSTANCE,
            ConversationsRepositoryImpl.INSTANCE, credentialStorage, FirebaseTokenRetriever())

        return ConversationListViewModel(ConversationsRepositoryImpl.INSTANCE, conversationListManager, userManager, loginManager)
    }

    open fun createMessageListViewModel(appContext: Context, conversationSid: String): MessageListViewModel {
        val messageListManager = MessageListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE, ConversationsRepositoryImpl.INSTANCE)
        return MessageListViewModel(appContext, conversationSid, ConversationsRepositoryImpl.INSTANCE, messageListManager)
    }

    open fun createConversationDetailsViewModel(conversationSid: String): ConversationDetailsViewModel {
        val conversationListManager = ConversationListManagerImpl(ConversationsClientWrapper.INSTANCE)
        val participantListManager = ParticipantListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE)
        return ConversationDetailsViewModel(conversationSid, ConversationsRepositoryImpl.INSTANCE, conversationListManager, participantListManager)
    }

    open fun createParticipantListViewModel(conversationSid: String): ParticipantListViewModel {
        val participantListManager = ParticipantListManagerImpl(conversationSid, ConversationsClientWrapper.INSTANCE)
        return ParticipantListViewModel(conversationSid, ConversationsRepositoryImpl.INSTANCE, participantListManager)
    }

    open fun createFCMManager(context: Context): FCMManager {
        val credentialStorage = CredentialStorage(context.applicationContext)
        if (fcmManagerImpl == null) {
            fcmManagerImpl = FCMManagerImpl(context, ConversationsClientWrapper.INSTANCE, credentialStorage)
        }
        return fcmManagerImpl!!
    }
}
