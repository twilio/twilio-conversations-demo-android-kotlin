package com.twilio.conversations.app.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twilio.conversations.app.common.SingleLiveEvent
import com.twilio.conversations.app.common.asConversationDetailsViewItem
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.data.models.ConversationDetailsViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.manager.ParticipantListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class ConversationDetailsViewModel(
    val conversationSid: String,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    private val participantListManager: ParticipantListManager
) : ViewModel() {

    val conversationDetails = MutableLiveData<ConversationDetailsViewItem>()
    val isShowProgress = MutableLiveData<Boolean>()
    val onDetailsError = SingleLiveEvent<ConversationsError>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationRemoved = SingleLiveEvent<Unit>()
    val onConversationRenamed = SingleLiveEvent<Unit>()
    val onParticipantAdded = SingleLiveEvent<String>()

    init {
        Timber.d("init: $conversationSid")
        viewModelScope.launch {
            getConversationResult()
        }
    }

    private suspend fun getConversationResult() {
        conversationsRepository.getConversation(conversationSid).collect { result ->
            if (result.requestStatus is RepositoryRequestStatus.Error) {
                onDetailsError.value = ConversationsError.CONVERSATION_GET_FAILED
                return@collect
            }
            result.data?.let { conversationDetails.value = it.asConversationDetailsViewItem() }
        }
    }

    private fun setShowProgress(show: Boolean) {
        if (isShowProgress.value != show) {
            isShowProgress.value = show
        }
    }

    fun renameConversation(friendlyName: String) = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        Timber.d("Renaming conversation: $friendlyName")
        try {
            setShowProgress(true)
            conversationListManager.renameConversation(conversationSid, friendlyName)
            onConversationRenamed.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to rename conversation")
            onDetailsError.value = ConversationsError.CONVERSATION_RENAME_FAILED
        } finally {
            setShowProgress(false)
        }
    }

    fun muteConversation() = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        Timber.d("Muting conversation: $conversationSid")
        try {
            setShowProgress(true)
            conversationListManager.muteConversation(conversationSid)
            onConversationMuted.value = true
        } catch (e: ConversationsException) {
            Timber.d("Failed to mute conversation")
            onDetailsError.value = ConversationsError.CONVERSATION_MUTE_FAILED
        } finally {
            setShowProgress(false)
        }
    }

    fun unmuteConversation() = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        Timber.d("Unmuting conversation: $conversationSid")
        try {
            setShowProgress(true)
            conversationListManager.unmuteConversation(conversationSid)
            onConversationMuted.value = false
        } catch (e: ConversationsException) {
            Timber.d("Failed to unmute conversation")
            onDetailsError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
        } finally {
            setShowProgress(false)
        }
    }

    fun removeConversation() = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        Timber.d("Removing conversation: $conversationSid")
        try {
            setShowProgress(true)
            conversationListManager.removeConversation(conversationSid)
            onConversationRemoved.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to remove conversation")
            onDetailsError.value = ConversationsError.CONVERSATION_REMOVE_FAILED
        } finally {
            setShowProgress(false)
        }
    }

    fun addParticipant(identity: String) = viewModelScope.launch {
        if (isShowProgress.value == true) {
            return@launch
        }
        Timber.d("Adding participant: $identity")
        try {
            setShowProgress(true)
            participantListManager.addParticipant(identity)
            onParticipantAdded.value = identity
        } catch (e: ConversationsException) {
            Timber.d("Failed to remove conversation")
            onDetailsError.value = ConversationsError.MEMBER_ADD_FAILED
        } finally {
            setShowProgress(false)
        }
    }

    fun isConversationMuted() = conversationDetails.value?.isMuted == true
}
