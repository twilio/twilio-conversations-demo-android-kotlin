package com.twilio.conversations.app.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.R
import com.twilio.conversations.app.adapters.MessageListAdapter
import com.twilio.conversations.app.common.SheetListener
import com.twilio.conversations.app.common.enums.ConversationsError
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.data.models.MessageListViewItem
import kotlinx.android.synthetic.main.activity_conversation.*
import kotlinx.android.synthetic.main.view_add_reaction_screen.*
import kotlinx.android.synthetic.main.view_select_attachment_screen.*
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

private const val REQUEST_PICK_IMAGE = 1
private const val REQUEST_IMAGE_CAPTURE = 2

private const val SAVED_STATE_IMAGE_CAPTURE_CONTENT_URI = "SAVED_STATE_IMAGE_CAPTURE_CONTENT_URI"

class MessageListActivity : BaseActivity() {

    private val sheetBehavior by lazy { BottomSheetBehavior.from(addReactionSheet) }
    private val sheetListener by lazy { SheetListener(sheet_background) { hideKeyboard() } }

    private val attachmentSheetBehavior by lazy { BottomSheetBehavior.from(selectAttachmentSheet) }
    private val attachmentSheetListener = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            sheet_background.visibility = View.VISIBLE
            sheet_background.alpha = slideOffset
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                sheet_background.visibility = View.GONE
            }
        }
    }

    private var imageCaptureContentUri: Uri? = null

    val messageListViewModel by lazyViewModel {
        injector.createMessageListViewModel(applicationContext, intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)

        imageCaptureContentUri = savedInstanceState?.getParcelable(SAVED_STATE_IMAGE_CAPTURE_CONTENT_URI)
        initViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_conversation_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_conversation_details -> ConversationDetailsActivity.start(this, messageListViewModel.conversationSid)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            sheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> hideReactionDialog()
            attachmentSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> hideAttachmentSheet()
            else -> super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(SAVED_STATE_IMAGE_CAPTURE_CONTENT_URI, imageCaptureContentUri)
        super.onSaveInstanceState(outState)
    }

    private fun initViews() {
        setSupportActionBar(conversation_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        conversation_toolbar.setNavigationOnClickListener { onBackPressed() }
        sheetBehavior.addBottomSheetCallback(sheetListener)
        attachmentSheetBehavior.addBottomSheetCallback(attachmentSheetListener)
        sheet_background.setOnClickListener {
            hideReactionDialog()
            hideAttachmentSheet()
        }
        val adapter = MessageListAdapter(
            onResend = { message ->
                Timber.d("Re-send clicked: ${message.uuid}")
                resendMessage(message)
            },
            onDownloadMedia = { message ->
                Timber.d("Download clicked: $message")
                messageListViewModel.startMessageMediaDownload(message.index, message.mediaFileName)
            },
            onOpenMedia = { uri ->
                Timber.d("Open clicked")
                viewUri(uri)
            },
            onAddReaction = { messageIndex ->
                Timber.d("Add reaction clicked")
                messageListViewModel.selectedMessageIndex = messageIndex
                showReactionDialog()
            },
            onReactionClicked = {reaction, messageIndex ->
                Timber.d("Reaction clicked:, $reaction")
                messageListViewModel.selectedMessageIndex = messageIndex
                messageListViewModel.addRemoveReaction(reaction)
            }
        )
        messageList.adapter = adapter
        messageList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val index = (recyclerView.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
                val message = adapter.getMessage(index)
                if (message != null) {
                    messageListViewModel.handleMessageDisplayed(message.index)
                }
            }
        })

        messageSendButton.setOnClickListener {
            messageInput.text.toString().takeIf { it.isNotBlank() }?.let { message ->
                Timber.d("Sending message: $message")
                messageListViewModel.sendTextMessage(message)
                messageInput.setText("")
            }
        }
        messageInput.doAfterTextChanged {
            messageListViewModel.typing()
        }
        messageListViewModel.conversationName.observe(this, { conversationName ->
            title = conversationName
        })
        messageListViewModel.messageItems.observe(this, { messages ->
            val lastVisibleMessageIndex = (messageList.layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()
            // Scroll list to bottom when it was at the bottom before submitting new messages
            val commitCallback: Runnable? = if (lastVisibleMessageIndex == adapter.itemCount - 1) {
                Runnable { messageList.scrollToPosition(adapter.itemCount - 1) }
            } else {
                null
            }
            adapter.submitList(messages, commitCallback)
        })
        messageListViewModel.onMessageError.observe(this, { error ->
            if (error == ConversationsError.CONVERSATION_GET_FAILED) {
                finish()
            }
            conversationLayout.showSnackbar(getErrorMessage(error))
        })
        messageListViewModel.typingParticipantsList.observe(this, { participants ->
            typingIndicator.text = if (participants.isNotEmpty()) {
                val participantsList = participants.joinToString(limit = 3, truncated = getString(R.string.typing_indicator_overflow))
                resources.getQuantityString(R.plurals.typing_indicator, participants.size, participantsList)
            } else {
                ""
            }
        })
        reaction_heart.setOnClickListener {
            Timber.d("Heart clicked")
            messageListViewModel.addRemoveReaction(Reaction.HEART)
            hideReactionDialog()
        }
        reaction_laugh.setOnClickListener {
            Timber.d("Laugh clicked")
            messageListViewModel.addRemoveReaction(Reaction.LAUGH)
            hideReactionDialog()
        }
        reaction_sad.setOnClickListener {
            Timber.d("Sad clicked")
            messageListViewModel.addRemoveReaction(Reaction.SAD)
            hideReactionDialog()
        }
        reaction_thumbs_down.setOnClickListener {
            Timber.d("Thumbs down clicked")
            messageListViewModel.addRemoveReaction(Reaction.THUMBS_DOWN)
            hideReactionDialog()
        }
        reaction_thumbs_up.setOnClickListener {
            Timber.d("Thumbs up clicked")
            messageListViewModel.addRemoveReaction(Reaction.THUMBS_UP)
            hideReactionDialog()
        }
        messageAttachmentButton.setOnClickListener {
            showAttachmentSheet()
        }
        attachment_choose_image.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, null), REQUEST_PICK_IMAGE)
            hideAttachmentSheet()
            hideKeyboard()
        }
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            attachment_take_picture.visibility = View.GONE
        }
        attachment_take_picture.setOnClickListener {
            startImageCaptureActivity()
            hideAttachmentSheet()
            hideKeyboard()
        }
    }

    private fun resendMessage(message: MessageListViewItem) {
        if (message.type == MessageType.TEXT) {
            messageListViewModel.resendTextMessage(message.uuid)
        } else if (message.type == MessageType.MEDIA) {
            val fileInputStream = message.mediaUploadUri?.let { contentResolver.openInputStream(it) }
            if (fileInputStream != null) {
                messageListViewModel.resendMediaMessage(fileInputStream, message.uuid)
            } else {
                Timber.w("Could not get input stream for file reading: ${message.mediaUploadUri}")
                showToast(R.string.err_failed_to_resend_media)
            }
        }
    }

    private fun startImageCaptureActivity() {
        val photoFile = try {
            createImageFile()
        } catch (e: IOException) {
            Timber.e(e)
            showToast(R.string.err_failed_to_capture_image)
            return
        }
        imageCaptureContentUri = FileProvider.getUriForFile(this, "com.twilio.conversations.app.fileprovider", photoFile)
        Timber.d("Capturing image to $imageCaptureContentUri")
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageCaptureContentUri)
        startActivityForResult(Intent.createChooser(intent, null), REQUEST_IMAGE_CAPTURE)
    }

    private fun viewUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = uri
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, null))
    }

    private fun hideReactionDialog() {
        sheetBehavior.hide()
    }

    private fun showReactionDialog() {
        sheetBehavior.show()
    }

    private fun hideAttachmentSheet() {
        attachmentSheetBehavior.hide()
    }

    private fun showAttachmentSheet() {
        attachmentSheetBehavior.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val uri = when(requestCode) {
            REQUEST_PICK_IMAGE -> data?.data
            REQUEST_IMAGE_CAPTURE -> imageCaptureContentUri
            else -> null
        }
        if ((requestCode == REQUEST_PICK_IMAGE || requestCode == REQUEST_IMAGE_CAPTURE) && resultCode == Activity.RESULT_OK && uri != null) {
            val inputStream = contentResolver.openInputStream(uri)
            val type = contentResolver.getType(uri)
            val name = contentResolver.getString(uri, OpenableColumns.DISPLAY_NAME)
            if (inputStream != null) {
                messageListViewModel.sendMediaMessage(uri.toString(), inputStream, name, type)
            } else {
                Timber.w("Could not get input stream for file reading: $data")
                showToast(R.string.err_failed_to_send_media)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val dir = if (externalMediaDirs.isNotEmpty() && externalMediaDirs[0] != null) {
            externalMediaDirs[0]
        } else {
            throw IOException("No external media dir available")
        }
        val storageDir = File(dir, "images")
        storageDir.mkdir()
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, MessageListActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
