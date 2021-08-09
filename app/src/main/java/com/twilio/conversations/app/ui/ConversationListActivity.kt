package com.twilio.conversations.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.twilio.conversations.app.R
import com.twilio.conversations.app.databinding.ActivityConversationListBinding
import com.twilio.conversations.app.ui.fragments.ConversationListFragment
import com.twilio.conversations.app.ui.fragments.DebugFragment
import com.twilio.conversations.app.ui.fragments.ProfileFragment
import timber.log.Timber

class ConversationListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("onCreate")
        super.onCreate(savedInstanceState)

        val binding = ActivityConversationListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.page_conversation_list -> replaceFragment(ConversationListFragment())
                R.id.page_profile -> replaceFragment(ProfileFragment())
                R.id.page_debug -> replaceFragment(DebugFragment())
            }
            return@setOnItemSelectedListener true
        }

        if (savedInstanceState == null) {
            replaceFragment(ConversationListFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Timber.d("replaceFragment")

        supportFragmentManager.findFragmentById(R.id.fragment_container)?.let { currentFragment ->
            if (currentFragment::class == fragment::class) {
                Timber.d("replaceFragment: skip")
                return
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow()
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
