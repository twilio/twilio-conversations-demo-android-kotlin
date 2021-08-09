package com.twilio.conversations.app.common

import android.app.Application
import android.content.Context
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.manager.ConnectivityMonitor
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.manager.MessageListManager
import com.twilio.conversations.app.manager.ParticipantListManager
import com.twilio.conversations.app.manager.UserManager
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.viewModel.ConversationDetailsViewModel
import com.twilio.conversations.app.viewModel.ConversationListViewModel
import com.twilio.conversations.app.viewModel.MessageListViewModel
import com.twilio.conversations.app.viewModel.ParticipantListViewModel
import com.twilio.conversations.app.viewModel.SplashViewModel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

val testInjector: TestInjector get() = injector as? TestInjector ?: error("You must call setupTestInjector() first")

fun setupTestInjector() = setupTestInjector(TestInjector())

open class TestInjector : Injector() {

    val typingParticipantsListConversation = BroadcastChannel<List<ParticipantDataItem>>(1)
    val messageResultConversation = BroadcastChannel<RepositoryResult<PagedList<MessageListViewItem>>>(1)

    var userConversationRepositoryResult : Flow<RepositoryResult<List<ConversationDataItem>>> = emptyFlow()
    var participantRepositoryResult : Flow<RepositoryResult<List<ParticipantDataItem>>> = emptyFlow()
    var typingParticipantsList = typingParticipantsListConversation.asFlow()
    var messageResult = messageResultConversation.asFlow()

    private val repositoryMock: ConversationsRepository = mock {
        whenever(it.getUserConversations()) doAnswer { userConversationRepositoryResult }
        whenever(it.getConversationParticipants(any())) doAnswer { participantRepositoryResult }
        whenever(it.getTypingParticipants(any())) doAnswer { typingParticipantsList }
        whenever(it.getMessages(any(), any())) doAnswer { messageResult }
        whenever(it.getSelfUser()) doAnswer { emptyFlow() }
        whenever(it.getConversation(any())) doAnswer { flowOf(RepositoryResult(null, RepositoryRequestStatus.COMPLETE)) }
    }

    private val conversationListManagerMock: ConversationListManager = mock {
        onBlocking { createConversation(any()) } doReturn  ""
        onBlocking { joinConversation(any()) } doReturn Unit
        onBlocking { removeConversation(any()) } doReturn Unit
        onBlocking { leaveConversation(any()) } doReturn Unit
        onBlocking { muteConversation(any()) } doReturn Unit
        onBlocking { unmuteConversation(any()) } doReturn Unit
    }

    private val participantListManagerMock: ParticipantListManager = mock {
        onBlocking { addChatParticipant(any()) } doReturn Unit
        onBlocking { addNonChatParticipant(any(), any(), any()) } doReturn Unit
        onBlocking { removeParticipant(any()) } doReturn Unit
    }

    private val userManagerMock: UserManager = mock {
        onBlocking { setFriendlyName(any()) } doReturn Unit
    }

    private val messageListManager: MessageListManager = mock()

    private val loginManagerMock: LoginManager = mock {
        whenever(it.isLoggedIn()) doReturn true
    }

    private val connectivityMonitorMock: ConnectivityMonitor = mock()

    override fun createSplashViewModel(application: Application)
            = SplashViewModel(loginManagerMock)

    override fun createConversationListViewModel(applicationContext: Context)
            = ConversationListViewModel(applicationContext, repositoryMock, conversationListManagerMock, connectivityMonitorMock)

    override fun createMessageListViewModel(appContext: Context, conversationSid: String)
            = MessageListViewModel(appContext, conversationSid, repositoryMock, messageListManager)

    override fun createParticipantListViewModel(conversationSid: String)
            = ParticipantListViewModel(conversationSid, repositoryMock, participantListManagerMock)

    override fun createConversationDetailsViewModel(conversationSid: String)
            = ConversationDetailsViewModel(conversationSid, repositoryMock, conversationListManagerMock, participantListManagerMock)
}
