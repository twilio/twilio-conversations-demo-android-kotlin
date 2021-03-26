@file:Suppress("IncorrectScope")

package com.twilio.conversations.app.testUtil

import com.twilio.conversations.app.data.CredentialStorage

const val VALID_CREDENTIAL = "e"
const val INVALID_CREDENTIAL = ""
const val OUTDATED_CREDENTIAL = "outdated"

fun credentialStorageNotEmpty(credentialStorage: CredentialStorage, credential: String, fcmToken: String = INVALID_CREDENTIAL) {
    whenCall(credentialStorage.isEmpty()).thenReturn(false)
    whenCall(credentialStorage.identity).thenReturn(credential)
    whenCall(credentialStorage.password).thenReturn(credential)
    whenCall(credentialStorage.fcmToken).thenReturn(fcmToken)
}

fun credentialStorageEmpty(credentialStorage: CredentialStorage) {
    whenCall(credentialStorage.isEmpty()).thenReturn(true)
    whenCall(credentialStorage.identity).thenReturn(INVALID_CREDENTIAL)
    whenCall(credentialStorage.password).thenReturn(INVALID_CREDENTIAL)
    whenCall(credentialStorage.fcmToken).thenReturn(INVALID_CREDENTIAL)
}
