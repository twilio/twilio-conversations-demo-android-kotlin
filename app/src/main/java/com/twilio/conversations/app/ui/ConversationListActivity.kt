package com.twilio.conversations.app.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.twilio.conversations.app.R
import com.twilio.conversations.app.common.SheetListener
import com.twilio.conversations.app.common.enums.CrashIn
import com.twilio.conversations.app.common.extensions.*
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.ui.fragments.ConversationFragment
import kotlinx.android.synthetic.main.activity_conversations_list.*
import kotlinx.android.synthetic.main.view_conversation_add_screen.*
import kotlinx.android.synthetic.main.view_drawer_header.view.*
import kotlinx.android.synthetic.main.view_user_profile_screen.*
import timber.log.Timber

class ConversationListActivity : AppCompatActivity() {

    private val addConversationSheet by lazy { BottomSheetBehavior.from(add_conversation_sheet) }
    private val userProfileSheet by lazy { BottomSheetBehavior.from(user_profile_sheet) }
    private val sheetListener by lazy { SheetListener(sheet_background) { hideKeyboard() } }
    private val progressDialog: AlertDialog by lazy {
        AlertDialog.Builder(this)
            .setCancelable(false)
            .setView(R.layout.view_loading_dialog)
            .create()
    }
    private lateinit var toggle: ActionBarDrawerToggle

    val conversationsListViewModel by lazyViewModel { injector.createConversationListViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversations_list)

        initViews()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu_conversation_list, menu)

        val filterMenuItem = menu.findItem(R.id.filter_conversations)
        if (conversationsListViewModel.conversationFilter.isNotEmpty()) {
            filterMenuItem.expandActionView()
        }
        (filterMenuItem.actionView as SearchView).apply {
            queryHint = getString(R.string.conversation_filter_hint)
            if (conversationsListViewModel.conversationFilter.isNotEmpty()) {
                setQuery(conversationsListViewModel.conversationFilter, false)
            }
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = true

                override fun onQueryTextChange(newText: String): Boolean {
                    conversationsListViewModel.conversationFilter = newText
                    return true
                }

            })
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_conversation_add -> switchConversationAddDialog()
        }
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        toggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        toggle.onConfigurationChanged(newConfig)
    }

    override fun onBackPressed() {
        var handled = false
        supportFragmentManager.fragments.forEach {
            if (it is ConversationFragment && it.onBackPressed()) {
                handled = true
            }
        }
        if (addConversationSheet.isShowing()) {
            hideConversationAddSheet()
        } else if (userProfileSheet.isShowing()) {
            userProfileSheet.hide()
        } else if (conversation_drawer_layout.isDrawerOpen(GravityCompat.START)) {
            conversation_drawer_layout.closeDrawers()
        } else if (!handled) {
            super.onBackPressed()
        }
    }

    private fun initViews() {
        setSupportActionBar(conversation_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.activity_title_conversations_list)
        toggle = ActionBarDrawerToggle(this, conversation_drawer_layout, conversation_toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        toggle.isDrawerIndicatorEnabled = true
        toggle.syncState()

        conversation_drawer_layout.addDrawerListener(toggle)
        conversation_drawer_layout.addDrawerListener(object: DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                hideKeyboard()
            }
        })

        addConversationSheet.addBottomSheetCallback(sheetListener)
        userProfileSheet.addBottomSheetCallback(sheetListener)
        sheet_background.setOnClickListener {
            hideConversationAddSheet()
            userProfileSheet.hide()
        }

        user_profile_update_button.setOnClickListener {
            conversationsListViewModel.setFriendlyName(user_profile_friendly_name.text.toString())
        }

        user_profile_cancel_button.setOnClickListener {
            userProfileSheet.hide()
        }

        add_conversation_button.setOnClickListener {
            val conversationName = conversation_name_input.text.toString()
            conversationsListViewModel.createConversation(conversationName)
        }

        add_conversation_cancel_button.setOnClickListener {
            userProfileSheet.hide()
        }

        drawer_sign_out_button.setOnClickListener {
            Timber.d("Sign out clicked")
            conversation_drawer_layout.closeDrawers()
            conversationsListViewModel.signOut()
        }

        drawer_java_crash.setOnClickListener {
            throw RuntimeException("Simulated crash in ConversationListActivity.kt")
        }

        drawer_tm_crash.setOnClickListener {
            conversationsListViewModel.simulateCrash(CrashIn.TM_CLIENT_CPP)
        }

        drawer_chat_crash.setOnClickListener {
            conversationsListViewModel.simulateCrash(CrashIn.CHAT_CLIENT_CPP)
        }

        conversation_drawer_menu.getHeaderView(0).drawer_settings_button.setOnClickListener {
            Timber.d("Drawer settings clicked")
            conversation_drawer_layout.closeDrawers()
            userProfileSheet.show()
        }

        conversationsListViewModel.onConversationCreated.observe(this, {
            hideConversationAddSheet()
        })

        conversationsListViewModel.onConversationError.observe(this, { error ->
            conversations_list_layout.showSnackbar(getErrorMessage(error))
            hideConversationAddSheet()
            userProfileSheet.hide()
        })

        conversationsListViewModel.isDataLoading.observe(this, { showLoading ->
            if (showLoading) {
                progressDialog.show()
            } else {
                progressDialog.hide()
            }
        })

        conversationsListViewModel.selfUser.observe(this, { user ->
            Timber.d("Self user received: ${user.friendlyName} ${user.identity}")
            conversation_drawer_menu.getHeaderView(0).drawer_participant_name.text = user.friendlyName
            conversation_drawer_menu.getHeaderView(0).drawer_participant_info.text = user.identity
            user_profile_friendly_name.setText(user.friendlyName)
            user_profile_identity.text = user.identity
        })

        conversationsListViewModel.onUserUpdated.observe(this, {
            userProfileSheet.hide()
        })

        conversationsListViewModel.onSignedOut.observe(this, {
            LoginActivity.start(this)
        })
    }

    private fun switchConversationAddDialog() {
        if (!addConversationSheet.isShowing()) {
            addConversationSheet.show()
        } else {
            hideConversationAddSheet()
        }
    }

    private fun hideConversationAddSheet() {
        conversation_name_input.text?.clear()
        addConversationSheet.hide()
    }

    companion object {

        fun start(context: Context) {
            val intent = getStartIntent(context)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        }

        fun getStartIntent(context: Context) =
            Intent(context, ConversationListActivity::class.java)
    }
}
