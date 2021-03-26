package com.twilio.conversations.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.twilio.conversations.app.data.models.ParticipantListViewItem
import com.twilio.conversations.app.databinding.RowParticipantItemBinding
import kotlin.properties.Delegates

class ParticipantListAdapter(private val onParticipantClicked: (participant: ParticipantListViewItem) -> Unit) : RecyclerView.Adapter<ParticipantListAdapter.ViewHolder>() {

    var participants: List<ParticipantListViewItem> by Delegates.observable(emptyList()) { _, old, new ->
        DiffUtil.calculateDiff(ConversationDiff(old, new)).dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RowParticipantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = participants.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.binding.participant = participants[position]
        holder.binding.participantItem.setOnClickListener {
            holder.binding.participant?.let { onParticipantClicked(it) }
        }
    }

    class ViewHolder(val binding: RowParticipantItemBinding) : RecyclerView.ViewHolder(binding.root)

    class ConversationDiff(private val oldItems: List<ParticipantListViewItem>,
                            private val newItems: List<ParticipantListViewItem>
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
