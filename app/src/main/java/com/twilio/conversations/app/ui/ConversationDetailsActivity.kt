package com.twilio.conversations.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.SheetListener
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.databinding.ActivityConversationDetailsBinding
import kotlinx.android.synthetic.main.activity_conversation_details.*
import kotlinx.android.synthetic.main.view_add_chat_participant_screen.*
import kotlinx.android.synthetic.main.view_add_non_chat_participant_screen.*
import kotlinx.android.synthetic.main.view_conversation_rename_screen.*
import timber.log.Timber

class ConversationDetailsActivity : BaseActivity() {

    private val renameConversationSheet by lazy { BottomSheetBehavior.from(rename_conversation_sheet) }
    private val addChatParticipantSheet by lazy { BottomSheetBehavior.from(add_chat_participant_sheet) }
    private val addNonChatParticipantSheet by lazy { BottomSheetBehavior.from(add_non_chat_participant_sheet) }
    private val sheetListener by lazy { SheetListener(sheet_background) { hideKeyboard() } }
    private val progressDialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(R.layout.view_loading_dialog)
            .create()
    }

    val conversationDetailsViewModel by lazyViewModel {
        injector.createConversationDetailsViewModel(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil
            .setContentView<ActivityConversationDetailsBinding>(this, R.layout.activity_conversation_details)
            .apply {
            lifecycleOwner = this@ConversationDetailsActivity
        }

        initViews(binding)
    }

    override fun onBackPressed() {
        if (renameConversationSheet.isShowing()) {
            renameConversationSheet.hide()
            return
        }
        if (addChatParticipantSheet.isShowing()) {
            addChatParticipantSheet.hide()
            return
        }
        if (addNonChatParticipantSheet.isShowing()) {
            addNonChatParticipantSheet.hide()
            return
        }
        super.onBackPressed()
    }

    private fun initViews(binding: ActivityConversationDetailsBinding) {
        setSupportActionBar(binding.conversationDetailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.conversationDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
        renameConversationSheet.addBottomSheetCallback(sheetListener)
        addChatParticipantSheet.addBottomSheetCallback(sheetListener)
        addNonChatParticipantSheet.addBottomSheetCallback(sheetListener)
        title = getString(R.string.details_title)

        binding.addChatParticipantButton.setOnClickListener {
            Timber.d("Add chat participant clicked")
            add_chat_participant_id_input.text?.clear()
            addChatParticipantSheet.show()
        }

        binding.addNonChatParticipantButton.setOnClickListener {
            Timber.d("Add non-chat participant clicked")
            add_non_chat_participant_phone_input.text?.clear()
            add_non_chat_participant_proxy_input.text?.clear()
            addNonChatParticipantSheet.show()
        }

        binding.participantsListButton.setOnClickListener {
            Timber.d("Show participant list clicked")
            ParticipantListActivity.start(this, conversationDetailsViewModel.conversationSid)
        }

        binding.conversationRenameButton.setOnClickListener {
            Timber.d("Show rename conversation popup clicked")
            renameConversationSheet.show()
        }

        binding.conversationMuteButton.setOnClickListener {
            Timber.d("Conversation mute clicked")
            if (conversationDetailsViewModel.isConversationMuted()) {
                conversationDetailsViewModel.unmuteConversation()
            } else {
                conversationDetailsViewModel.muteConversation()
            }
        }

        binding.conversationLeaveButton.setOnClickListener {
            Timber.d("Conversation leave clicked")
            conversationDetailsViewModel.leaveConversation()
        }

        sheet_background.setOnClickListener {
            renameConversationSheet.hide()
            addChatParticipantSheet.hide()
            addNonChatParticipantSheet.hide()
        }

        rename_conversation_cancel_button.setOnClickListener {
            renameConversationSheet.hide()
        }

        rename_conversation_button.setOnClickListener {
            Timber.d("Conversation rename clicked")
            renameConversationSheet.hide()
            conversationDetailsViewModel.renameConversation(rename_conversation_input.text.toString())
        }

        add_chat_participant_id_cancel_button.setOnClickListener {
            addChatParticipantSheet.hide()
        }

        add_chat_participant_id_button.setOnClickListener {
            Timber.d("Add chat participant clicked")
            addChatParticipantSheet.hide()
            conversationDetailsViewModel.addChatParticipant(add_chat_participant_id_input.text.toString())
        }

        add_non_chat_participant_id_cancel_button.setOnClickListener {
            addNonChatParticipantSheet.hide()
        }

        add_non_chat_participant_id_button.setOnClickListener {
            Timber.d("Add non-chat participant clicked")
            addNonChatParticipantSheet.hide()
            conversationDetailsViewModel.addNonChatParticipant(
                    add_non_chat_participant_phone_input.text.toString(),
                    add_non_chat_participant_proxy_input.text.toString(),
            )
        }

        conversationDetailsViewModel.isShowProgress.observe(this, { show ->
            if (show) {
                progressDialog.show()
            } else {
                progressDialog.hide()
            }
        })

        conversationDetailsViewModel.conversationDetails.observe(this, { conversationDetails ->
            Timber.d("Conversation details received: $conversationDetails")
            binding.details = conversationDetails
            rename_conversation_input.setText(conversationDetails.conversationName)
        })

        conversationDetailsViewModel.onDetailsError.observe(this, { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                showToast(R.string.err_failed_to_get_conversation)
                finish()
            }
            conversationDetailsLayout.showSnackbar(getErrorMessage(error))
        })

        conversationDetailsViewModel.onConversationLeft.observe(this, {
            ConversationListActivity.start(this)
            finish()
        })

        conversationDetailsViewModel.onParticipantAdded.observe(this, { identity ->
            conversationDetailsLayout.showSnackbar(getString(R.string.participant_added_message, identity))
        })
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, ConversationDetailsActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
