package com.twilio.conversations.app.viewModel

import androidx.lifecycle.*
import com.twilio.conversations.Conversation
import com.twilio.conversations.app.common.*
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.data.models.ConversationListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.data.models.UserViewItem
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.manager.LoginManager
import com.twilio.conversations.app.manager.UserManager
import com.twilio.conversations.app.repository.ConversationsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import kotlin.properties.Delegates

@ExperimentalCoroutinesApi
@FlowPreview
class ConversationListViewModel(
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    private val userManager: UserManager,
    private val loginManager: LoginManager
) : ViewModel() {

    private val unfilteredUserConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())
    val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())
    val selfUser = MutableLiveData<UserViewItem>()
    val isDataLoading = SingleLiveEvent<Boolean>()
    val onConversationCreated = SingleLiveEvent<Unit>()
    val onConversationRemoved = SingleLiveEvent<Unit>()
    val onConversationJoined = SingleLiveEvent<Unit>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationLeft = SingleLiveEvent<Unit>()
    val onConversationError = SingleLiveEvent<ConversationsError>()
    val onUserUpdated = SingleLiveEvent<Unit>()
    val onSignedOut = SingleLiveEvent<Unit>()
    var selectedConversationSid: String? = null
    var conversationFilter by Delegates.observable("") { _, _, name ->
        userConversationItems.value = unfilteredUserConversationItems.value?.filterByName(name) ?: emptyList()
    }

    init {
        Timber.d("init")

        getUserConversations()
        getSelfUser()

        unfilteredUserConversationItems.observeForever {
            userConversationItems.value = it.filterByName(conversationFilter)
        }
    }

    fun getUserConversations() = viewModelScope.launch {
        conversationsRepository.getUserConversations().collect { (list, status) ->
            Timber.d("UserConversations collected: ${list.size}, ${status}")
            unfilteredUserConversationItems.value = list.asConversationListViewItems().merge(unfilteredUserConversationItems.value)
            if (status is RepositoryRequestStatus.Error) {
                onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
            }
        }
    }

    private fun getSelfUser() = viewModelScope.launch {
        conversationsRepository.getSelfUser().collect { user ->
            Timber.d("Self user collected: ${user.friendlyName}, ${user.identity}")
            selfUser.value = user.asUserViewItem()
        }
    }

    private fun setDataLoading(loading: Boolean) {
        if (isDataLoading.value != loading) {
            isDataLoading.value = loading
        }
    }

    private fun setConversationLoading(conversationSid: String, loading: Boolean) {
        fun ConversationListViewItem.transform() = if (sid == conversationSid) copy(isLoading = loading) else this
        unfilteredUserConversationItems.value = unfilteredUserConversationItems.value?.map { it.transform() }
    }

    private fun isConversationLoading(conversationSid: String): Boolean =
            unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.isLoading == true

    private fun List<ConversationListViewItem>.filterByName(name: String): List<ConversationListViewItem> =
        if (name.isEmpty()) {
            this
        } else {
            filter {
                it.name.contains(name, ignoreCase = true)
            }
        }

    fun createConversation(friendlyName: String) = viewModelScope.launch {
        Timber.d("Creating conversation: $friendlyName")
        try {
            setDataLoading(true)
            val conversation = conversationListManager.createConversation(friendlyName)
            Timber.d("Created conversation: $friendlyName $conversation")
            onConversationCreated.call()
            joinConversation(conversation)
        } catch (e: ConversationsException) {
            Timber.d("Failed to create conversation")
            onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
        } finally {
            setDataLoading(false)
        }
    }

    fun joinConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        Timber.d("Joining conversation: $conversationSid")
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.joinConversation(conversationSid)
            onConversationJoined.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to join conversation")
            onConversationError.value = ConversationsError.CONVERSATION_JOIN_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun muteConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        Timber.d("Muting conversation: $conversationSid")
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.muteConversation(conversationSid)
            onConversationMuted.value = true
        } catch (e: ConversationsException) {
            Timber.d("Failed to mute conversation")
            onConversationError.value = ConversationsError.CONVERSATION_MUTE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun unmuteConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        Timber.d("Unmuting conversation: $conversationSid")
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.unmuteConversation(conversationSid)
            onConversationMuted.value = false
        } catch (e: ConversationsException) {
            Timber.d("Failed to unmute conversation")
            onConversationError.value = ConversationsError.CONVERSATION_UNMUTE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun leaveConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        Timber.d("Leaving conversation: $conversationSid")
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.leaveConversation(conversationSid)
            onConversationLeft.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to remove conversation")
            onConversationError.value = ConversationsError.CONVERSATION_LEAVE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun removeConversation(conversationSid: String) = viewModelScope.launch {
        if (isConversationLoading(conversationSid)) {
            return@launch
        }
        Timber.d("Removing conversation: $conversationSid")
        try {
            setConversationLoading(conversationSid, true)
            conversationListManager.removeConversation(conversationSid)
            onConversationRemoved.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to remove conversation")
            onConversationError.value = ConversationsError.CONVERSATION_REMOVE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }

    fun setFriendlyName(friendlyName: String) = viewModelScope.launch {
        Timber.d("Updating self user: $friendlyName")
        try {
            setDataLoading(true)
            userManager.setFriendlyName(friendlyName)
            Timber.d("Self user updated: $friendlyName")
            onUserUpdated.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to update self user")
            onConversationError.value = ConversationsError.USER_UPDATE_FAILED
        } finally {
            setDataLoading(false)
        }
    }

    fun simulateCrash(where: CrashIn) {
        conversationsRepository.simulateCrash(where)
    }

    fun signOut() = viewModelScope.launch {
        Timber.d("signOut")
        loginManager.signOut()
        onSignedOut.call()
    }

    fun isConversationJoined(conversationSid: String): Boolean =
        unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.participatingStatus == Conversation.ConversationStatus.JOINED.value
    fun isConversationMuted(conversationSid: String): Boolean =
        unfilteredUserConversationItems.value?.find { it.sid == conversationSid }?.isMuted == true
}
