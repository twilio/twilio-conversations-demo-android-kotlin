package com.twilio.conversations.app.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.Conversation
import com.twilio.conversations.app.common.asConversationDetailsViewItem
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.createTestConversationDataItem
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.manager.ParticipantListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitNotCalled
import com.twilio.conversations.app.testUtil.waitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Conversation::class
)
class ConversationDetailsViewModelTest {

    @Rule
    var coroutineTestRule = CoroutineTestRule()

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val participantIdentity = "participantIdentity"
    private val participantPhone = "participantPhone"
    private val participantProxyPhone = "participantProxyPhone"
    private val participantFriendlyName = "$participantPhone"
    private val conversationSid = "conversationSid"
    private val conversationName = "Test Conversation"
    private val conversationCreator = "User 01"
    private val conversation: ConversationDataItem? = createTestConversationDataItem(sid = conversationSid, friendlyName = conversationName,
        createdBy = conversationCreator)

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var conversationListManager: ConversationListManager

    @MockK
    private lateinit var participantListManager: ParticipantListManager

    private lateinit var conversationDetailsViewModel: ConversationDetailsViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(Dispatchers.Default)
        coEvery { conversationsRepository.getConversation(any()) } returns
                flowOf(RepositoryResult(conversation, RepositoryRequestStatus.COMPLETE))
        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `conversationDetailViewModel_conversationDetails should be initialized on init`() = runBlocking {
        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        assertEquals(conversation?.asConversationDetailsViewItem(), conversationDetailsViewModel.conversationDetails.waitValue())
    }

    @Test
    fun `conversationDetailViewModel_onDetailsError should be called when get conversation failed`() = runBlocking {
        coEvery { conversationsRepository.getConversation(any()) } returns
                flowOf(RepositoryResult(conversation, RepositoryRequestStatus.Error(ConversationsError.CONVERSATION_GET_FAILED)))
        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        assertTrue(conversationDetailsViewModel.onDetailsError.waitValue(ConversationsError.CONVERSATION_GET_FAILED))
    }
    @Test
    fun `conversationDetailViewModel_muteConversation() should call onDetailsError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.muteConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_MUTE_FAILED)

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.muteConversation()

        coVerify { conversationListManager.muteConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationMuted.waitNotCalled())
        assertTrue(conversationDetailsViewModel.onDetailsError.waitValue(ConversationsError.CONVERSATION_MUTE_FAILED))
    }

    @Test
    fun `conversationDetailViewModel_muteConversation() should call onConversationJoined on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.muteConversation(conversationSid) } returns Unit

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.muteConversation()

        coVerify { conversationListManager.muteConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationMuted.waitValue(true))
    }

    @Test
    fun `conversationDetailViewModel_unmuteConversation() should call onDetailsError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.unmuteConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_UNMUTE_FAILED)

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.unmuteConversation()

        coVerify { conversationListManager.unmuteConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationMuted.waitNotCalled())
        assertTrue(conversationDetailsViewModel.onDetailsError.waitValue(ConversationsError.CONVERSATION_UNMUTE_FAILED))
    }

    @Test
    fun `conversationDetailViewModel_unmuteConversation() should call onConversationJoined on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.unmuteConversation(conversationSid) } returns Unit

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.unmuteConversation()

        coVerify { conversationListManager.unmuteConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationMuted.waitValue(false))
    }

    @Test
    fun `conversationDetailViewModel_leaveConversation() should call onConversationLeft on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.leaveConversation(conversationSid) } returns Unit

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.leaveConversation()

        coVerify { conversationListManager.leaveConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationLeft.waitCalled())
    }

    @Test
    fun `conversationDetailViewModel_leaveConversation() should call onDetailsError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.leaveConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_REMOVE_FAILED)

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.leaveConversation()

        coVerify { conversationListManager.leaveConversation(conversationSid) }
        assertTrue(conversationDetailsViewModel.onConversationLeft.waitNotCalled())
        assertTrue(conversationDetailsViewModel.onDetailsError.waitValue(ConversationsError.CONVERSATION_REMOVE_FAILED))
    }

    @Test
    fun `conversationDetailViewModel_renameConversation() should call onConversationRenamed on success`() = runBlocking {
        val conversationSid = "sid"
        val friendlyName = conversationName + "2"
        coEvery { conversationListManager.renameConversation(conversationSid, friendlyName) } returns Unit

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.renameConversation(friendlyName)

        coVerify { conversationListManager.renameConversation(conversationSid, friendlyName) }
        assertTrue(conversationDetailsViewModel.onConversationRenamed.waitCalled())
    }

    @Test
    fun `conversationDetailViewModel_renameConversation() should call onDetailsError on failure`() = runBlocking {
        val conversationSid = "sid"
        val friendlyName = conversationName + "2"
        coEvery { conversationListManager.renameConversation(conversationSid, friendlyName) } throws ConversationsException(ConversationsError.CONVERSATION_RENAME_FAILED)

        conversationDetailsViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailsViewModel.renameConversation(friendlyName)

        coVerify { conversationListManager.renameConversation(conversationSid, friendlyName) }
        assertTrue(conversationDetailsViewModel.onConversationRenamed.waitNotCalled())
        assertTrue(conversationDetailsViewModel.onDetailsError.waitValue(ConversationsError.CONVERSATION_RENAME_FAILED))
    }

    @Test
    fun `conversationDetailViewModel_addChatParticipant() should call onParticipantAdded on success`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.addChatParticipant(participantIdentity) } returns Unit

        val conversationDetailViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailViewModel.addChatParticipant(participantIdentity)

        coVerify { participantListManager.addChatParticipant(participantIdentity) }
        assertTrue(conversationDetailViewModel.onParticipantAdded.waitCalled())
    }

    @Test
    fun `conversationDetailViewModel_addChatParticipant() should call onParticipantError on failure`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.addChatParticipant(participantIdentity) } throws ConversationsException(ConversationsError.PARTICIPANT_ADD_FAILED)

        val conversationDetailViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailViewModel.addChatParticipant(participantIdentity)

        coVerify { participantListManager.addChatParticipant(participantIdentity) }
        assertTrue(conversationDetailViewModel.onDetailsError.waitValue(ConversationsError.PARTICIPANT_ADD_FAILED))
    }

    @Test
    fun `conversationDetailViewModel_addNonChatParticipant() should call onParticipantAdded on success`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.addNonChatParticipant(participantPhone, participantProxyPhone, participantFriendlyName) } returns Unit

        val conversationDetailViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailViewModel.addNonChatParticipant(participantPhone, participantProxyPhone)

        coVerify { participantListManager.addNonChatParticipant(participantPhone, participantProxyPhone, participantFriendlyName) }
        assertTrue(conversationDetailViewModel.onParticipantAdded.waitCalled())
    }

    @Test
    fun `conversationDetailViewModel_addNonChatParticipant() should call onParticipantError on failure`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.addNonChatParticipant(participantPhone, participantProxyPhone, participantFriendlyName) } throws ConversationsException(ConversationsError.PARTICIPANT_ADD_FAILED)

        val conversationDetailViewModel = ConversationDetailsViewModel(conversationSid, conversationsRepository, conversationListManager, participantListManager)
        conversationDetailViewModel.addNonChatParticipant(participantPhone, participantProxyPhone)

        coVerify { participantListManager.addNonChatParticipant(participantPhone, participantProxyPhone, participantFriendlyName) }
        assertTrue(conversationDetailViewModel.onDetailsError.waitValue(ConversationsError.PARTICIPANT_ADD_FAILED))
    }
}
