package com.twilio.conversations.app.viewModel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.twilio.conversations.Conversation
import com.twilio.conversations.User
import com.twilio.conversations.app.USER_CONVERSATION_COUNT
import com.twilio.conversations.app.common.asConversationListViewItems
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.createTestConversationDataItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.getMockedConversations
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.manager.UserManager
import com.twilio.conversations.app.repository.ConversationsRepository
import com.twilio.conversations.app.testUtil.CoroutineTestRule
import com.twilio.conversations.app.testUtil.createUserMock
import com.twilio.conversations.app.testUtil.toConversationMock
import com.twilio.conversations.app.testUtil.waitCalled
import com.twilio.conversations.app.testUtil.waitNotCalled
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

@RunWith(PowerMockRunner::class)
@PrepareForTest(
    Conversation::class,
    User::class
)
class ConversationListViewModelTest {

    @Rule
    var coroutineTestRule = CoroutineTestRule()

    @Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var conversationListManager: ConversationListManager

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var loginManager: LoginManager

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.COMPLETE))
        coEvery { conversationsRepository.getSelfUser() } returns flowOf(createUserMock())
    }

    @Test
    fun `conversationListViewModel_userConversationDataItems() should contain all user conversations stored in local cache`() = runBlocking {
        val expectedConversations = getMockedConversations(USER_CONVERSATION_COUNT, "User Conversations").toList()

        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(expectedConversations, RepositoryRequestStatus.COMPLETE))

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(expectedConversations.asConversationListViewItems()))
        assertEquals(USER_CONVERSATION_COUNT, conversationListViewModel.userConversationItems.waitValue().size)
    }

    @Test
    fun `conversationListViewModel_filteredUserConversationItems should contain only filtered items`() = runBlocking {
        val conversationAbc = createTestConversationDataItem(friendlyName = "abc")
        val conversationBcd = createTestConversationDataItem(friendlyName = "bcd")
        val conversationCde = createTestConversationDataItem(friendlyName = "cde")
        val expectedConversations = listOf(conversationAbc, conversationBcd, conversationCde)
        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(expectedConversations, RepositoryRequestStatus.COMPLETE))
        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)

        conversationListViewModel.conversationFilter = "c"
        assertEquals(3, conversationListViewModel.userConversationItems.waitValue().size)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(expectedConversations.asConversationListViewItems()))

        conversationListViewModel.conversationFilter = "b"
        assertEquals(2, conversationListViewModel.userConversationItems.waitValue().size)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(listOf(conversationAbc, conversationBcd).asConversationListViewItems()))

        conversationListViewModel.conversationFilter = "a"
        assertEquals(1, conversationListViewModel.userConversationItems.waitValue().size)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(listOf(conversationAbc).asConversationListViewItems()))

        conversationListViewModel.conversationFilter = ""
        assertEquals(3, conversationListViewModel.userConversationItems.waitValue().size)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(expectedConversations.asConversationListViewItems()))
    }

    @Test
    fun `conversationListViewModel_filteredUserConversationItems should ignore filter case`() = runBlocking {
        val namePrefix = "User Conversations"
        val expectedConversations = getMockedConversations(USER_CONVERSATION_COUNT, namePrefix).toList()
        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(expectedConversations, RepositoryRequestStatus.COMPLETE))
        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)

        // When the filter string matches all conversation names but is in uppercase
        conversationListViewModel.conversationFilter = namePrefix.toUpperCase()

        // Then verify that all conversations match the filter
        assertEquals(USER_CONVERSATION_COUNT, conversationListViewModel.userConversationItems.waitValue().size)
        assertTrue(conversationListViewModel.userConversationItems.waitValue(expectedConversations.asConversationListViewItems()))
    }

    @Test
    fun `conversationListViewModel_userConversationDataItems() should be empty when Error occurred`() = runBlocking {
        coEvery { conversationsRepository.getUserConversations() } returns
                flowOf(RepositoryResult(listOf(), RepositoryRequestStatus.Error(ConversationsError.GENERIC_ERROR)))

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        assertEquals(0, conversationListViewModel.userConversationItems.waitValue().size)
    }

    @Test
    fun `conversationListViewModel_createConversation() should call onConversationCreated on success`() = runBlocking {
        val conversationName = "Private Conversation"
        val conversationMock = createTestConversationDataItem(
            friendlyName = conversationName,
            uniqueName = conversationName,
        ).toConversationMock()

        coEvery { conversationListManager.createConversation(conversationName)} returns conversationMock.sid
        coEvery { conversationListManager.joinConversation(conversationMock.sid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.createConversation(conversationName)

        assertTrue(conversationListViewModel.isDataLoading.waitCalled())
        assertTrue(conversationListViewModel.onConversationCreated.waitCalled())
        assertTrue(conversationListViewModel.onConversationJoined.waitCalled())
        coVerify { conversationListManager.joinConversation(any()) }
        assertTrue(conversationListViewModel.isDataLoading.waitCalled())
    }

    @Test
    fun `conversationListViewModel_createConversation() should call onConversationError on failure`() = runBlocking {
        val conversationName = "Private Conversation"

        coEvery { conversationListManager.createConversation(conversationName)} throws ConversationsException(ConversationsError.CONVERSATION_CREATE_FAILED)
        coEvery { conversationListManager.joinConversation(any()) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.createConversation(conversationName)

        assertTrue(conversationListViewModel.isDataLoading.waitCalled())
        coVerify(exactly = 0) { conversationListManager.joinConversation(any()) }
        assertTrue(conversationListViewModel.onConversationCreated.waitNotCalled())
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_CREATE_FAILED))
        assertTrue(conversationListViewModel.isDataLoading.waitCalled())
    }

    @Test
    fun `conversationListViewModel_joinConversation() should call onConversationJoined on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.joinConversation(conversationSid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.joinConversation(conversationSid)

        coVerify { conversationListManager.joinConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationJoined.waitCalled())
    }

    @Test
    fun `conversationListViewModel_joinConversation() should call onConversationError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.joinConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_JOIN_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.joinConversation(conversationSid)

        coVerify { conversationListManager.joinConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationJoined.waitNotCalled())
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_JOIN_FAILED))
    }

    @Test
    fun `conversationListViewModel_muteConversation() should call onConversationError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.muteConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_MUTE_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.muteConversation(conversationSid)

        coVerify { conversationListManager.muteConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationMuted.waitNotCalled())
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_MUTE_FAILED))
    }

    @Test
    fun `conversationListViewModel_muteConversation() should call onConversationJoined on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.muteConversation(conversationSid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.muteConversation(conversationSid)

        coVerify { conversationListManager.muteConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationMuted.waitValue(true))
    }

    @Test
    fun `conversationListViewModel_unmuteConversation() should call onConversationError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.unmuteConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_UNMUTE_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.unmuteConversation(conversationSid)

        coVerify { conversationListManager.unmuteConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationMuted.waitNotCalled())
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_UNMUTE_FAILED))
    }

    @Test
    fun `conversationListViewModel_unmuteConversation() should call onConversationJoined on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.unmuteConversation(conversationSid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.unmuteConversation(conversationSid)

        coVerify { conversationListManager.unmuteConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationMuted.waitValue(false))
    }

    @Test
    fun `conversationListViewModel_leaveConversation() should call onConversationLeft on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.leaveConversation(conversationSid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.leaveConversation(conversationSid)

        coVerify { conversationListManager.leaveConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationLeft.waitCalled())
    }

    @Test
    fun `conversationListViewModel_leaveConversation() should call onConversationError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.leaveConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_LEAVE_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.leaveConversation(conversationSid)

        coVerify { conversationListManager.leaveConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationLeft.waitNotCalled())
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_LEAVE_FAILED))
    }

    @Test
    fun `conversationListViewModel_removeConversation() should call onConversationRemoved on success`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.removeConversation(conversationSid) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.removeConversation(conversationSid)

        coVerify { conversationListManager.removeConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationRemoved.waitCalled())
    }

    @Test
    fun `conversationListViewModel_removeConversation() should call onConversationError on failure`() = runBlocking {
        val conversationSid = "sid"
        coEvery { conversationListManager.removeConversation(conversationSid) } throws ConversationsException(ConversationsError.CONVERSATION_REMOVE_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.removeConversation(conversationSid)

        coVerify { conversationListManager.removeConversation(conversationSid) }
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.CONVERSATION_REMOVE_FAILED))
    }

    @Test
    fun `conversationListViewModel_setFriendlyName() should call onUserUpdated on success`() = runBlocking {
        val friendlyName = "friendly name"
        coEvery { userManager.setFriendlyName(friendlyName) } returns Unit

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.setFriendlyName(friendlyName)

        coVerify { userManager.setFriendlyName(friendlyName) }
        assertTrue(conversationListViewModel.onUserUpdated.waitCalled())
    }

    @Test
    fun `conversationListViewModel_setFriendlyName() should call onConversationError on failure`() = runBlocking {
        val friendlyName = "friendly name"
        coEvery { userManager.setFriendlyName(friendlyName) } throws ConversationsException(ConversationsError.USER_UPDATE_FAILED)

        val conversationListViewModel = ConversationListViewModel(conversationsRepository, conversationListManager, userManager, loginManager)
        conversationListViewModel.setFriendlyName(friendlyName)

        coVerify { userManager.setFriendlyName(friendlyName) }
        assertTrue(conversationListViewModel.onConversationError.waitValue(ConversationsError.USER_UPDATE_FAILED))
    }
}
