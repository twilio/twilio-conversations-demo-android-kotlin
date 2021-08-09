@file:Suppress("IncorrectScope")

package com.twilio.conversations.app.repository

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.Conversation
import com.twilio.conversations.ConversationListener
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.ConversationsClientListener
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.User
import com.twilio.conversations.app.ItemDataSource
import com.twilio.conversations.app.MESSAGE_COUNT
import com.twilio.conversations.app.USER_CONVERSATION_COUNT
import com.twilio.conversations.app.common.asMessageDataItems
import com.twilio.conversations.app.common.asMessageListViewItems
import com.twilio.conversations.app.common.asParticipantDataItem
import com.twilio.conversations.app.common.enums.ConversationsError.UNKNOWN
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.common.extensions.getAndSubscribeUser
import com.twilio.conversations.app.common.extensions.getConversation
import com.twilio.conversations.app.common.extensions.getLastMessages
import com.twilio.conversations.app.common.extensions.waitForSynchronization
import com.twilio.conversations.app.common.toMessageDataItem
import com.twilio.conversations.app.createTestConversationDataItem
import com.twilio.conversations.app.createTestMessageDataItem
import com.twilio.conversations.app.createTestParticipantDataItem
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.LocalCacheProvider
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryRequestStatus.COMPLETE
import com.twilio.conversations.app.data.models.RepositoryRequestStatus.FETCHING
import com.twilio.conversations.app.data.models.RepositoryRequestStatus.SUBSCRIBING
import com.twilio.conversations.app.getMockedConversations
import com.twilio.conversations.app.getMockedMessages
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.toConversationMock
import com.twilio.conversations.app.testUtil.toMessageMock
import com.twilio.conversations.app.testUtil.toParticipantMock
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    ConversationsClient::class,
    Conversation::class,
    Participant::class,
    Message::class,
    User::class
)
class ConversationsRepositoryTest {

    private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    @Rule
    var coroutineTestRule = CoroutineTestRule(testDispatcher)

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var conversationsRepository: ConversationsRepositoryImpl

    @RelaxedMockK
    private lateinit var localCacheProvider: LocalCacheProvider

    @MockK
    private lateinit var conversationsClientWrapper: ConversationsClientWrapper

    @MockK
    private lateinit var conversationsClient: ConversationsClient
    
    @MockK
    private lateinit var conversation: Conversation

    private lateinit var clientListener: ConversationsClientListener

    private val myIdentity = "test_id"

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic("com.twilio.conversations.app.common.extensions.TwilioExtensionsKt")
        mockkStatic("com.twilio.conversations.app.common.DataConverterKt")

        every { conversationsClient.myIdentity } returns myIdentity
        every { conversationsClient.addListener(any()) } answers { clientListener = it.invocation.args[0] as ConversationsClientListener }
        every { conversationsClient.myConversations } returns emptyList()

        coEvery { conversation.waitForSynchronization() } returns conversation
        coEvery { conversationsClient.getConversation(any()) } returns conversation
        coEvery { conversationsClientWrapper.getConversationsClient() } returns conversationsClient

        conversationsRepository = ConversationsRepositoryImpl(conversationsClientWrapper, localCacheProvider, coroutineTestRule.testDispatcherProvider)
        conversationsRepository.subscribeToConversationsClientEvents()
    }

    @Test
    fun `getUserConversations() should return statuses in correct order`() = runBlocking {
        every { localCacheProvider.conversationsDao().getUserConversations() } returns flowOf(emptyList())

        val actual = conversationsRepository.getUserConversations().toList().map { it.requestStatus }
        val expected = listOf(FETCHING, SUBSCRIBING, COMPLETE)

        assertEquals(expected, actual)
    }

    @Test
    fun `getUserConversations() first should return user conversations stored in local cache`() = runBlocking {
        val expectedConversations = getMockedConversations(USER_CONVERSATION_COUNT, "User Conversations").toList()

        every { localCacheProvider.conversationsDao().getUserConversations() } returns flowOf(expectedConversations)

        val actualConversations = conversationsRepository.getUserConversations().first().data

        assertEquals(expectedConversations, actualConversations)
    }

    @Test
    fun `getUserConversations() should fetch conversations and store them in local cache`() = runBlocking {
        val expectedConversation = createTestConversationDataItem()

        every { localCacheProvider.conversationsDao().getUserConversations() } returns flowOf(emptyList())
        every { conversationsClient.myConversations } returns listOf(expectedConversation.toConversationMock())

        coEvery { conversationsClient.getConversation(any()) } returns expectedConversation.toConversationMock()

        val actualStatus = conversationsRepository.getUserConversations().toList().last().requestStatus

        assertEquals(COMPLETE, actualStatus)
        verify { localCacheProvider.conversationsDao().insert(expectedConversation) }
    }

    @Test
    fun `getUserConversations() should delete outdated conversations from local cache`() = runBlocking {
        val expectedConversations = getMockedConversations(USER_CONVERSATION_COUNT, "User Conversations").toList()

        every { localCacheProvider.conversationsDao().getUserConversations() } returns flowOf(expectedConversations)

        val actualStatus = conversationsRepository.getUserConversations().toList().last().requestStatus

        assertEquals(COMPLETE, actualStatus)
        verify { localCacheProvider.conversationsDao().deleteGoneUserConversations(emptyList()) }
        confirmVerified(localCacheProvider)
    }

    @Test
    fun `getUserConversations() should return error if cannot fetch conversation`() = runBlocking {
        val expectedConversation = createTestConversationDataItem()

        every { localCacheProvider.conversationsDao().getUserConversations() } returns flowOf(emptyList())
        every { conversationsClient.myConversations } returns listOf(expectedConversation.toConversationMock())
        coEvery { conversationsClient.getConversation(any()) } throws ConversationsException(UNKNOWN)

        val actualStatus = conversationsRepository.getUserConversations().toList().last().requestStatus
        assertEquals(UNKNOWN, (actualStatus as RepositoryRequestStatus.Error).error)
    }

    @Test
    fun `onConversationDeleted should remove received Conversation from local cache when called`() = runBlocking {
        val conversation = createTestConversationDataItem().toConversationMock()

        clientListener.onConversationDeleted(conversation)

        verify(timeout = 10_000) { localCacheProvider.conversationsDao().delete(conversation.sid) }
        confirmVerified(localCacheProvider)
    }

    @Test
    fun `onConversationAdded should add received Conversation to local cache when called`() = runBlocking {
        val conversation = createTestConversationDataItem()
        coEvery { conversationsClient.getConversation(any()) } returns conversation.toConversationMock()

        val lastMessage = createTestMessageDataItem()
        every { localCacheProvider.messagesDao().getLastMessage(conversation.sid) } returns lastMessage

        clientListener.onConversationAdded(conversation.toConversationMock())

        verify(timeout = 10_000) { localCacheProvider.conversationsDao().insert(conversation) }
        verify(timeout = 10_000) { localCacheProvider.conversationsDao().update(conversation.sid,
            conversation.participatingStatus, conversation.notificationLevel, conversation.friendlyName) }

        // Also should update the last message of the conversation in local cache
        verify(timeout = 10_000) { localCacheProvider.messagesDao().getLastMessage(conversation.sid) }
        verify(timeout = 10_000) { localCacheProvider.conversationsDao().updateLastMessage(conversation.sid, lastMessage.body!!, lastMessage.sendStatus, lastMessage.dateCreated) }

        confirmVerified(localCacheProvider)
    }

    @Test
    fun `onConversationUpdated should update received Conversation in local cache when called`() = runBlocking {
        val conversation = createTestConversationDataItem()
        coEvery { conversationsClient.getConversation(any()) } returns conversation.toConversationMock()

        val lastMessage = createTestMessageDataItem()
        every { localCacheProvider.messagesDao().getLastMessage(conversation.sid) } returns lastMessage

        clientListener.onConversationUpdated(conversation.toConversationMock(), Conversation.UpdateReason.ATTRIBUTES)

        verify(timeout = 10_000) { localCacheProvider.conversationsDao().insert(conversation) }
        verify(timeout = 10_000) { localCacheProvider.conversationsDao().update(conversation.sid,
            conversation.participatingStatus, conversation.notificationLevel, conversation.friendlyName) }

        // Also should update the last message of the conversation in local cache
        verify(timeout = 10_000) { localCacheProvider.messagesDao().getLastMessage(conversation.sid) }
        verify(timeout = 10_000) { localCacheProvider.conversationsDao().updateLastMessage(conversation.sid, lastMessage.body!!, lastMessage.sendStatus, lastMessage.dateCreated) }

        confirmVerified(localCacheProvider)
    }

    @Test
    fun `getMessages() should return statuses in correct order`() = runBlocking {
        every { localCacheProvider.messagesDao().getMessagesSorted(any()) } returns ItemDataSource.factory(emptyList())
        coEvery { conversation.getLastMessages(any()).asMessageDataItems(any()) } returns emptyList()
        val expected = listOf(FETCHING, COMPLETE)

        assertEquals(expected, conversationsRepository.getMessages("", 1).take(2).toList().map { it.requestStatus })
    }

    @Test
    fun `getMessages() first should return messages stored in local cache`() = runBlocking {
        val conversationSid = "conversation_1"
        val expectedMessages = getMockedMessages(MESSAGE_COUNT, "Message body", conversationSid)

        every { localCacheProvider.messagesDao().getMessagesSorted(conversationSid) } returns ItemDataSource.factory(expectedMessages)

        val actualConversations = conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT).first().data

        assertEquals(expectedMessages.asMessageListViewItems(), actualConversations)
    }

    @Test
    fun `getMessages() should fetch messages and store them in local cache`() = runBlocking {
        val conversationSid = "conversation_1"
        val expectedMessage = createTestMessageDataItem(conversationSid = conversationSid)

        every { localCacheProvider.messagesDao().getMessagesSorted(any()) } returns ItemDataSource.factory(emptyList())
        coEvery { conversation.getLastMessages(any()).asMessageDataItems(any()) } returns listOf(expectedMessage)

        conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT).first { it.requestStatus is COMPLETE }

        verify { localCacheProvider.messagesDao().insert(listOf(expectedMessage)) }
    }

    @Test
    fun `getMessages() should return error if cannot fetch conversation descriptors`() = runBlocking {
        every { localCacheProvider.messagesDao().getMessagesSorted(any()) } returns ItemDataSource.factory(emptyList())
        coEvery { conversation.getLastMessages(any()).asMessageDataItems(any()) } throws ConversationsException(UNKNOWN)

        val actualStatus = conversationsRepository.getMessages("conversationSid", MESSAGE_COUNT)
            .first { it.requestStatus is RepositoryRequestStatus.Error }.requestStatus

        assertEquals(UNKNOWN, (actualStatus as RepositoryRequestStatus.Error).error)
    }

    @Test
    fun `getMessages() should return error if cannot fetch conversation`() = runBlocking {
        val conversationSid = "conversation_1"
        val expectedMessage = createTestMessageDataItem(conversationSid = conversationSid)

        every { localCacheProvider.messagesDao().getMessagesSorted(any())} returns ItemDataSource.factory(emptyList())
        coEvery { conversation.getLastMessages(any()).asMessageDataItems(any()) } returns listOf(expectedMessage)
        coEvery { conversationsClient.getConversation(any()) } throws ConversationsException(UNKNOWN)

        val actualStatus = conversationsRepository.getMessages(conversationSid, MESSAGE_COUNT)
            .first { it.requestStatus is RepositoryRequestStatus.Error }.requestStatus
        assertEquals(UNKNOWN, (actualStatus as RepositoryRequestStatus.Error).error)
    }

    @Test
    fun `getTypingMemebers should return data from LocalCache`() = runBlocking {
        val conversationSid = "123"
        val typingParticipants = listOf(ParticipantDataItem(conversationSid = conversationSid, identity = "asd", sid = "321",
            lastReadMessageIndex = null, lastReadTimestamp = null, friendlyName = "user", isOnline = true))
        every { localCacheProvider.participantsDao().getTypingParticipants(conversationSid) } returns flowOf(typingParticipants)

        assertEquals(typingParticipants, conversationsRepository.getTypingParticipants(conversationSid).first())
    }

    @Test
    fun `participant typing status updated via messageListManagerListener`() = runBlocking {
        // Set up a ConversationsRepository and capture the messageListManagerListener that's added to joined conversations
        val conversationsClient = mockk<ConversationsClient>()
        val listenerSlot = slot<ConversationListener>()
        coEvery { conversationsClientWrapper.getConversationsClient() } returns conversationsClient

        val conversation = mockk<Conversation>()
        coEvery { conversationsClient.getConversation(any()) } returns conversation
        every { conversation.addListener(capture(listenerSlot)) } answers { }
        every { conversation.friendlyName } returns ""
        every { conversation.sid } returns "123"

        conversationsRepository = ConversationsRepositoryImpl(conversationsClientWrapper, localCacheProvider)
        clientListener.onConversationAdded(conversation)

        val participant = mockk<Participant>()
        val user = mockk<User>()
        every { user.identity } returns "User"
        every { user.friendlyName } returns "friendlyUser"
        every { user.isOnline } returns true
        every { participant.sid } returns "321"
        every { participant.conversation } returns conversation
        every { participant.identity } returns "asd"
        every { participant.lastReadMessageIndex } returns null
        every { participant.lastReadTimestamp } returns null
        coEvery { participant.getAndSubscribeUser() } returns user
        val participantDataItemTyping = participant.asParticipantDataItem(typing = true, user)
        val participantDataItemNotTyping = participant.asParticipantDataItem(typing = false, user)

        // When calling ConversationListener.onTypingStarted(..)
        listenerSlot.captured.onTypingStarted(conversation, participant)

        // Then the local cache is updated with that participant
        verify { localCacheProvider.participantsDao().insertOrReplace(participantDataItemTyping) }

        // When calling ConversationListener.onTypingEnded(..)
        listenerSlot.captured.onTypingEnded(conversation, participant)

        // Then the local cache is updated with that participant
        verify { localCacheProvider.participantsDao().insertOrReplace(participantDataItemNotTyping) }
    }

    @Test
    fun `message deleted via ConversationListener`() = testDispatcher.runBlockingTest {
        val conversationListenerCaptor = ArgumentCaptor.forClass(ConversationListener::class.java)
        val conversation = createTestConversationDataItem().toConversationMock(conversationListenerCaptor = conversationListenerCaptor)
        val participant = createTestParticipantDataItem().toParticipantMock(conversation)
        val message = createTestMessageDataItem().toMessageMock(participant)
        val expectedMessage = message.toMessageDataItem(currentUserIdentity = myIdentity)
        prepareConversationsRepository(conversation)

        conversationListenerCaptor.value.onMessageDeleted(message)

        verify(timeout = 10_000) { localCacheProvider.messagesDao().delete(expectedMessage) }
    }

    @Test
    fun `message updated via ConversationListener`() = testDispatcher.runBlockingTest {
        val conversationListenerCaptor = ArgumentCaptor.forClass(ConversationListener::class.java)
        val conversation = createTestConversationDataItem().toConversationMock(conversationListenerCaptor = conversationListenerCaptor)
        val participant = createTestParticipantDataItem().toParticipantMock(conversation)
        val message = createTestMessageDataItem().toMessageMock(participant)
        val expectedMessage = message.toMessageDataItem(currentUserIdentity = myIdentity)
        prepareConversationsRepository(conversation)

        conversationListenerCaptor.value.onMessageUpdated(message, Message.UpdateReason.BODY)

        verify(timeout = 10_000) { localCacheProvider.messagesDao().insertOrReplace(expectedMessage) }
    }

    @Test
    fun `message added via ConversationListener`() = testDispatcher.runBlockingTest {
        val conversationListenerCaptor = ArgumentCaptor.forClass(ConversationListener::class.java)
        val conversation = createTestConversationDataItem().toConversationMock(conversationListenerCaptor = conversationListenerCaptor)
        val participant = createTestParticipantDataItem().toParticipantMock(conversation)
        val message = createTestMessageDataItem().toMessageMock(participant)
        val expectedMessage = message.toMessageDataItem(currentUserIdentity = myIdentity)
        prepareConversationsRepository(conversation)

        conversationListenerCaptor.value.onMessageAdded(message)

        verify(timeout = 10_000) { localCacheProvider.messagesDao().updateByUuidOrInsert(expectedMessage) }
    }

    private fun prepareConversationsRepository(conversation: Conversation) {
        // Set up a ConversationsRepository with a single conversation
        val conversationsClient = mockk<ConversationsClient>()
        every { conversationsClient.myIdentity } returns myIdentity
        every { conversationsClient.addListener(any()) } answers { clientListener = it.invocation.args[0] as ConversationsClientListener }
        coEvery { conversationsClientWrapper.getConversationsClient() } returns conversationsClient

        coEvery { conversationsClient.getConversation(any()) } returns conversation
        conversationsRepository = ConversationsRepositoryImpl(conversationsClientWrapper, localCacheProvider,
            coroutineTestRule.testDispatcherProvider)
        clientListener.onConversationAdded(conversation)
    }
}
