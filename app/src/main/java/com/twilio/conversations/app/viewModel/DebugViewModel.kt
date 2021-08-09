package com.twilio.conversations.app.viewModel

import androidx.lifecycle.ViewModel
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.app.repository.ConversationsRepository

class DebugViewModel(private val conversationsRepository: ConversationsRepository) : ViewModel() {

    fun simulateCrash(where: CrashIn) = conversationsRepository.simulateCrash(where)
}
