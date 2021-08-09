package com.twilio.conversations.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.app.data.models.ConversationListViewItem
import com.twilio.conversations.app.databinding.RowConversationItemBinding
import kotlin.properties.Delegates

class ConversationListAdapter(private val callback: OnConversationEvent) : RecyclerView.Adapter<ConversationListAdapter.ViewHolder>() {

    var conversations: List<ConversationListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowConversationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = conversations.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.conversation = conversations[position]
        holder.binding.conversationItem.setOnClickListener {
            holder.binding.conversation?.sid?.let { callback.onConversationClicked(it) }
        }
    }

    fun isMuted(position: Int) = conversations[position].isMuted

    class ViewHolder(val binding: RowConversationItemBinding) : RecyclerView.ViewHolder(binding.root)

    class ConversationDiff(private val oldItems: List<ConversationListViewItem>,
                            private val newItems: List<ConversationListViewItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition].sid == newItems[newItemPosition].sid
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldItems[oldItemPosition] == newItems[newItemPosition]
        }
    }
}

interface OnConversationEvent {

    fun onConversationClicked(conversationSid: String)
}
