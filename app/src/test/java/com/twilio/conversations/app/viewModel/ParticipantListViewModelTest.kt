package com.twilio.conversations.app.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.Participant
import com.twilio.conversations.app.PARTICIPANT_COUNT
import com.twilio.conversations.app.common.asParticipantListViewItems
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.createTestParticipantDataItem
import com.twilio.conversations.app.createTestParticipantListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.getMockedParticipants
import com.twilio.conversations.app.manager.ParticipantListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitValue
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import java.util.Locale

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Participant::class
)
class ParticipantListViewModelTest {

    private val conversationSid = "conversationSid"
    private val participant = createTestParticipantListViewItem(sid = "sid")

    @Rule
    var coroutineTestRule = CoroutineTestRule()

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var participantListManager: ParticipantListManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `participantListViewModel_participantsList should contain all participants stored in local cache`() = runBlocking {
        val expectedParticipants = getMockedParticipants(PARTICIPANT_COUNT, "User Conversations").toList()
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(expectedParticipants, RepositoryRequestStatus.COMPLETE))
        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)
        assertTrue(participantListViewModel.participantsList.waitValue(expectedParticipants.asParticipantListViewItems()))
        assertEquals(PARTICIPANT_COUNT, participantListViewModel.participantsList.waitValue().size)
    }

    @Test
    fun `participantListViewModel_participantsList should contain only filtered items`() = runBlocking {
        val participantAbc = createTestParticipantDataItem(friendlyName = "abc")
        val participantBcd = createTestParticipantDataItem(friendlyName = "bcd")
        val participantCde = createTestParticipantDataItem(friendlyName = "cde")
        val expectedParticipants = listOf(participantAbc, participantBcd, participantCde)
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(expectedParticipants, RepositoryRequestStatus.COMPLETE))
        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)

        participantListViewModel.participantFilter = "c"
        assertEquals(3, participantListViewModel.participantsList.waitValue().size)
        assertTrue(participantListViewModel.participantsList.waitValue(expectedParticipants.asParticipantListViewItems()))

        participantListViewModel.participantFilter = "b"
        assertEquals(2, participantListViewModel.participantsList.waitValue().size)
        assertTrue(participantListViewModel.participantsList.waitValue(listOf(participantAbc, participantBcd).asParticipantListViewItems()))

        participantListViewModel.participantFilter = "a"
        assertEquals(1, participantListViewModel.participantsList.waitValue().size)
        assertTrue(participantListViewModel.participantsList.waitValue(listOf(participantAbc).asParticipantListViewItems()))

        participantListViewModel.participantFilter = ""
        assertEquals(3, participantListViewModel.participantsList.waitValue().size)
        assertTrue(participantListViewModel.participantsList.waitValue(expectedParticipants.asParticipantListViewItems()))
    }

    @Test
    fun `participantListViewModel_filteredUserConversationItems should ignore filter case`() = runBlocking {
        val namePrefix = "User Conversations"
        val expectedParticipants = getMockedParticipants(PARTICIPANT_COUNT, namePrefix).toList()
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(expectedParticipants, RepositoryRequestStatus.COMPLETE))
        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)

        // When the filter string matches all conversation names but is in uppercase
        participantListViewModel.participantFilter = namePrefix.uppercase(Locale.getDefault())

        // Then verify that all conversations match the filter
        assertEquals(PARTICIPANT_COUNT, participantListViewModel.participantsList.waitValue().size)
        assertTrue(participantListViewModel.participantsList.waitValue(expectedParticipants.asParticipantListViewItems()))
    }

    @Test
    fun `participantListViewModel_participantList should be empty when Error occurred`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.Error(ConversationsError.GENERIC_ERROR)))

        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)
        assertEquals(0, participantListViewModel.participantsList.waitValue().size)
    }

    @Test
    fun `participantListViewModel_removeParticipant() should call onParticipantRemoved on success`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.removeParticipant(participant.sid) } returns Unit

        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)
        participantListViewModel.selectedParticipant = participant
        participantListViewModel.removeSelectedParticipant()

        coVerify { participantListManager.removeParticipant(participant.sid) }
        assertTrue(participantListViewModel.onParticipantRemoved.waitCalled())
    }

    @Test
    fun `participantListViewModel_removeParticipant() should call onParticipantError on failure`() = runBlocking {
        coEvery { conversationsRepository.getConversationParticipants(any()) } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { participantListManager.removeParticipant(participant.sid) } throws ConversationsException(ConversationsError.PARTICIPANT_REMOVE_FAILED)

        val participantListViewModel = ParticipantListViewModel(conversationSid, conversationsRepository, participantListManager)
        participantListViewModel.selectedParticipant = participant
        participantListViewModel.removeSelectedParticipant()

        coVerify { participantListManager.removeParticipant(participant.sid) }
        assertTrue(participantListViewModel.onParticipantError.waitValue(ConversationsError.PARTICIPANT_REMOVE_FAILED))
    }
}
