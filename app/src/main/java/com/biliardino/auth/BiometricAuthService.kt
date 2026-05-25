package com.biliardino.auth

import androidx.biometric.BiometricManager
import android.content.Context

object BiometricAuthService {
    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
