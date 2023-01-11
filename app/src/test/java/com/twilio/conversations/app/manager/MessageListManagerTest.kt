package com.twilio.conversations.app.manager

import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.twilio.conversations.Conversation
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.Media
import com.twilio.conversations.Message
import com.twilio.conversations.Participant
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.extensions.createTwilioException
import com.twilio.conversations.app.common.extensions.toConversationsError
import com.twilio.conversations.app.createTestMessageDataItem
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.toMessageMock
import com.twilio.conversations.app.testUtil.whenCall
import com.twilio.conversations.extensions.sendMessage
import com.twilio.util.TwilioException
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    ConversationsClient::class,
    Conversation::class,
    Message::class,
    Participant::class,
)
class MessageListManagerTest {

    private val testDispatcher: TestCoroutineDispatcher = TestCoroutineDispatcher()

    @Rule
    var coroutineTestRule = CoroutineTestRule(testDispatcher)

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val conversationSid = "conversation_1"
    private val participantIdentity = "participant_id"

    private lateinit var messageListManager: MessageListManagerImpl

    @MockK
    private lateinit var conversationsClient: ConversationsClient
    @MockK
    private lateinit var conversation: Conversation
    @MockK
    private lateinit var participant: Participant
    @MockK
    private lateinit var conversationsClientWrapper: ConversationsClientWrapper
    @Mock
    private lateinit var conversationsRepository: ConversationsRepository

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Unconfined)

        mockkStatic("com.twilio.conversations.app.common.extensions.TwilioExtensionsKt")
        mockkStatic("com.twilio.conversations.app.common.DataConverterKt")
        mockkStatic("com.twilio.conversations.extensions.ConversationsExtensionsKt")
        mockkStatic("android.os.Looper")
        every { Looper.getMainLooper() } returns mockk()

        every { conversationsClient.myIdentity } returns participantIdentity

        coEvery { conversationsClient.getConversation(any()) } returns conversation
        coEvery { conversation.getParticipantByIdentity(any()) } returns participant
        coEvery { participant.identity } returns participantIdentity
        coEvery { conversationsClientWrapper.getConversationsClient() } returns conversationsClient

        messageListManager = MessageListManagerImpl(conversationSid, conversationsClientWrapper, conversationsRepository, coroutineTestRule.testDispatcherProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendTextMessage() should update local cache with send status SENT on success`() = runBlockingTest {
        val messageUuid = "uuid"
        val message = createTestMessageDataItem(body = "test message", uuid = messageUuid)
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        messageListManager.sendTextMessage(message.body!!, message.uuid)

        verify(conversationsRepository).insertMessage(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENDING.value
        })
    }

    @Test
    fun `sendTextMessage() should update local cache with send status SENDING on failure`() = runBlockingTest {
        val message = createTestMessageDataItem(body = "test message", uuid = "uuid")
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        try {
            messageListManager.sendTextMessage(message.body!!, message.uuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository).insertMessage(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENDING.value
        })

        verify(conversationsRepository, times(0)).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENT.value
        })
    }

    @Test
    fun `sendTextMessage() should NOT update local cache with on participant failure`() = runBlockingTest {
        val message = createTestMessageDataItem(body = "test message", uuid = "uuid")
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.getParticipantByIdentity(any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        try {
            messageListManager.sendTextMessage(message.body!!, message.uuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository, times(0)).insertMessage(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENDING.value
        })

        verify(conversationsRepository, times(0)).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENT.value
        })
    }

    @Test
    fun `retrySendMessage() should update local cache with send status SENT on success`() = runBlockingTest(testDispatcher) {
        val message = createTestMessageDataItem(body = "test message", uuid = "uuid",
            author = participantIdentity, sendStatus = SendStatus.ERROR.value)
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        messageListManager.retrySendTextMessage(message.uuid)

        inOrder(conversationsRepository).verify(conversationsRepository).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENDING.value
        })
        inOrder(conversationsRepository).verify(conversationsRepository).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENT.value
        })
    }

    @Test
    fun `retrySendMessage() should NOT update local cache if already sending`() = runBlockingTest(testDispatcher) {
        val message = createTestMessageDataItem(body = "test message", uuid = "uuid",
            author = participantIdentity, sendStatus = SendStatus.SENDING.value)
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        messageListManager.retrySendTextMessage(message.uuid)

        verify(conversationsRepository, times(0)).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENT.value
        })
    }

    @Test
    fun `retrySendMessage() should update local cache with send status SENDING on failure`() = runBlockingTest(testDispatcher) {
        val message = createTestMessageDataItem(body = "test message", uuid = "uuid",
            author = participantIdentity, sendStatus = SendStatus.ERROR.value)
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        coEvery { conversationsClient.getConversation(any(), any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        try {
            messageListManager.retrySendTextMessage(message.uuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository).updateMessageByUuid(argThat {
            message.body == body && message.uuid == uuid && sendStatus == SendStatus.SENDING.value
        })
    }

    @Test
    fun `sendMediaMessage() should update local cache with send status SENT on success`() = runBlockingTest {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem()
        every { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        messageListManager.sendMediaMessage(mediaUri, mockk(), fileName, mimeType, messageUuid)

        verify(conversationsRepository).insertMessage(argThat {
            type == MessageType.MEDIA.value
                    && body == null
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENDING.value
                    && mediaFileName == fileName
                    && mediaUploadUri == mediaUri
                    && mediaType == mimeType
        })
    }

    @Test
    fun `sendMediaMessage() should update local cache with send status SENDING on failure`() = runBlockingTest {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem()
        every { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        try {
            messageListManager.sendMediaMessage(mediaUri, mockk(), fileName, mimeType, messageUuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository).insertMessage(argThat {
            type == MessageType.MEDIA.value
                    && body == null
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENDING.value
                    && mediaFileName == fileName
                    && mediaUploadUri == mediaUri
                    && mediaType == mimeType
        })

        verify(conversationsRepository, never()).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == null
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENT.value
                    && mediaFileName == fileName
                    && mediaUploadUri == mediaUri
                    && mediaType == mimeType
        })
    }

    @Test
    fun `sendMediaMessage() should NOT update local cache with on participant failure`() = runBlockingTest {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem()
        every { participant.sid } returns message.participantSid
        coEvery { conversation.getParticipantByIdentity(any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        try {
            messageListManager.sendMediaMessage(mediaUri, mockk(), fileName, mimeType, messageUuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository, never()).insertMessage(argThat {
            type == MessageType.MEDIA.value
                    && body == null
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENDING.value
                    && mediaFileName == fileName
                    && mediaUploadUri == mediaUri
                    && mediaType == mimeType
        })

        verify(conversationsRepository, never()).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == null
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENT.value
                    && mediaFileName == fileName
                    && mediaUploadUri == mediaUri
                    && mediaType == mimeType
        })
    }

    @Test
    fun `retrySendMediaMessage() should update local cache with send status SENT on success`() = runBlockingTest(testDispatcher) {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem(uuid = messageUuid, author = participantIdentity,
            sendStatus = SendStatus.ERROR.value, mediaUploadUri = mediaUri, mediaFileName = fileName,
            mediaType = mimeType, type = MessageType.MEDIA.value, mediaSid = "sid")
        every { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        messageListManager.retrySendMediaMessage(mockk(), message.uuid)

        inOrder(conversationsRepository).verify(conversationsRepository).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == ""
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENDING.value
                    && mediaFileName == fileName
                    && mediaType == mimeType
        })
        inOrder(conversationsRepository).verify(conversationsRepository).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == ""
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENT.value
                    && mediaFileName == fileName
                    && mediaType == mimeType
        })
    }

    @Test
    fun `retrySendMediaMessage() should NOT update local cache if already sending`() = runBlockingTest(testDispatcher) {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem(uuid = messageUuid, author = participantIdentity,
            sendStatus = SendStatus.SENDING.value, mediaUploadUri = mediaUri, mediaFileName = fileName,
            mediaType = mimeType, type = MessageType.MEDIA.value, mediaSid = "sid")
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        messageListManager.retrySendMediaMessage(mockk(), message.uuid)

        verify(conversationsRepository, times(0)).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == ""
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENT.value
                    && mediaFileName == fileName
                    && mediaType == mimeType
        })
    }

    @Test
    fun `retrySendMediaMessage() should update local cache with send status SENDING on failure`() = runBlockingTest(testDispatcher) {
        val messageUuid = "uuid"
        val mediaUri = "uri"
        val fileName = "fileName"
        val mimeType = "mimeType"
        val message = createTestMessageDataItem(uuid = messageUuid, author = participantIdentity,
            sendStatus = SendStatus.ERROR.value, mediaUploadUri = mediaUri, mediaFileName = fileName,
            mediaType = mimeType, type = MessageType.MEDIA.value, mediaSid = "sid")
        coEvery { participant.sid } returns message.participantSid
        coEvery { conversation.sendMessage(any()) } returns message.toMessageMock(participant)
        coEvery { conversationsClient.getConversation(any(), any()) } throws createTwilioException(ConversationsError.MESSAGE_SEND_FAILED)
        whenCall(conversationsRepository.getMessageByUuid(message.uuid)).thenReturn(message)
        try {
            messageListManager.retrySendMediaMessage(mockk(), message.uuid)
        } catch (e: TwilioException) {
            assert(ConversationsError.MESSAGE_SEND_FAILED == e.toConversationsError())
        }

        verify(conversationsRepository).updateMessageByUuid(argThat {
            type == MessageType.MEDIA.value
                    && body == ""
                    && uuid == messageUuid
                    && sendStatus == SendStatus.SENDING.value
                    && mediaFileName == fileName
                    && mediaType == mimeType
        })
    }

    @Test
    fun `setMessageMediaDownloadId should update repository`() = runBlockingTest {
        val messageIndex = 1L
        val downloadId = 2L
        val messageSid = "sid"
        val message = mockk<Message>()
        every { message.sid } returns messageSid
        coEvery { conversation.getMessageByIndex(messageIndex) } returns message

        messageListManager.setMessageMediaDownloadId(messageIndex, downloadId)

        verify { conversationsRepository.updateMessageMediaDownloadStatus(messageSid = message.sid, downloadId = downloadId)}
    }

    @Test
    fun `getMediaContentTemporaryUrl returns Media getTemporaryContentUrl`() = runBlockingTest {
        val messageIndex = 1L
        val mediaTempUrl = "url"
        val message = mockk<Message>()
        val media = mockk<Media>()
        coEvery { conversation.getMessageByIndex(messageIndex, any()) } returns message
        every { message.firstMedia } returns media
        coEvery { media.getTemporaryContentUrl(any()) } returns mediaTempUrl

        assertEquals(mediaTempUrl, messageListManager.getMediaContentTemporaryUrl(messageIndex))
    }
}
