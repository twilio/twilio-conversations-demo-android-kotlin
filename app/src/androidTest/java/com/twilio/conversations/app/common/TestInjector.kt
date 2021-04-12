package com.twilio.conversations.app.common

import android.app.Application
import android.content.Context
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.manager.*
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.mockito.mock
import com.twilio.conversations.app.testUtil.mockito.whenCall
import com.twilio.conversations.app.viewModel.ConversationDetailsViewModel
import com.twilio.conversations.app.viewModel.ConversationListViewModel
import com.twilio.conversations.app.viewModel.MessageListViewModel
import com.twilio.conversations.app.viewModel.ParticipantListViewModel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

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
        whenCall(getUserConversations()) doAnswer { userConversationRepositoryResult }
        whenCall(getConversationParticipants(any())) doAnswer { participantRepositoryResult }
        whenCall(getTypingParticipants(any())) doAnswer { typingParticipantsList }
        whenCall(getMessages(any(), any())) doAnswer { messageResult }
        whenCall(getSelfUser()) doAnswer { emptyFlow() }
        runBlocking {
            whenCall(getConversation(any())) doAnswer { flowOf(RepositoryResult(null as ConversationDataItem?, RepositoryRequestStatus.COMPLETE)) }
        }
    }

    private val messageListManagerMock: ConversationListManager = com.nhaarman.mockitokotlin2.mock {
        onBlocking { createConversation(any()) } doReturn  ""
        onBlocking { joinConversation(any()) } doReturn Unit
        onBlocking { removeConversation(any()) } doReturn Unit
        onBlocking { leaveConversation(any()) } doReturn Unit
        onBlocking { muteConversation(any()) } doReturn Unit
        onBlocking { unmuteConversation(any()) } doReturn Unit
    }

    private val participantListManagerMock: ParticipantListManager = com.nhaarman.mockitokotlin2.mock {
        onBlocking { addChatParticipant(any()) } doReturn Unit
        onBlocking { addNonChatParticipant(any(), any(), any()) } doReturn Unit
        onBlocking { removeParticipant(any()) } doReturn Unit
    }

    private val userManagerMock: UserManager = com.nhaarman.mockitokotlin2.mock {
        onBlocking { setFriendlyName(any()) } doReturn Unit
    }

    private val messageListManager: MessageListManager = com.nhaarman.mockitokotlin2.mock()

    private val loginManagerMock: LoginManager = com.nhaarman.mockitokotlin2.mock()

    override fun createConversationListViewModel(application: Application)
            = ConversationListViewModel(repositoryMock, messageListManagerMock, userManagerMock, loginManagerMock)

    override fun createMessageListViewModel(appContext: Context, conversationSid: String)
            = MessageListViewModel(appContext, conversationSid, repositoryMock, messageListManager)

    override fun createParticipantListViewModel(conversationSid: String)
            = ParticipantListViewModel(conversationSid, repositoryMock, participantListManagerMock)

    override fun createConversationDetailsViewModel(conversationSid: String)
            = ConversationDetailsViewModel(conversationSid, repositoryMock, messageListManagerMock, participantListManagerMock)
}
