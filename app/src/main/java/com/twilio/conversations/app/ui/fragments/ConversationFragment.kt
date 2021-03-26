package com.twilio.conversations.app.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.adapters.OnConversationEvent
import com.twilio.conversations.app.common.SheetListener
import com.twilio.conversations.app.common.extensions.hide
import com.twilio.conversations.app.common.extensions.isShowing
import com.twilio.conversations.app.common.extensions.lazyActivityViewModel
import com.twilio.conversations.app.common.extensions.show
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.ui.MessageListActivity
import kotlinx.android.synthetic.main.fragment_conversations_list.*
import kotlinx.android.synthetic.main.view_conversation_remove_screen.*

abstract class ConversationFragment : Fragment(), OnConversationEvent {

    private val sheetBehavior by lazy { BottomSheetBehavior.from(removeConversationSheet) }
    private val sheetListener by lazy { SheetListener(sheet_background) {} }

    val conversationListViewModel by lazyActivityViewModel {
        injector.createConversationListViewModel(requireActivity().application)
    }

    private fun hideBottomSheet() {
        sheetBehavior.hide()
    }

    private fun showBottomSheet() {
        sheetBehavior.show()
    }

    open fun onBackPressed(): Boolean {
        if (sheetBehavior.isShowing()) {
            hideBottomSheet()
            return true
        }
        return false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sheetBehavior.addBottomSheetCallback(sheetListener)
        sheet_background.setOnClickListener { hideBottomSheet() }
        remove_conversation_button.setOnClickListener {
            conversationListViewModel.selectedConversationSid?.let {
                conversationListViewModel.removeConversation(it)
            }
            hideBottomSheet()
        }
        leave_conversation_button.setOnClickListener {
            conversationListViewModel.selectedConversationSid?.let {
                conversationListViewModel.leaveConversation(it)
            }
            hideBottomSheet()
        }
        if (conversationListViewModel.selectedConversationSid == null) {
            hideBottomSheet()
        }
    }

    override fun onConversationClicked(conversationSid: String) {
        if (conversationListViewModel.isConversationJoined(conversationSid)) {
            MessageListActivity.start(requireContext(), conversationSid)
        } else {
            conversationListViewModel.joinConversation(conversationSid)
        }
    }

    override fun onConversationLongClicked(conversationSid: String) {
        if (conversationListViewModel.isConversationJoined(conversationSid)) {
            conversationListViewModel.selectedConversationSid = conversationSid
            hideBottomSheet()
            showBottomSheet()
        }
    }

    override fun onConversationMuteClicked(conversationSid: String) {
        if (conversationListViewModel.isConversationMuted(conversationSid)) {
            conversationListViewModel.unmuteConversation(conversationSid)
        } else {
            conversationListViewModel.muteConversation(conversationSid)
        }
    }
}
