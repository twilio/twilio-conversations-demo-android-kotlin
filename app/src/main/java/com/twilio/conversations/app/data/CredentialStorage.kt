package com.twilio.conversations.app.data

import android.content.Context
import androidx.preference.PreferenceManager
import timber.log.Timber
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class CredentialStorage(applicationContext: Context) {

    var identity by stringPreference()
        private set

    var password by stringPreference()
        private set

    var fcmToken by stringPreference()

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)

    private fun stringPreference() = object : ReadWriteProperty<Any?, String> {

        override fun getValue(thisRef: Any?, property: KProperty<*>): String {
            Timber.d("CredentialStorage getValue()")
            return sharedPreferences.getString(property.name, "")!!
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) {
            Timber.d("CredentialStorage setValue()")
            sharedPreferences.edit()
                .putString(property.name, value)
                .apply()
        }
    }

    fun isEmpty(): Boolean = identity.isEmpty() || password.isEmpty()

    fun clearCredentials() {
        Timber.d("clearCredentials")
        sharedPreferences.edit().clear().apply()
    }

    fun storeCredentials(identity: String, password: String) {
        this.identity = identity
        this.password = password
    }
}
