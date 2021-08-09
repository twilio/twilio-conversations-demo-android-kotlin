package com.twilio.conversations.app.viewModel

import android.content.Context
import androidx.lifecycle.*
import com.twilio.conversations.app.common.*
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.ConversationsException
import com.twilio.conversations.app.data.models.ConversationListViewItem
import com.twilio.conversations.app.data.models.RepositoryRequestStatus
import com.twilio.conversations.app.manager.ConnectivityMonitor
import com.twilio.conversations.app.manager.ConversationListManager
import com.twilio.conversations.app.repository.ConversationsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import kotlin.properties.Delegates

@ExperimentalCoroutinesApi
@FlowPreview
class ConversationListViewModel(
    private val applicationContext: Context,
    private val conversationsRepository: ConversationsRepository,
    private val conversationListManager: ConversationListManager,
    connectivityMonitor: ConnectivityMonitor
) : ViewModel() {

    private val unfilteredUserConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val userConversationItems = MutableLiveData<List<ConversationListViewItem>>(emptyList())

    val isDataLoading = SingleLiveEvent<Boolean>()
    val isNoResultsFoundVisible = MutableLiveData(false)
    val isNoConversationsVisible = MutableLiveData(false)
    val isNetworkAvailable = connectivityMonitor.isNetworkAvailable.asLiveData(viewModelScope.coroutineContext)

    val onConversationCreated = SingleLiveEvent<Unit>()
    val onConversationLeft = SingleLiveEvent<Unit>()
    val onConversationMuted = SingleLiveEvent<Boolean>()
    val onConversationError = SingleLiveEvent<ConversationsError>()

    var conversationFilter by Delegates.observable("") { _, _, _ -> updateUserConversationItems() }

    init {
        Timber.d("init")

        getUserConversations()

        unfilteredUserConversationItems.observeForever { updateUserConversationItems() }
    }

    private fun updateUserConversationItems() {
        val filteredItems = unfilteredUserConversationItems.value?.filterByName(conversationFilter) ?: emptyList()
        userConversationItems.value = filteredItems

        isNoResultsFoundVisible.value = conversationFilter.isNotEmpty() && filteredItems.isEmpty()
        isNoConversationsVisible.value = conversationFilter.isEmpty() && filteredItems.isEmpty()
    }

    fun getUserConversations() = viewModelScope.launch {
        conversationsRepository.getUserConversations().collect { (list, status) ->
            Timber.d("UserConversations collected: ${list.size}, ${status}")

            unfilteredUserConversationItems.value = list
                .asConversationListViewItems(applicationContext)
                .merge(unfilteredUserConversationItems.value)

            if (status is RepositoryRequestStatus.Error) {
                onConversationError.value = ConversationsError.CONVERSATION_FETCH_USER_FAILED
            }
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
            filter { it.name.contains(name, ignoreCase = true) }
        }

    fun createConversation(friendlyName: String) = viewModelScope.launch {
        Timber.d("Creating conversation: $friendlyName")
        try {
            setDataLoading(true)
            val conversationSid = conversationListManager.createConversation(friendlyName)
            conversationListManager.joinConversation(conversationSid)
            Timber.d("Created conversation: $friendlyName $conversationSid")
            onConversationCreated.call()
        } catch (e: ConversationsException) {
            Timber.d("Failed to create conversation")
            onConversationError.value = ConversationsError.CONVERSATION_CREATE_FAILED
        } finally {
            setDataLoading(false)
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
            Timber.d("Failed to leave conversation")
            onConversationError.value = ConversationsError.CONVERSATION_LEAVE_FAILED
        } finally {
            setConversationLoading(conversationSid, false)
        }
    }
}
