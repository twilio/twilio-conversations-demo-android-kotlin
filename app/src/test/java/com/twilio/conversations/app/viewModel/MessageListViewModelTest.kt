package com.twilio.conversations.app.viewModel

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.Conversation
import com.twilio.conversations.User
import com.twilio.conversations.app.asPagedList
import com.twilio.conversations.app.common.asMessageListViewItems
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.DownloadState
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.createTestConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.getMockedMessages
import com.twilio.conversations.app.manager.MessageListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitNotCalled
import com.twilio.conversations.app.testUtil.waitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.io.InputStream
import java.util.*

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Conversation::class
)
class MessageListViewModelTest {

    @Rule
    var coroutineTestRule = CoroutineTestRule()

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val conversationSid = "conversationSid"
    private val conversationName = "Test Conversation"
    private val messageCount = 10
    private val messageBody = "Test Message"

    @MockK
    private lateinit var selfUser: User

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var messageListManager: MessageListManager

    @MockK
    private lateinit var context: Context

    private lateinit var messageListViewModel: MessageListViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Default)
        val conversation: ConversationDataItem? = createTestConversationDataItem(sid = conversationSid, friendlyName = conversationName)
        coEvery { conversationsRepository.getConversation(any()) } returns
                flowOf(RepositoryResult(conversation, RepositoryRequestStatus.COMPLETE))
        coEvery { conversationsRepository.getMessages(any(), any()) } returns
                flowOf(RepositoryResult(listOf< MessageListViewItem>().asPagedList(), RepositoryRequestStatus.COMPLETE))
        coEvery { messageListManager.updateMessageStatus(any(), any()) } returns Unit
        coEvery { conversationsRepository.getTypingParticipants(conversationSid) } returns flowOf(listOf())

        every { selfUser.identity } returns "selfuser"
        coEvery { conversationsRepository.getSelfUser() } returns flowOf(selfUser)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `messageListViewModel_conversationName should be initialized on init`() = runBlocking {
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        assertEquals(conversationName, messageListViewModel.conversationName.waitValue())
    }

    @Test
    fun `messageListViewModel_messageItems should contain all messages`() = runBlocking {
        val expectedMessages = getMockedMessages(messageCount, messageBody, conversationSid).asMessageListViewItems()

        coEvery { conversationsRepository.getMessages(any(), any()) } returns
                flowOf(RepositoryResult(expectedMessages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        assertEquals(expectedMessages, messageListViewModel.messageItems.waitValue())
        assertEquals(messageCount, messageListViewModel.messageItems.waitValue().size)
    }

    @Test
    fun `messageListViewModel_messageItems should be empty when Error occurred`() = runBlocking {
        coEvery { conversationsRepository.getMessages(any(), any()) } returns
                flowOf(RepositoryResult(listOf< MessageListViewItem>().asPagedList(), RepositoryRequestStatus.Error(ConversationsError.GENERIC_ERROR)))

        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        assertEquals(0, messageListViewModel.messageItems.waitValue().size)
    }

    @Test
    fun `messageListViewModel_sendTextMessage should call onMessageSent on success`() = runBlocking {
        coEvery { messageListManager.sendTextMessage(any(), any()) } returns Unit
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.sendTextMessage(messageBody)

        assertTrue(messageListViewModel.onMessageSent.waitCalled())
        assertTrue(messageListViewModel.onMessageError.waitNotCalled())
    }

    @Test
    fun `messageListViewModel_sendTextMessage should call onMessageError on failure`() = runBlocking {
        coEvery { messageListManager.sendTextMessage(any(), any()) } throws ConversationsException(ConversationsError.MESSAGE_SEND_FAILED)
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.sendTextMessage(messageBody)

        assertTrue(messageListViewModel.onMessageSent.waitNotCalled())
        assertTrue(messageListViewModel.onMessageError.waitValue(ConversationsError.MESSAGE_SEND_FAILED))
    }

    @Test
    fun `messageListViewModel_resendTextMessage should call onMessageSent on success`() = runBlocking {
        coEvery { messageListManager.retrySendTextMessage(any()) } returns Unit
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.resendTextMessage(UUID.randomUUID().toString())

        assertTrue(messageListViewModel.onMessageSent.waitCalled())
        assertTrue(messageListViewModel.onMessageError.waitNotCalled())
    }

    @Test
    fun `messageListViewModel_resendTextMessage should call onMessageError on failure`() = runBlocking {
        coEvery { messageListManager.retrySendTextMessage(any()) } throws ConversationsException(ConversationsError.MESSAGE_SEND_FAILED)
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.resendTextMessage(UUID.randomUUID().toString())

        assertTrue(messageListViewModel.onMessageSent.waitNotCalled())
        assertTrue(messageListViewModel.onMessageError.waitValue(ConversationsError.MESSAGE_SEND_FAILED))
    }

    @Test
    fun `typingParticipantsList is updated`() = runBlocking {
        val userFriendlyName = "userFriendlyName"
        coEvery { conversationsRepository.getTypingParticipants(conversationSid) } returns flowOf(listOf(
            ParticipantDataItem(
                identity = "identity", conversationSid = conversationSid, lastReadTimestamp = null,
                lastReadMessageIndex = null, sid = "321", friendlyName = userFriendlyName, isOnline = true
            )
        ))

        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        assertEquals(listOf(userFriendlyName), messageListViewModel.typingParticipantsList.waitValue())
    }

    @Test
    fun `sendMediaMessage should call onMessageSent on success`() = runBlocking {
        coEvery { messageListManager.sendMediaMessage(any(), any(), any(), any(), any()) } returns Unit
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.sendMediaMessage("", mock(InputStream::class.java), null, null)

        assertTrue(messageListViewModel.onMessageSent.waitCalled())
        assertTrue(messageListViewModel.onMessageError.waitNotCalled())
    }

    @Test
    fun `sendMediaMessage should call onMessageError on failure`() = runBlocking {
        coEvery { messageListManager.sendMediaMessage(any(), any(), any(), any(), any()) } throws ConversationsException(ConversationsError.MESSAGE_SEND_FAILED)
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.sendMediaMessage("", mock(InputStream::class.java), null, null)

        assertTrue(messageListViewModel.onMessageSent.waitNotCalled())
        assertTrue(messageListViewModel.onMessageError.waitValue(ConversationsError.MESSAGE_SEND_FAILED))
    }

    @Test
    fun `resendMediaMessage should call onMessageSent on success`() = runBlocking {
        coEvery { messageListManager.retrySendMediaMessage(any(), any()) } returns Unit
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.resendMediaMessage( mock(InputStream::class.java), "")

        assertTrue(messageListViewModel.onMessageSent.waitCalled())
        assertTrue(messageListViewModel.onMessageError.waitNotCalled())
    }

    @Test
    fun `resendMediaMessage should call onMessageError on failure`() = runBlocking {
        coEvery { messageListManager.retrySendMediaMessage(any(), any()) } throws ConversationsException(ConversationsError.MESSAGE_SEND_FAILED)
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)
        messageListViewModel.resendMediaMessage( mock(InputStream::class.java), "")

        assertTrue(messageListViewModel.onMessageSent.waitNotCalled())
        assertTrue(messageListViewModel.onMessageError.waitValue(ConversationsError.MESSAGE_SEND_FAILED))
    }

    @Test
    fun `updateMessageMediaDownloadStatus should call MessageListManager#updateMessageMediaDownloadStatus`() = runBlocking {
        val messageIndex = 1L
        val downloadState = DownloadState.NOT_STARTED
        val downloadedBytes = 2L
        val downloadLocation = "asd"
        coEvery { messageListManager.updateMessageMediaDownloadState(any(), any(), any(), any()) } returns Unit
        messageListViewModel = MessageListViewModel(context, conversationSid, conversationsRepository, messageListManager)

        messageListViewModel.updateMessageMediaDownloadStatus(messageIndex, downloadState, downloadedBytes, downloadLocation)
        coVerify { messageListManager.updateMessageMediaDownloadState(messageIndex, downloadState, downloadedBytes, downloadLocation) }
    }
}
