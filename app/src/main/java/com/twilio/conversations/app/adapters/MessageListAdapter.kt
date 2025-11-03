package com.twilio.conversations.app.adapters

import android.net.Uri
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
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
import com.twilio.conversations.app.common.enums.SendStatus
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.data.models.MessageAttachmentViewItem
import com.twilio.conversations.app.databinding.RowMessageItemIncomingBinding
import com.twilio.conversations.app.databinding.RowMessageItemOutgoingBinding
import com.twilio.conversations.app.databinding.ViewReactionItemBinding
import timber.log.Timber

class MessageListAdapter(
    private val onDisplaySendError: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem, media: MessageAttachmentViewItem) -> Unit,
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
                addReactions(binding.messageReactionHolder, message)
                setupAttachments(binding.attachmentsContainer, message, longClickListener)
            }
            is RowMessageItemOutgoingBinding -> {
                binding.message = message
                addReactions(binding.messageReactionHolder, message)
                setupAttachments(binding.attachmentsContainer, message, longClickListener)
            }
            else -> error("Unknown binding type: $binding")
        }
    }

    private fun setupAttachments(
        attachmentsContainer: LinearLayout,
        message: MessageListViewItem,
        longClickListener: View.OnLongClickListener
    ) {
        attachmentsContainer.removeAllViews()

        message.attachmentsList.forEach { attachment ->
            val attachmentView = LayoutInflater.from(attachmentsContainer.context)
                .inflate(R.layout.item_attachment, attachmentsContainer, false)

            val icon = attachmentView.findViewById<ImageView>(R.id.attachment_icon)
            val fileName = attachmentView.findViewById<TextView>(R.id.attachment_file_name)
            val info = attachmentView.findViewById<TextView>(R.id.attachment_info)
            val progress = attachmentView.findViewById<ProgressBar>(R.id.attachment_progress)
            val failed = attachmentView.findViewById<ImageView>(R.id.attachment_failed)

            fileName.text = attachment.fileName ?: "Unknown file"
            val size = Formatter.formatShortFileSize(attachmentsContainer.context, attachment.size ?: 0)
            
            when {
                message.sendStatus == SendStatus.ERROR -> {
                    info.text = attachmentsContainer.context.getString(R.string.err_failed_to_upload_media)
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.colorAccent))
                    failed.visibility = View.VISIBLE
                    progress.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_to_download)
                }
                attachment.downloadState == ERROR -> {
                    info.text = attachmentsContainer.context.getString(R.string.err_failed_to_download_media)
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.colorAccent))
                    failed.visibility = View.VISIBLE
                    progress.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_to_download)
                }
                attachment.downloadState == DOWNLOADING -> {
                    val downloadedBytes = Formatter.formatShortFileSize(attachmentsContainer.context, attachment.downloadedBytes ?: 0)
                    info.text = attachmentsContainer.context.getString(R.string.attachment_downloading, downloadedBytes)
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.text_subtitle))
                    progress.visibility = View.VISIBLE
                    failed.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_to_download)
                }
                attachment.uploading -> {
                    val uploadedBytes = Formatter.formatShortFileSize(attachmentsContainer.context, attachment.uploadedBytes ?: 0)
                    info.text = attachmentsContainer.context.getString(R.string.attachment_uploading, uploadedBytes)
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.text_subtitle))
                    progress.visibility = View.VISIBLE
                    failed.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_to_download)
                }
                attachment.downloadState == COMPLETED || attachment.uploadUri != null -> {
                    info.text = attachmentsContainer.context.getString(R.string.attachment_tap_to_open)
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.colorPrimary))
                    progress.visibility = View.GONE
                    failed.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_downloaded)
                }
                else -> {
                    info.text = size
                    info.setTextColor(ContextCompat.getColor(attachmentsContainer.context, R.color.text_subtitle))
                    progress.visibility = View.GONE
                    failed.visibility = View.GONE
                    icon.setImageResource(R.drawable.ic_attachment_to_download)
                }
            }

            attachmentView.setOnClickListener {
                when {
                    attachment.downloadState == COMPLETED && attachment.uri != null -> {
                        onOpenMedia(attachment.uri, attachment.type ?: "")
                    }
                    attachment.uploadUri != null -> {
                        onOpenMedia(attachment.uploadUri, attachment.type ?: "")
                    }
                    attachment.downloadState != DOWNLOADING -> {
                        onDownloadMedia(message, attachment)
                    }
                }
            }

            attachmentView.setOnLongClickListener(longClickListener)

            attachmentsContainer.addView(attachmentView)
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
