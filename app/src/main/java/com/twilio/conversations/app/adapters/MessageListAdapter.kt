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
import com.twilio.conversations.app.databinding.RowMessageItemIncomingBinding
import com.twilio.conversations.app.databinding.RowMessageItemOutgoingBinding
import com.twilio.conversations.app.databinding.ViewReactionItemBinding
import timber.log.Timber

class MessageListAdapter(
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem) -> Unit,
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

        val mediaSize = Formatter.formatShortFileSize(context, message.mediaSize ?: 0)
        val mediaUploadedBytes = Formatter.formatShortFileSize(context, message.mediaUploadedBytes ?: 0)
        val mediaDownloadedBytes = Formatter.formatShortFileSize(context, message.mediaDownloadedBytes ?: 0)

        val attachmentInfoText = when {
            message.sendStatus == SendStatus.ERROR -> context.getString(R.string.err_failed_to_upload_media)

            message.mediaUploading -> context.getString(R.string.attachment_uploading, mediaUploadedBytes)

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == COMPLETED -> context.getString(R.string.attachment_tap_to_open)

            message.mediaDownloadState == NOT_STARTED -> mediaSize

            message.mediaDownloadState == DOWNLOADING -> context.getString(
                R.string.attachment_downloading,
                mediaDownloadedBytes
            )

            message.mediaDownloadState == ERROR -> context.getString(R.string.err_failed_to_download_media)

            else -> error("Never happens")
        }

        val attachmentInfoColor = when {
            message.sendStatus == SendStatus.ERROR ||
                    message.mediaDownloadState == ERROR -> ContextCompat.getColor(context, R.color.colorAccent)

            message.mediaUploading -> ContextCompat.getColor(context, R.color.text_subtitle)

            message.mediaUploadUri != null ||
                    message.mediaDownloadState == COMPLETED -> ContextCompat.getColor(context, R.color.colorPrimary)

            else -> ContextCompat.getColor(context, R.color.text_subtitle)
        }

        val attachmentOnClickListener = View.OnClickListener {
            if (message.mediaDownloadState == COMPLETED && message.mediaUri != null) {
                onOpenMedia(message.mediaUri, message.mediaType!!)
            } else if (message.mediaUploadUri != null) {
                onOpenMedia(message.mediaUploadUri, message.mediaType!!)
            } else if (message.mediaDownloadState != DOWNLOADING) {
                onDownloadMedia(message)
            }
        }

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
                addReactions(binding.messageReactionHolder, message)
                binding.attachmentInfo.text = attachmentInfoText
                binding.attachmentInfo.setTextColor(attachmentInfoColor)
                binding.attachmentBackground.setOnClickListener(attachmentOnClickListener)
                binding.attachmentBackground.setOnLongClickListener(longClickListener)
            }
            is RowMessageItemOutgoingBinding -> {
                binding.message = message
                addReactions(binding.messageReactionHolder, message)
                binding.attachmentInfo.text = attachmentInfoText
                binding.attachmentInfo.setTextColor(attachmentInfoColor)
                binding.attachmentBackground.setOnClickListener(attachmentOnClickListener)
                binding.attachmentBackground.setOnLongClickListener(longClickListener)
            }
            else -> error("Unknown binding type: $binding")
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
