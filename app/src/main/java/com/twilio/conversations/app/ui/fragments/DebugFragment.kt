package com.twilio.conversations.app.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.app.common.extensions.lazyViewModel
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.databinding.FragmentDebugBinding
import timber.log.Timber

class DebugFragment : Fragment() {

    lateinit var binding: FragmentDebugBinding

    val debugViewModel by lazyViewModel { injector.createDebugViewModel() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDebugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Timber.d("onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        requireActivity().title = getString(R.string.title_debug)

        binding.crashInConversationListFragment.setOnClickListener {
            throw RuntimeException("Simulated crash in ConversationListActivity.kt")
        }

        binding.crashInChatClientCpp.setOnClickListener {
            debugViewModel.simulateCrash(CrashIn.CHAT_CLIENT_CPP)
        }

        binding.crashInTmClientCpp.setOnClickListener {
            debugViewModel.simulateCrash(CrashIn.TM_CLIENT_CPP)
        }
    }
}
