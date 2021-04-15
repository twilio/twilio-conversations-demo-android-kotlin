package com.twilio.conversations.app

import android.app.Application
import android.content.Intent
import androidx.emoji.bundled.BundledEmojiCompatConfig
import androidx.emoji.text.EmojiCompat
import com.google.firebase.FirebaseApp
import com.twilio.conversations.ConversationsClient
import com.twilio.conversations.ConversationsClient.LogLevel
import com.twilio.conversations.app.common.LineNumberDebugTree
import com.twilio.conversations.app.data.ConversationsClientWrapper
import com.twilio.conversations.app.data.localCache.LocalCacheProvider
import com.twilio.conversations.app.repository.ConversationsRepositoryImpl
import com.twilio.conversations.app.ui.SplashActivity
import timber.log.Timber

class ConversationsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            ConversationsClient.setLogLevel(LogLevel.DEBUG)
            Timber.plant(LineNumberDebugTree("Demo"))
        }

        FirebaseApp.initializeApp(this)
        EmojiCompat.init(BundledEmojiCompatConfig(this))
        ConversationsClientWrapper.createInstance()
        LocalCacheProvider.createInstance(this)
        ConversationsRepositoryImpl.createInstance(ConversationsClientWrapper.INSTANCE, LocalCacheProvider.INSTANCE)

        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        Timber.d("startActivity SplashActivity")
        startActivity(intent)
    }
}
