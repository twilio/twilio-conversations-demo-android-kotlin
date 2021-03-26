package com.twilio.conversations.app.testUtil

import com.twilio.conversations.app.INVALID_IDENTITY
import com.twilio.conversations.app.INVALID_PASSWORD
import com.twilio.conversations.app.VALID_IDENTITY
import com.twilio.conversations.app.VALID_PASSWORD
import com.twilio.conversations.app.data.CredentialStorage

fun setValidCredentials(credentialStorage: CredentialStorage) {
    credentialStorage.storeCredentials(VALID_IDENTITY, VALID_PASSWORD)
}

fun setInvalidCredentials(credentialStorage: CredentialStorage) {
    credentialStorage.storeCredentials(INVALID_IDENTITY, INVALID_PASSWORD)
}
