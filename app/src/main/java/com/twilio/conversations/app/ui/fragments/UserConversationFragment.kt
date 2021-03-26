package com.twilio.conversations.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.twilio.conversations.app.R
import com.twilio.conversations.app.adapters.ConversationListAdapter
import kotlinx.android.synthetic.main.fragment_conversations_list.*

class UserConversationFragment : ConversationFragment() {

    private val adapter by lazy { ConversationListAdapter(this) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.fragment_conversations_list, container, false)!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        conversationRefresh.setOnRefreshListener { conversationListViewModel.getUserConversations() }
        conversationList.adapter = adapter
        conversationListViewModel.userConversationItems.observe(viewLifecycleOwner, {
            adapter.conversations = it
            conversationRefresh.isRefreshing = false
        })
    }
}
