package com.twilio.conversations.app

import android.app.Application
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.ConversationsClient.LogLevel
import com.twilio.conversations.app.common.LineNumberDebugTree
import com.twilio.conversations.app.common.injector
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.LocalCacheProvider
import com.twilio.conversations.app.repository.ConversationsRepositoryImpl
import com.twilio.conversations.app.ui.LoginActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class ConversationsApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val appForegroundObserver = AppForegroundObserver()

    private var isForegrounded = false

    private var runOnForeground = {}

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            ConversationsClient.setLogLevel(LogLevel.DEBUG)
            Timber.plant(LineNumberDebugTree("Demo"))
        }

        FirebaseAnalytics.getInstance(this)
        FirebaseApp.initializeApp(this)
        EmojiCompat.init(BundledEmojiCompatConfig(this))
        ConversationsClientWrapper.createInstance(this)
        LocalCacheProvider.createInstance(this)
        ConversationsRepositoryImpl.createInstance(ConversationsClientWrapper.INSTANCE, LocalCacheProvider.INSTANCE)

        ProcessLifecycleOwner.get().lifecycle.addObserver(appForegroundObserver)

        ConversationsClientWrapper.INSTANCE.onUpdateTokenFailure += { signOut() }
    }

    private fun signOut() = applicationScope.launch {
        val loginManager = injector.createLoginManager(applicationContext)
        loginManager.signOut()

        startLoginActivityWhenInForeground()
    }

    private fun startLoginActivityWhenInForeground() {
        if (isForegrounded) {
            LoginActivity.start(this)
        } else {
            runOnForeground = { LoginActivity.start(this) }
        }
    }

    private inner class AppForegroundObserver : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            isForegrounded = false
        }

        override fun onStart(owner: LifecycleOwner) {
            isForegrounded = true

            runOnForeground()
            runOnForeground = {}
        }
    }
}
