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
import timber.log.Timber

class ConversationDetailsActivity : BaseActivity() {
    private lateinit var binding: ActivityConversationDetailsBinding
    private val renameConversationSheetBehavior by lazy { BottomSheetBehavior.from(binding.renameConversationSheet.root) }
    private val addChatParticipantSheetBehavior by lazy { BottomSheetBehavior.from(binding.addChatParticipantSheet.root) }
    private val addNonChatParticipantSheetBehavior by lazy { BottomSheetBehavior.from(binding.addNonChatParticipantSheet.root) }
    private val sheetListener by lazy { SheetListener(binding.sheetBackground) { hideKeyboard() } }
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
        binding = DataBindingUtil
            .setContentView<ActivityConversationDetailsBinding>(this, R.layout.activity_conversation_details)
            .apply {
            lifecycleOwner = this@ConversationDetailsActivity
        }

        initViews()
    }

    override fun onBackPressed() {
        if (renameConversationSheetBehavior.isShowing()) {
            renameConversationSheetBehavior.hide()
            return
        }
        if (addChatParticipantSheetBehavior.isShowing()) {
            addChatParticipantSheetBehavior.hide()
            return
        }
        if (addNonChatParticipantSheetBehavior.isShowing()) {
            addNonChatParticipantSheetBehavior.hide()
            return
        }
        super.onBackPressed()
    }

    private fun initViews() {
        setSupportActionBar(binding.conversationDetailsToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.conversationDetailsToolbar.setNavigationOnClickListener { onBackPressed() }
        renameConversationSheetBehavior.addBottomSheetCallback(sheetListener)
        addChatParticipantSheetBehavior.addBottomSheetCallback(sheetListener)
        addNonChatParticipantSheetBehavior.addBottomSheetCallback(sheetListener)
        title = getString(R.string.details_title)

        binding.addChatParticipantButton.setOnClickListener {
            Timber.d("Add chat participant clicked")
            binding.addChatParticipantSheet.addChatParticipantIdInput.text?.clear()
            addChatParticipantSheetBehavior.show()
        }

        binding.addNonChatParticipantButton.setOnClickListener {
            Timber.d("Add non-chat participant clicked")
            binding.addNonChatParticipantSheet.addNonChatParticipantPhoneInput.text?.clear()
            binding.addNonChatParticipantSheet.addNonChatParticipantProxyInput.text?.clear()
            addNonChatParticipantSheetBehavior.show()
        }

        binding.participantsListButton.setOnClickListener {
            Timber.d("Show participant list clicked")
            ParticipantListActivity.start(this, conversationDetailsViewModel.conversationSid)
        }

        binding.conversationRenameButton.setOnClickListener {
            Timber.d("Show rename conversation popup clicked")
            renameConversationSheetBehavior.show()
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

        binding.sheetBackground.setOnClickListener {
            renameConversationSheetBehavior.hide()
            addChatParticipantSheetBehavior.hide()
            addNonChatParticipantSheetBehavior.hide()
        }

        binding.renameConversationSheet.renameConversationCancelButton.setOnClickListener {
            renameConversationSheetBehavior.hide()
        }

        binding.renameConversationSheet.renameConversationButton.setOnClickListener {
            Timber.d("Conversation rename clicked")
            renameConversationSheetBehavior.hide()
            conversationDetailsViewModel.renameConversation(binding.renameConversationSheet.renameConversationInput.text.toString())
        }

        binding.addChatParticipantSheet.addChatParticipantIdCancelButton.setOnClickListener {
            addChatParticipantSheetBehavior.hide()
        }

        binding.addChatParticipantSheet.addChatParticipantIdButton.setOnClickListener {
            Timber.d("Add chat participant clicked")
            addChatParticipantSheetBehavior.hide()
            conversationDetailsViewModel.addChatParticipant(binding.addChatParticipantSheet.addChatParticipantIdInput.text.toString())
        }

        binding.addNonChatParticipantSheet.addNonChatParticipantIdCancelButton.setOnClickListener {
            addNonChatParticipantSheetBehavior.hide()
        }

        binding.addNonChatParticipantSheet.addNonChatParticipantIdButton.setOnClickListener {
            Timber.d("Add non-chat participant clicked")
            addNonChatParticipantSheetBehavior.hide()
            conversationDetailsViewModel.addNonChatParticipant(
                    binding.addNonChatParticipantSheet.addNonChatParticipantPhoneInput.text.toString(),
                    binding.addNonChatParticipantSheet.addNonChatParticipantProxyInput.text.toString(),
            )
        }

        conversationDetailsViewModel.isShowProgress.observe(this) { show ->
            if (show) {
                progressDialog.show()
            } else {
                progressDialog.hide()
            }
        }

        conversationDetailsViewModel.conversationDetails.observe(this) { conversationDetails ->
            Timber.d("Conversation details received: $conversationDetails")
            binding.details = conversationDetails
            binding.renameConversationSheet.renameConversationInput.setText(conversationDetails.conversationName)
        }

        conversationDetailsViewModel.onDetailsError.observe(this) { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                showToast(R.string.err_failed_to_get_conversation)
                finish()
            }
            binding.conversationDetailsLayout.showSnackbar(getErrorMessage(error))
        }

        conversationDetailsViewModel.onConversationLeft.observe(this) {
            ConversationListActivity.start(this)
            finish()
        }

        conversationDetailsViewModel.onParticipantAdded.observe(this) { identity ->
            binding.conversationDetailsLayout.showSnackbar(
                getString(
                    R.string.participant_added_message,
                    identity
                )
            )
        }
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, ConversationDetailsActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
