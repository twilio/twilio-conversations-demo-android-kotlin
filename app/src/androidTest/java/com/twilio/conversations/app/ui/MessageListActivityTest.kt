package com.twilio.conversations.app.ui

import android.app.Activity.RESULT_OK
import android.app.Instrumentation.ActivityResult
import android.content.Intent.*
import android.provider.MediaStore.ACTION_IMAGE_CAPTURE
import android.provider.MediaStore.EXTRA_OUTPUT
import android.text.format.Formatter
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.*
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.gson.Gson
import com.twilio.conversations.Message
import com.twilio.conversations.app.*
import com.twilio.conversations.app.adapters.MessageListAdapter
import com.twilio.conversations.app.common.asMessageListViewItems
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.common.setupTestInjector
import com.twilio.conversations.app.common.testInjector
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.entity.ParticipantDataItem
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.RepositoryResult
import com.twilio.conversations.app.testUtil.BottomSheetBehaviorIdlingResource
import com.twilio.conversations.app.testUtil.WaitForViewMatcher
import com.twilio.conversations.app.testUtil.atPosition
import com.twilio.conversations.app.testUtil.removeProgressBarIndeterminateDrawables
import com.twilio.conversations.app.viewModel.MessageListViewModel
import kotlinx.coroutines.channels.sendBlocking
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber

@RunWith(AndroidJUnit4::class)
class MessageListActivityTest {

    private val plainParticipant = ParticipantDataItem(
        identity = "", conversationSid = "", lastReadTimestamp = null,
        lastReadMessageIndex = null, sid = "", friendlyName = "user", isOnline = true
    )

    @get:Rule
    var activityRule: IntentsTestRule<MessageListActivity> = IntentsTestRule(MessageListActivity::class.java, false, false)

    private val conversationSid = "conversationSid"
    private val messageBody = "Test Message"
    private val messageAuthor = "User1"

    private lateinit var messageListViewModel: MessageListViewModel

    @Before
    fun setUp() {
        activityRule.launchActivity(MessageListActivity.getStartIntent(InstrumentationRegistry.getInstrumentation().targetContext, conversationSid))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        ConversationsClientWrapper.recreateInstance(context)
        messageListViewModel = activityRule.activity.messageListViewModel
        removeProgressBarIndeterminateDrawables(activityRule.activity)
    }

    @Test
    fun incomingMessagesDisplayed() {
        val messages = getMockedMessages(MESSAGE_COUNT, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMessagesDisplayed() {
        val messages = getMockedMessages(MESSAGE_COUNT, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, sendStatus = SendStatus.SENT).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMessageRetryDisplayed() {
        val messages = listOf(createTestMessageDataItem(body = messageBody,
            direction = Direction.OUTGOING.value, sendStatus = SendStatus.ERROR.value, author = messageAuthor))
            .asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun typingIndicatorDisplayed() {
        testInjector.typingParticipantsListConversation.offer(listOf(plainParticipant.copy(identity = "user1")))
        onView(withId(R.id.typingIndicator)).check(matches(withText("user1 is typing…")))
    }

    @Test
    fun typingIndicatorMaxLength() {
        testInjector.typingParticipantsListConversation.offer(
            listOf(
                plainParticipant.copy(identity = "user1"),
                plainParticipant.copy(identity = "user2"),
                plainParticipant.copy(identity = "user3"),
                plainParticipant.copy(identity = "user4")
            )
        )
        onView(withId(R.id.typingIndicator)).check(matches(withText("user1, user2, user3, and others are typing…")))
    }

    @Test
    fun incomingMessagesReactionsDisplayed() {
        val reactionAttributes = Reaction.values().asList().getExpectedReactions(listOf("1"))
        val attributes = Gson().toJson(reactionAttributes)
        val messages = getMockedMessages(MESSAGE_COUNT, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun incomingMessagesReactionsAddedAndRemoved() {
        val participantSid = "participant2"
        var reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid))
        var attributes = Gson().toJson(reactionAttributes)
        var message = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)

        reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid, messageAuthor))
        attributes = Gson().toJson(reactionAttributes)
        message = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)

        reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid))
        attributes = Gson().toJson(reactionAttributes)
        message = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)
    }

    @Test
    fun outgoingMessagesReactionsDisplayed() {
        val reactionAttributes = Reaction.values().asList().getExpectedReactions(listOf("1"))
        val attributes = Gson().toJson(reactionAttributes)
        val messages = getMockedMessages(MESSAGE_COUNT, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, attributes, sendStatus = SendStatus.SENT).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMessagesReactionsAddedAndRemoved() {
        val participantSid = "participant2"
        var reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid))
        var attributes = Gson().toJson(reactionAttributes)
        var message = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)

        reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid, messageAuthor))
        attributes = Gson().toJson(reactionAttributes)
        message = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)

        reactionAttributes = listOf(Reaction.HEART).getExpectedReactions(listOf(participantSid))
        attributes = Gson().toJson(reactionAttributes)
        message = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, attributes).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(message.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(message)
    }

    @Test
    fun incomingMediaMessageDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun incomingMediaMessageProgressDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100, mediaDownloading = true,
            mediaDownloadedBytes = 10).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun incomingMediaMessageCompletedDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.INCOMING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100, mediaDownloading = false,
            mediaDownloadedBytes = 100, mediaUri = "file://").asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMediaMessageDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMediaMessageProgressDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100, mediaDownloading = true,
            mediaDownloadedBytes = 10).asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun outgoingMediaMessageCompletedDisplayed() {
        val messages = getMockedMessages(1, messageBody,
            conversationSid, Direction.OUTGOING.value, messageAuthor, type = Message.Type.MEDIA,
            mediaFileName = "test.txt", mediaSize = 100, mediaDownloading = false,
            mediaDownloadedBytes = 100, mediaUri = "file://").asMessageListViewItems()
        testInjector.messageResultConversation.sendBlocking(RepositoryResult(messages.asPagedList(), RepositoryRequestStatus.COMPLETE))

        validateMessageItems(messages)
    }

    @Test
    fun galleryUploadStarted() {
        intending(not(isInternal())).respondWith(ActivityResult(RESULT_OK, null))
        val idlingResource = BottomSheetBehaviorIdlingResource(getBottomSheerBehavior())
        IdlingRegistry.getInstance().register(idlingResource)

        onView(withId(R.id.messageAttachmentButton)).perform(click())
        onView(withId(R.id.attachment_choose_image)).perform(click())

        intended(allOf(
            hasAction(ACTION_CHOOSER),
            hasExtra(`is`(EXTRA_INTENT), allOf(
                hasAction(ACTION_GET_CONTENT),
                hasCategories(setOf(CATEGORY_OPENABLE)),
                hasType("image/*")
            ))
        ))

        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    @Test
    fun imageCaptureStarted() {
        intending(anyIntent()).respondWith(ActivityResult(RESULT_OK, null))
        val idlingResource = BottomSheetBehaviorIdlingResource(getBottomSheerBehavior())
        IdlingRegistry.getInstance().register(idlingResource)

        onView(withId(R.id.messageAttachmentButton)).perform(click())
        onView(withId(R.id.attachment_take_picture)).perform(click())

        intended(allOf(
            hasAction(ACTION_CHOOSER),
            hasExtra(`is`(EXTRA_INTENT), allOf(
                hasAction(ACTION_IMAGE_CAPTURE),
                hasExtraWithKey(EXTRA_OUTPUT)
            ))
        ))

        IdlingRegistry.getInstance().unregister(idlingResource)
    }

    private fun validateMessageItems(messages: List<MessageListViewItem>) =
        messages.forEachIndexed { index, conversation ->
            validateMessageItem(index, conversation)
        }

    private fun validateMessageItem(index: Int, message: MessageListViewItem) {
        Timber.d("Validating message: $index $message")
        // Scroll to correct message list position
        WaitForViewMatcher.performOnView(
            allOf(withId(R.id.messageList), isDisplayed()),
            RecyclerViewActions.scrollToPosition<MessageListAdapter.ViewHolder>(index)
        )

        Timber.d("Validating message item: $index $message")
        // Validate message item
        WaitForViewMatcher.assertOnView(
            allOf(
                // Given the list item
                withId(R.id.message_item),
                // Check for correct message body
                hasDescendant(
                    allOf(
                        allOf(
                            withId(R.id.message_author),
                            withText(message.author)
                        ),
                        // Check for correct message author text
                        hasSibling(
                            allOf(
                                withId(R.id.message_body),
                                withText(message.body)
                            )
                        )
                    )
                )
            ), matches(isCompletelyDisplayed())
        )
        Timber.d("Validated message text: $message")
        // Validate message reactions
        message.reactions.forEach { reaction ->
            Timber.d("Validating reactions: $reaction")
            WaitForViewMatcher.assertOnView(
                atPosition(
                    index, allOf(
                        withId(R.id.message_item),
                        hasDescendant(
                            allOf(
                                withId(R.id.message_reaction_holder),
                                hasDescendant(withText(reaction.key.emoji)),
                                hasDescendant(withText(reaction.value.size.toString()))
                            )
                        )
                    )
                ), matches(isCompletelyDisplayed())
            )
        }
        // Validate media messages
        if (message.type == MessageType.MEDIA) {
            val mediaMatcher = when {
                message.mediaDownloading -> hasDescendant(withId(R.id.attachment_progress))
                message.isDownloaded() -> hasDescendant(withText(R.string.button_open_attachment))
                else -> hasDescendant(withText(R.string.button_download_attachment))
            }
            WaitForViewMatcher.assertOnView(
                atPosition(
                    index, allOf(
                        withId(R.id.message_item),
                        allOf(
                            hasDescendant(
                                allOf(
                                    withId(R.id.attachment_icon),
                                    hasSibling(
                                        allOf(
                                            withId(R.id.attachment_file_name),
                                            withText(message.mediaFileName)
                                        )
                                    ),
                                    hasSibling(
                                        allOf(
                                            withId(R.id.attachment_size),
                                            withText(
                                                Formatter.formatShortFileSize(
                                                    InstrumentationRegistry.getInstrumentation().targetContext,
                                                    message.mediaSize ?: 0
                                                )
                                            )
                                        )
                                    )
                                )
                            ),
                            hasDescendant(
                                allOf(
                                    withId(R.id.attachment_progress),
                                    withEffectiveVisibility(
                                        if (message.mediaDownloading)
                                            Visibility.VISIBLE else Visibility.GONE
                                    )
                                )
                            ),
                            mediaMatcher
                        )
                    )
                ), matches(isCompletelyDisplayed())
            )
        }
        // Validate message send state
        if (message.direction == Direction.OUTGOING) {
            WaitForViewMatcher.assertOnView(
                atPosition(
                    index, allOf(
                        withId(R.id.message_item),
                        allOf(
                            hasDescendant(
                                allOf(
                                    withId(R.id.message_resend),
                                    withEffectiveVisibility(
                                        if (message.sendStatus != SendStatus.SENT)
                                            Visibility.VISIBLE else Visibility.GONE
                                    )
                                )
                            ),
                            hasDescendant(
                                allOf(
                                    withId(R.id.message_resend_icon),
                                    withEffectiveVisibility(
                                        if (message.sendStatus == SendStatus.ERROR)
                                            Visibility.VISIBLE else Visibility.GONE
                                    )
                                )
                            ),
                            hasDescendant(
                                allOf(
                                    withId(R.id.message_resend_spinner),
                                    withEffectiveVisibility(
                                        if (message.sendStatus == SendStatus.SENDING)
                                            Visibility.VISIBLE else Visibility.GONE
                                    )
                                )
                            )

                        )
                    )
                ), matches(isCompletelyDisplayed())
            )
        }
        Timber.d("Validated message: $index $message")
    }

    private fun getBottomSheerBehavior(): BottomSheetBehavior<*> {
        return BottomSheetBehavior.from(activityRule.activity.findViewById(R.id.selectAttachmentSheet))
    }

    companion object {
        @BeforeClass
        @JvmStatic
        fun setupInjector() = setupTestInjector()
    }
}
