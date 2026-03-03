package com.synapse.social.studioasinc.core.util

import android.content.Context
import androidx.biometric.BiometricManager



interface BiometricChecker {
    fun canAuthenticate(context: Context): Int
}



class BiometricCheckerImpl : BiometricChecker {
    override fun canAuthenticate(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    }
}
