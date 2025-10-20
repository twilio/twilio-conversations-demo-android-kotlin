package com.twilio.conversations.app.adapters

import android.net.Uri
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.DownloadState.COMPLETED
import com.twilio.conversations.app.common.enums.DownloadState.DOWNLOADING
import com.twilio.conversations.app.common.enums.DownloadState.ERROR
import com.twilio.conversations.app.common.enums.DownloadState.NOT_STARTED
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.MessageMediaViewItem
import com.twilio.conversations.app.databinding.RowMessageItemIncomingBinding
import com.twilio.conversations.app.databinding.RowMessageItemOutgoingBinding
import com.twilio.conversations.app.databinding.RowMessageMediaItemBinding
import com.twilio.conversations.app.databinding.ViewReactionItemBinding
import timber.log.Timber

class MessageListAdapter(
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem, media: MessageMediaViewItem) -> Unit,
    private val onOpenMedia: (location: Uri, mimeType: String) -> Unit,
    private val onItemLongClick: (messageIndex: Long) -> Unit,
    private val onReactionClicked: (messageIndex: Long) -> Unit

) : PagedListAdapter<MessageListViewItem, MessageListAdapter.ViewHolder>(MESSAGE_COMPARATOR) {

    fun getMessage(position: Int): MessageListViewItem? {
        return getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.direction?.value ?: Direction.OUTGOING.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {

            Direction.INCOMING.value ->
                RowMessageItemIncomingBinding.inflate(LayoutInflater.from(parent.context), parent, false)

            else -> RowMessageItemOutgoingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        }
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = getItem(position)
        if (message == null) {
            Timber.e("onBindViewHolder called for a missing item (position: $position, total items: $itemCount)")
            return
        }

        val binding = holder.binding
        val context = binding.root.context

        val longClickListener = View.OnLongClickListener {
            onItemLongClick(message.index)
            return@OnLongClickListener true
        }

        binding.root.setOnLongClickListener(longClickListener)

        if (message.sendStatus == SendStatus.ERROR) {
            binding.root.setOnClickListener {
                onDisplaySendError(message)
            }
        }

        when (binding) {
            is RowMessageItemIncomingBinding -> {
                binding.message = message
//                addReactions(binding.messageReactionHolder, message)
                updateAttachments(binding.attachmentsContainer, message)
                binding.attachmentsContainer.setOnLongClickListener(longClickListener)
            }
            is RowMessageItemOutgoingBinding -> {
                binding.message = message
//                addReactions(binding.messageReactionHolder, message)
                updateAttachments(binding.attachmentsContainer, message)
                binding.attachmentsContainer.setOnLongClickListener(longClickListener)
            }
            else -> error("Unknown binding type: $binding")
        }

    }

    private fun updateAttachments(
        attachmentsContainer: android.widget.LinearLayout,
        message: MessageListViewItem
    ) {
        attachmentsContainer.removeAllViews()
        val context = attachmentsContainer.context

        if (message.mediaData.isEmpty()) {
            attachmentsContainer.visibility = android.view.View.GONE
            return
        }
        attachmentsContainer.visibility = android.view.View.VISIBLE

        message.mediaData.forEach { mediaItem ->
            // Inflate a new layout for each attachment
            val attachmentBinding = RowMessageMediaItemBinding.inflate(
                LayoutInflater.from(context),
                attachmentsContainer,
                false // Attach manually below
            )

            // Determine text and color for this specific media item
            val mediaSize = mediaItem.mediaSize?.let { Formatter.formatShortFileSize(context, it) }
            val mediaUploadedBytes =
                Formatter.formatShortFileSize(context, mediaItem.mediaUploadedBytes ?: 0)
            val mediaDownloadedBytes =
                Formatter.formatShortFileSize(context, mediaItem.mediaDownloadedBytes ?: 0)

            attachmentBinding.attachmentFileName.text = mediaItem.mediaFileName ?: "Attachment"

            val attachmentInfoText = when {
                message.sendStatus == SendStatus.ERROR -> context.getString(R.string.err_failed_to_upload_media)
                mediaItem.mediaUploading -> context.getString(
                    R.string.attachment_uploading,
                    mediaUploadedBytes
                )

                mediaItem.mediaUploadUri != null || mediaItem.mediaDownloadState == COMPLETED -> context.getString(
                    R.string.attachment_tap_to_open
                )

                mediaItem.mediaDownloadState == NOT_STARTED -> mediaSize
                mediaItem.mediaDownloadState == DOWNLOADING -> context.getString(
                    R.string.attachment_downloading,
                    mediaDownloadedBytes
                )

                mediaItem.mediaDownloadState == ERROR -> context.getString(R.string.err_failed_to_download_media)
                else -> ""
            }

            val attachmentInfoColor = when {
                message.sendStatus == SendStatus.ERROR || mediaItem.mediaDownloadState == ERROR ->
                    ContextCompat.getColor(context, R.color.colorAccent)

                mediaItem.mediaUploading || mediaItem.mediaDownloadState == DOWNLOADING ->
                    ContextCompat.getColor(context, R.color.text_subtitle)

                mediaItem.mediaUploadUri != null || mediaItem.mediaDownloadState == COMPLETED ->
                    ContextCompat.getColor(context, R.color.colorPrimary)

                else -> ContextCompat.getColor(context, R.color.text_subtitle)
            }

            attachmentBinding.attachmentInfo.text = attachmentInfoText
            attachmentBinding.attachmentInfo.setTextColor(attachmentInfoColor)

            // Set click listener for this specific attachment
            attachmentBinding.root.setOnClickListener {
                when {
                    mediaItem.mediaDownloadState == COMPLETED && mediaItem.mediaUri != null ->
                        mediaItem.mediaType?.let { it1 -> onOpenMedia(mediaItem.mediaUri, it1) }

                    mediaItem.mediaUploadUri != null ->
                        mediaItem.mediaType?.let { it1 ->
                            onOpenMedia(mediaItem.mediaUploadUri,
                                it1
                            )
                        }

                    mediaItem.mediaDownloadState != DOWNLOADING && !mediaItem.mediaUploading ->
                        onDownloadMedia(message, mediaItem) // Pass the specific media item
                }
            }

            // Add the newly created attachment view to the container
            attachmentsContainer.addView(attachmentBinding.root)
        }
    }

    private fun addReactions(rootView: LinearLayout, message: MessageListViewItem) {
        rootView.setOnClickListener { onReactionClicked(message.index) }
        rootView.removeAllViews()
        Timber.d("Adding reactions: ${message.reactions}")
        message.reactions.forEach { reaction ->
            if (reaction.value.isNotEmpty()) {
                val emoji = ViewReactionItemBinding.inflate(LayoutInflater.from(rootView.context))
                emoji.emojiIcon.setText(reaction.key.emoji)
                emoji.emojiCounter.text = reaction.value.size.toString()

                val color = if (message.direction == Direction.OUTGOING) R.color.white else R.color.colorPrimary
                emoji.emojiCounter.setTextColor(ContextCompat.getColor(rootView.context, color))

                rootView.addView(emoji.root)
            }
        }
    }

    class ViewHolder(val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root)

    companion object {
        val MESSAGE_COMPARATOR = object : DiffUtil.ItemCallback<MessageListViewItem>() {
            override fun areContentsTheSame(oldItem: MessageListViewItem, newItem: MessageListViewItem) =
                oldItem == newItem

            override fun areItemsTheSame(oldItem: MessageListViewItem, newItem: MessageListViewItem) =
                oldItem.sid == newItem.sid
        }
    }
}
