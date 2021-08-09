package com.twilio.conversations.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.R
import com.twilio.conversations.app.adapters.ParticipantListAdapter
import com.twilio.conversations.app.common.SheetListener
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.common.injector
import kotlinx.android.synthetic.main.activity_participants.*
import kotlinx.android.synthetic.main.view_participant_details_screen.*
import timber.log.Timber

class ParticipantListActivity : BaseActivity() {

    private val sheetBehavior by lazy { BottomSheetBehavior.from(participant_details_sheet) }
    private val sheetListener by lazy { SheetListener(sheet_background) }

    val participantListViewModel by lazyViewModel {
        injector.createParticipantListViewModel(intent.getStringExtra(EXTRA_CONVERSATION_SID)!!)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_participants)

        initViews()
    }

    override fun onBackPressed() {
        if (sheetBehavior.isShowing()) {
            sheetBehavior.hide()
            return
        }
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_participant_list, menu)

        val filterMenuItem = menu.findItem(R.id.filter_participants)
        if (participantListViewModel.participantFilter.isNotEmpty()) {
            filterMenuItem.expandActionView()
        }
        (filterMenuItem.actionView as SearchView).apply {
            queryHint = getString(R.string.participant_filter_hint)
            if (participantListViewModel.participantFilter.isNotEmpty()) {
                setQuery(participantListViewModel.participantFilter, false)
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String): Boolean {
                    participantListViewModel.participantFilter = newText
                    return true
                }
            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.filter_participants -> sheetBehavior.hide()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        setSupportActionBar(conversation_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        conversation_toolbar.setNavigationOnClickListener { onBackPressed() }
        sheetBehavior.addBottomSheetCallback(sheetListener)
        title = getString(R.string.participant_title)
        val adapter = ParticipantListAdapter { participant ->
            Timber.d("Participant clicked: $participant")
            participantListViewModel.selectedParticipant = participant
            participant_details_name.text = participant.friendlyName
            participant_details_status.setText(if (participant.isOnline) R.string.participant_online else R.string.participant_offline)
            sheetBehavior.show()
        }

        participantRefresh.setOnRefreshListener { participantListViewModel.getConversationParticipants() }
        participantList.adapter = adapter

        sheet_background.setOnClickListener {
            sheetBehavior.hide()
        }

        participant_details_remove.setOnClickListener {
            Timber.d("Participant remove clicked: ${participantListViewModel.selectedParticipant?.sid}")
            participantListViewModel.removeSelectedParticipant()
            sheetBehavior.hide()
        }

        participantListViewModel.participantsList.observe(this, { participants ->
            Timber.d("Participants received: $participants")
            adapter.participants = participants
            participantRefresh.isRefreshing = false
        })
        participantListViewModel.onParticipantError.observe(this, { error ->
            participantListLayout.showSnackbar(getErrorMessage(error))
        })
    }

    companion object {

        private const val EXTRA_CONVERSATION_SID = "ExtraConversationSid"

        fun start(context: Context, conversationSid: String) =
            context.startActivity(getStartIntent(context, conversationSid))

        fun getStartIntent(context: Context, conversationSid: String) =
            Intent(context, ParticipantListActivity::class.java).putExtra(EXTRA_CONVERSATION_SID, conversationSid)
    }
}
