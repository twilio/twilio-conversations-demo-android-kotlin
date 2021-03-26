package com.twilio.conversations.app.adapters

import android.net.Uri
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.databinding.ViewDataBinding
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.Direction
import com.twilio.conversations.app.common.enums.MessageType
import com.twilio.conversations.app.common.enums.Reaction
import com.twilio.conversations.app.data.models.MessageListViewItem
import com.twilio.conversations.app.databinding.RowMessageItemIncomingBinding
import com.twilio.conversations.app.databinding.RowMessageItemOutgoingBinding
import kotlinx.android.synthetic.main.view_reaction_item.view.*
import timber.log.Timber

class MessageListAdapter(
    private val onResend: (message: MessageListViewItem) -> Unit,
    private val onDownloadMedia: (message: MessageListViewItem) -> Unit,
    private val onOpenMedia: (location: Uri) -> Unit,
    private val onAddReaction: (messageIndex: Long) -> Unit,
    private val onReactionClicked: (reaction: Reaction, messageIndex: Long) -> Unit

) : PagedListAdapter<MessageListViewItem, MessageListAdapter.ViewHolder>(MESSAGE_COMPARATOR) {

    fun getMessage(position: Int) : MessageListViewItem? {
        return getItem(position)
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position)?.direction?.value ?: Direction.OUTGOING.value
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = when (viewType) {
            Direction.INCOMING.value -> RowMessageItemIncomingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
        when (val binding = holder.binding) {
            is RowMessageItemIncomingBinding -> {
                binding.message = message
                binding.messageReactionButton.setOnClickListener {
                    onAddReaction(message.index)
                }
                addReactions(binding.messageReactionHolder, message)

                if (message.type == MessageType.MEDIA) {
                    binding.attachmentSize.text =
                        Formatter.formatShortFileSize(binding.root.context, message.mediaSize ?: 0)
                    binding.attachmentDownload.setOnClickListener {
                        if (message.isDownloaded() && message.mediaUri != null) {
                            onOpenMedia(message.mediaUri)
                        } else {
                            onDownloadMedia(message)
                        }
                    }
                }
            }
            is RowMessageItemOutgoingBinding -> {
                binding.message = message
                binding.messageReactionButton.setOnClickListener {
                    onAddReaction(message.index)
                }
                addReactions(binding.messageReactionHolder, message)

                if (message.type == MessageType.MEDIA) {
                    binding.attachmentSize.text =
                        Formatter.formatShortFileSize(binding.root.context, message.mediaSize ?: 0)
                    binding.attachmentDownload.setOnClickListener {
                        if (message.isDownloaded() && message.mediaUri != null) {
                            onOpenMedia(message.mediaUri)
                        } else if (message.mediaUploadUri != null) {
                            onOpenMedia(message.mediaUploadUri)
                        } else {
                            onDownloadMedia(message)
                        }
                    }
                }
                binding.messageResendIcon.setOnClickListener { onResend(message) }
            }
            else -> error("Unknown binding type: $binding")
        }
    }

    private fun addReactions(rootView: LinearLayout, message: MessageListViewItem) {
        rootView.removeAllViews()
        Timber.d("Adding reactions: ${message.reactions}")
        message.reactions.forEach { reaction ->
            if (reaction.value.isNotEmpty()) {
                val emoji = LayoutInflater.from(rootView.context).inflate(R.layout.view_reaction_item, rootView, false)
                emoji.emoji_icon.setText(reaction.key.emoji)
                emoji.emoji_counter.text = reaction.value.size.toString()
                emoji.emoji_counter.visibility = if (reaction.value.size > 1) View.VISIBLE else View.GONE
                emoji.setOnClickListener {
                    onReactionClicked(reaction.key, message.index)
                }
                rootView.addView(emoji)
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
