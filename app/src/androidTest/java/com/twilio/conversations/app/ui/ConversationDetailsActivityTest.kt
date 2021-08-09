package com.twilio.conversations.app.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.setupTestInjector
import com.twilio.conversations.app.createTestConversationDetailsViewItem
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.testUtil.WaitForViewMatcher
import com.twilio.conversations.app.testUtil.waitUntilPopupStateChanged
import com.twilio.conversations.app.viewModel.ConversationDetailsViewModel
import kotlinx.android.synthetic.main.activity_conversation_details.*
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationDetailsActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<ConversationDetailsActivity> = ActivityTestRule(ConversationDetailsActivity::class.java, false, false)

    private lateinit var conversationDetailsViewModel: ConversationDetailsViewModel

    private val conversationSid = "conversationSid"
    private val participantSid = "participantSid"
    private val participantPhone = "111"
    private val participantProxyPhone = "222"

    @Before
    fun setUp() {
        activityRule.launchActivity(ConversationDetailsActivity.getStartIntent(InstrumentationRegistry.getInstrumentation().targetContext, conversationSid))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ConversationsClientWrapper.recreateInstance(context)
        conversationDetailsViewModel = activityRule.activity.conversationDetailsViewModel
    }

    @Test
    fun addChatParticipantSuccess() {
        WaitForViewMatcher.performOnView(withId(R.id.add_chat_participant_button), click())
        BottomSheetBehavior.from(activityRule.activity.add_chat_participant_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(withId(R.id.add_chat_participant_id_input), replaceText(participantSid), closeSoftKeyboard())
        WaitForViewMatcher.performOnView(withId(R.id.add_chat_participant_id_cancel_button), click())

        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onParticipantAdded.value = participantSid
        }
        onView(withText(activityRule.activity.getString(R.string.participant_added_message, participantSid)))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun addNonChatParticipantSuccess() {
        WaitForViewMatcher.performOnView(withId(R.id.add_non_chat_participant_button), click())
        BottomSheetBehavior.from(activityRule.activity.add_non_chat_participant_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(withId(R.id.add_non_chat_participant_phone_input), replaceText(participantPhone), closeSoftKeyboard())
        WaitForViewMatcher.performOnView(withId(R.id.add_non_chat_participant_proxy_input), replaceText(participantProxyPhone), closeSoftKeyboard())
        WaitForViewMatcher.performOnView(withId(R.id.add_non_chat_participant_id_cancel_button), click())

        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onParticipantAdded.value = participantPhone
        }
        onView(withText(activityRule.activity.getString(R.string.participant_added_message, participantPhone)))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun addParticipantFailed() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onDetailsError.value = ConversationsError.PARTICIPANT_ADD_FAILED
        }
        onView(withText(R.string.err_failed_to_add_participant))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun renameConversationSuccess() {
        val updatedConversationName = "updatedConversationName"
        val conversationAuthor = "UITester"
        val conversationCreatedDate = "23 May 2020"
        WaitForViewMatcher.performOnView(withId(R.id.conversation_rename_button), scrollTo(), click())
        BottomSheetBehavior.from(activityRule.activity.rename_conversation_sheet).waitUntilPopupStateChanged(BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(withId(R.id.rename_conversation_input), replaceText(updatedConversationName), closeSoftKeyboard())
        WaitForViewMatcher.performOnView(withId(R.id.rename_conversation_cancel_button), click())
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.conversationDetails.value = createTestConversationDetailsViewItem(conversationName = updatedConversationName,
                createdBy = conversationAuthor, createdOn = conversationCreatedDate)
        }
        WaitForViewMatcher.assertOnView(allOf(
            withId(R.id.conversation_details_holder),
            hasDescendant(withText(updatedConversationName)),
            hasDescendant(withText(activityRule.activity.getString(R.string.details_created_by, conversationAuthor))),
            hasDescendant(withText(activityRule.activity.getString(R.string.details_created_date, conversationCreatedDate)))
        ), matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun renameConversationFailed() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onDetailsError.value = ConversationsError.CONVERSATION_RENAME_FAILED
        }
        onView(withText(R.string.err_failed_to_rename_conversation))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun muteConversationSuccess() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.conversationDetails.value = createTestConversationDetailsViewItem(isMuted = true)
        }
        WaitForViewMatcher.assertOnView(withText(R.string.details_unmute_conversation), matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun unmuteConversationSuccess() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.conversationDetails.value = createTestConversationDetailsViewItem(isMuted = false)
        }
        onView(withText(R.string.details_mute_conversation))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun muteConversationFailed() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onDetailsError.value = ConversationsError.CONVERSATION_MUTE_FAILED
        }
        onView(withText(R.string.err_failed_to_mute_conversations))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun unmuteConversationFailed() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onDetailsError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
        }
        onView(withText(R.string.err_failed_to_unmute_conversation))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun leaveConversationSuccess() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onConversationLeft.value = Unit
        }
        assert(activityRule.activity.isFinishing)
    }

    @Test
    fun deleteConversationFailed() {
        UiThreadStatement.runOnUiThread {
            conversationDetailsViewModel.onDetailsError.value = ConversationsError.CONVERSATION_REMOVE_FAILED
        }
        onView(withText(R.string.err_failed_to_remove_conversation))
            .check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupInjector() = setupTestInjector()
    }
}
