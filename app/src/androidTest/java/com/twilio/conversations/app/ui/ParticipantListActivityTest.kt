package com.twilio.conversations.app.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.*
import com.twilio.conversations.app.adapters.ConversationListAdapter
import com.twilio.conversations.app.common.asParticipantListViewItems
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.setupTestInjector
import com.twilio.conversations.app.common.testInjector
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.ParticipantListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.testUtil.WaitForViewMatcher
import com.twilio.conversations.app.testUtil.atPosition
import com.twilio.conversations.app.testUtil.waitUntilPopupStateChanged
import com.twilio.conversations.app.viewModel.ParticipantListViewModel
import kotlinx.android.synthetic.main.activity_participants.*
import kotlinx.coroutines.flow.flowOf
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ParticipantListActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<ParticipantListActivity> = ActivityTestRule(ParticipantListActivity::class.java, false, false)

    private lateinit var participantListViewModel: ParticipantListViewModel

    private val conversationSid = "conversationSid"
    private val participantName = "participant"

    @Before
    fun setUp() {
        activityRule.launchActivity(ParticipantListActivity.getStartIntent(InstrumentationRegistry.getInstrumentation().targetContext, conversationSid))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ConversationsClientWrapper.recreateInstance(context)
        participantListViewModel = activityRule.activity.participantListViewModel
    }

    @Test
    fun participantsListVisible() {
        val participants = getMockedParticipants(PARTICIPANT_COUNT, participantName)
        updateAndValidateParticipantsList(participants)
    }

    @Test
    fun participantListChanged() {
        // Given a list of user participants
        val participants: MutableList<ParticipantDataItem> = getMockedParticipants(PARTICIPANT_COUNT, participantName)
        updateAndValidateParticipantsList(participants)

        // .. when new participant is added
        val newParticipant = createTestParticipantDataItem(friendlyName = "New Participant")
        participants.add(newParticipant)
        // .. then participant list is updated
        updateAndValidateParticipantsList(participants)

        // .. when a participant is removed
        participants.remove(newParticipant)
        // .. then participant list is updated
        updateAndValidateParticipantsList(participants)
    }

    @Test
    fun participantFetchFailed() {
        UiThreadStatement.runOnUiThread {
            participantListViewModel.onParticipantError.value = ConversationsError.PARTICIPANTS_FETCH_FAILED
        }

        onView(withText(R.string.err_failed_to_fetch_participants)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
    }

    @Test
    fun participantListFilter() {
        val participantAbc = createTestParticipantDataItem(friendlyName = "abc")
        val participantBcd = createTestParticipantDataItem(friendlyName = "bcd")
        val participantCde = createTestParticipantDataItem(friendlyName = "cde")
        val participants = listOf(participantAbc, participantBcd, participantCde)
        testInjector.participantRepositoryResult = flowOf(RepositoryResult(participants, RepositoryRequestStatus.COMPLETE))
        participantListViewModel.getConversationParticipants()

        onView(withId(R.id.filter_participants)).perform(click())
        onView(withId(R.id.search_src_text)).perform(typeTextIntoFocusedView("d"))

        validateParticipantItems(listOf(participantBcd, participantCde).asParticipantListViewItems())
    }

    @Test
    fun participantRemoved() {
        val participants: MutableList<ParticipantDataItem> = getMockedParticipants(PARTICIPANT_COUNT, participantName)
        val participantToRemove = participants[participants.size - 1]
        updateAndValidateParticipantsList(participants)

        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.participant_item),
            hasDescendant(allOf(
                withId(R.id.participant_name),
                withText(participantToRemove.friendlyName)
            ))
        ), click())

        // Espresso click() will work while the bottom sheet is animating and not trigger the click listener
        // UI tests should be run with disabled animations otherwise we have to sleep here to trigger the listener
        BottomSheetBehavior.from(activityRule.activity.participant_details_sheet).waitUntilPopupStateChanged(
            BottomSheetBehavior.STATE_EXPANDED)
        WaitForViewMatcher.performOnView(allOf(
            withId(R.id.participant_details_remove),
            isDisplayed()
        ), click())

        UiThreadStatement.runOnUiThread {
            participantListViewModel.onParticipantRemoved.verifyCalled()
            participants.remove(participantToRemove)
        }
        updateAndValidateParticipantsList(participants)
    }

    private fun updateAndValidateParticipantsList(participants: List<ParticipantDataItem>) {
        testInjector.participantRepositoryResult = flowOf(RepositoryResult(participants, RepositoryRequestStatus.COMPLETE))
        participantListViewModel.getConversationParticipants()

        validateParticipantItems(participants.asParticipantListViewItems())
    }

    private fun validateParticipantItems(participants: List<ParticipantListViewItem>) =
        participants.forEachIndexed { index, participant ->
            validateConversationItem(index, participant)
        }

    private fun validateConversationItem(index: Int, participant: ParticipantListViewItem) {
        // Scroll to correct participant list position
        WaitForViewMatcher.performOnView(
            allOf(withId(R.id.participantList), isDisplayed()),
            scrollToPosition<ConversationListAdapter.ViewHolder>(index)
        )

        // Validate participant item
        WaitForViewMatcher.assertOnView(atPosition(index, allOf(
            // Given the list item
            withId(R.id.participant_item),
            // Check for correct participant name
            hasDescendant(allOf(
                allOf(
                    withId(R.id.participant_name),
                    withText(participant.friendlyName)
                )
            ))
        )), matches(isCompletelyDisplayed()))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupInjector() = setupTestInjector()
    }
}
