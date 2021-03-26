package com.twilio.conversations.app.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.rule.ActivityTestRule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.Conversation
import com.twilio.conversations.app.*
import com.twilio.conversations.app.adapters.ConversationListAdapter
import com.twilio.conversations.app.common.asConversationListViewItems
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.setupTestInjector
import com.twilio.conversations.app.common.testInjector
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.entity.ConversationDataItem
import com.twilio.conversations.app.data.models.ConversationListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.testUtil.WaitForViewMatcher
import com.twilio.conversations.app.testUtil.atPosition
import com.twilio.conversations.app.testUtil.closeKeyboard
import com.twilio.conversations.app.testUtil.waitUntilPopupStateChanged
import com.twilio.conversations.app.viewModel.ConversationListViewModel
import kotlinx.android.synthetic.main.activity_conversations_list.*
import kotlinx.android.synthetic.main.fragment_conversations_list.*
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationListActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<ConversationListActivity> = ActivityTestRule(ConversationListActivity::class.java)

    private lateinit var conversationListViewModel: ConversationListViewModel
    private val userConversationCount = 5

    @Before
    fun setUp() {
        ConversationsClientWrapper.recreateInstance()
        conversationListViewModel = activityRule.activity.conversationsListViewModel
    }

    @Test
    fun userConversationsVisible() {
        val conversations = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME)
        updateAndValidateUserConversations(conversations)
    }

    @Test
    fun userConversationsChanged() {
        // Given a list of user conversations
        val conversations: MutableList<ConversationDataItem> = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME)
        updateAndValidateUserConversations(conversations)

        // .. when new conversation is added
        val newConversation = createTestConversationDataItem(
            friendlyName = "New User Conversation",
            participantsCount = 10,
            messagesCount = 99,
            unreadMessagesCount = 789
        )
        conversations.add(newConversation)
        // .. then conversation list is updated
        updateAndValidateUserConversations(conversations)

        // .. when a conversation is removed
        conversations.remove(newConversation)
        // .. then conversation list is updated
        updateAndValidateUserConversations(conversations)
    }

    @Test
    fun userConversationAdded() {
        WaitForViewMatcher.performOnView(withId(R.id.show_conversation_add), click())

        BottomSheetBehavior.from(activityRule.activity.add_conversation_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(withId(R.id.conversation_name_input), replaceText(MOCK_USER_CONVERSATION_NAME), closeKeyboard())
        WaitForViewMatcher.performOnView(withId(R.id.add_conversation_button), click())
    }

    @Test
    fun userConversationRemoved() {
        val conversations = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME, Conversation.ConversationStatus.JOINED)
        val conversationToRemove = conversations[conversations.size - 1]
        updateAndValidateUserConversations(conversations)

        WaitForViewMatcher.performOnView(allOf(
                withId(R.id.conversationItem),
                hasDescendant(allOf(
                    withId(R.id.conversationName),
                    withText(conversationToRemove.friendlyName)
                ))
            ), longClick())

        // Espresso click() will work while the bottom sheet is animating and not trigger the click listener
        // UI tests should be run with disabled animations otherwise we have to sleep here to trigger the listener
        BottomSheetBehavior.from(activityRule.activity.removeConversationSheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.remove_conversation_button),
            isDisplayed()
        ), click())

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationRemoved.verifyCalled()
            conversations.remove(conversationToRemove)
        }
        updateAndValidateUserConversations(conversations)
    }

    @Test
    fun userConversationLeft() {
        val conversations = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME, Conversation.ConversationStatus.JOINED)
        val conversationToRemove = conversations[conversations.size - 1]
        updateAndValidateUserConversations(conversations)

        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.conversationItem),
            hasDescendant(allOf(
                withId(R.id.conversationName),
                withText(conversationToRemove.friendlyName)
            ))
        ), longClick())

        BottomSheetBehavior.from(activityRule.activity.removeConversationSheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.leave_conversation_button),
            isDisplayed()
        ), click())

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationLeft.verifyCalled()
            conversations.remove(conversationToRemove)
        }
        updateAndValidateUserConversations(conversations)
    }

    @Test
    fun userConversationMuted() {
        val conversations = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME, Conversation.ConversationStatus.JOINED)
        val conversationToMute = conversations[conversations.size - 1]
        updateAndValidateUserConversations(conversations)

        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.conversationMute),
            withParent(allOf(
                withId(R.id.conversationItem),
                hasDescendant(allOf(
                    withId(R.id.conversationName),
                    withText(conversationToMute.friendlyName)
                ))))
        ), click())

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationMuted.awaitValue(true)
        }
    }

    @Test
    fun userConversationUnmuted() {
        val conversations = getMockedConversations(userConversationCount, MOCK_USER_CONVERSATION_NAME,
            Conversation.ConversationStatus.JOINED, Conversation.NotificationLevel.MUTED)
        val conversationToMute = conversations[conversations.size - 1]
        updateAndValidateUserConversations(conversations)

        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.conversationMute),
            withParent(allOf(
                withId(R.id.conversationItem),
                hasDescendant(allOf(
                    withId(R.id.conversationName),
                    withText(conversationToMute.friendlyName)
                ))))
        ), click())

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationMuted.awaitValue(false)
        }
    }

    @Test
    fun userConversationFetchFailed() {
        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
        }

        onView(withText(R.string.err_failed_to_fetch_user_conversations)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun userConversationFilter() {
        val conversationAbc = createTestConversationDataItem(friendlyName = "abc")
        val conversationBcd = createTestConversationDataItem(friendlyName = "bcd")
        val conversationCde = createTestConversationDataItem(friendlyName = "cde")
        val conversations = listOf(conversationAbc, conversationBcd, conversationCde)
        testInjector.userConversationRepositoryResult = flowOf(RepositoryResult(conversations,
            RepositoryRequestStatus.COMPLETE))
        conversationListViewModel.getUserConversations()

        onView(withId(R.id.filter_conversations)).perform(click())
        onView(withId(R.id.search_src_text)).perform(typeTextIntoFocusedView("d"), closeKeyboard())
        Espresso.pressBack()

        validateConversationItems(listOf(conversationBcd, conversationCde).asConversationListViewItems())
    }

    @Test
    fun userFriendlyNameChanged() {
        val identity = "identity"
        val friendlyName1 = "friendly name 1"
        val friendlyName2 = "friendly name 2"

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.selfUser.value = createTestUserViewItem(friendlyName = friendlyName1, identity = identity)
        }

        WaitForViewMatcher.performOnView(withId(R.id.conversation_drawer_layout), DrawerActions.open())
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.drawer_participant_name), withText(friendlyName1), isDisplayed()))
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.drawer_participant_info), withText(identity), isDisplayed()))
        WaitForViewMatcher.performOnView(withId(R.id.drawer_settings_button), click())

        BottomSheetBehavior.from(activityRule.activity.user_profile_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.user_profile_friendly_name), withText(friendlyName1), isDisplayed()))
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.user_profile_identity), withText(identity), isDisplayed()))
        WaitForViewMatcher.performOnView(withId(R.id.user_profile_friendly_name), replaceText(friendlyName2), pressImeActionButton())
        WaitForViewMatcher.performOnView(withId(R.id.user_profile_update_button), scrollTo(), click())

        UiThreadStatement.runOnUiThread {
            conversationListViewModel.selfUser.value = createTestUserViewItem(friendlyName = friendlyName2, identity = identity)
        }

        WaitForViewMatcher.performOnView(withId(R.id.conversation_drawer_layout), DrawerActions.open())
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.drawer_participant_name), withText(friendlyName2), isDisplayed()))
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.drawer_participant_info), withText(identity), isDisplayed()))
        WaitForViewMatcher.performOnView(withId(R.id.drawer_settings_button), click())

        BottomSheetBehavior.from(activityRule.activity.user_profile_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.user_profile_friendly_name), withText(friendlyName2), isDisplayed()))
        WaitForViewMatcher.assertOnView(allOf(withId(R.id.user_profile_identity), withText(identity), isDisplayed()))
    }

    @Test
    fun userFriendlyNameChangeFailed() {
        UiThreadStatement.runOnUiThread {
            conversationListViewModel.onConversationError.value = ConversationsError.USER_UPDATE_FAILED
        }

        onView(withText(R.string.err_failed_to_update_user)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    private fun updateAndValidateUserConversations(conversations: List<ConversationDataItem>) {
        testInjector.userConversationRepositoryResult = flowOf(RepositoryResult(conversations,
            RepositoryRequestStatus.COMPLETE))
        conversationListViewModel.getUserConversations()

        validateConversationItems(conversations.asConversationListViewItems())
    }

    private fun validateConversationItems(conversations: List<ConversationListViewItem>) =
        conversations.forEachIndexed { index, conversation ->
            validateConversationItem(index, conversation)
        }

    private fun validateConversationItem(index: Int, conversation: ConversationListViewItem) {
        // Scroll to correct conversation list position
        WaitForViewMatcher.performOnView(
            allOf(withId(R.id.conversationList), isDisplayed()),
            scrollToPosition<ConversationListAdapter.ViewHolder>(index)
        )

        // Validate conversation item
        WaitForViewMatcher.assertOnView(atPosition(index, allOf(
            // Given the list item
            withId(R.id.conversationItem),
            // Check for correct conversation name
            hasDescendant(allOf(
                allOf(
                    withId(R.id.conversationName),
                    withText(conversation.name)
                ),
                // Check for correct conversation subtitle text
                hasSibling(allOf(
                    withId(R.id.conversationInfo),
                    withText(activityRule.activity.getString(R.string.conversation_info, conversation.participantCount, conversation.dateCreated)))
                )
            )),
            // Check for conversation update time
            hasDescendant(allOf(
                withId(R.id.conversationUpdateTime),
                withText(conversation.dateUpdated)
            )),
            // Check unread message count
            hasDescendant(allOf(
                withId(R.id.conversationUnreadCount),
                isDisplayed(),
                withText(conversation.messageCount)
            ))
        )), matches(isCompletelyDisplayed()))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupInjector() = setupTestInjector()

        const val MOCK_USER_CONVERSATION_NAME = "Test User Conversation"
    }
}
