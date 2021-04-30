package com.twilio.conversations.app

import android.app.Application
import android.content.Intent
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
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
import com.twilio.conversations.app.ui.SplashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

class ConversationsApplication : Application(), LifecycleObserver {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)

        ConversationsClientWrapper.INSTANCE.onUpdateTokenFailure += { signOut() }

        startSplashActivity()
    }

    private fun startSplashActivity() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        Timber.d("startActivity SplashActivity")
        startActivity(intent)
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

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        isForegrounded = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        isForegrounded = true

        runOnForeground()
        runOnForeground = {}
    }
}
