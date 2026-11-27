package com.example.healthlog.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
    AVAILABLE,
    NO_CREDENTIAL_ENROLLED,
    UNAVAILABLE
}

interface AppAuthenticator {
    fun checkAvailability(context: Context): BiometricAvailability

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    )
}

/**
 * Default [AppAuthenticator] backed by AndroidX [BiometricPrompt] supporting biometrics
 * (fingerprint, face unlock) as well as device lock screen credentials (PIN, pattern, password).
 */
object AndroidBiometricAuthenticator : AppAuthenticator {

    // BIOMETRIC_WEAK includes Class 3 (strong) and Class 2 (weak) biometrics and is safely
    // combinable with DEVICE_CREDENTIAL across API 26–37.
    private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    override fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return when (biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS)) {
                BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
                BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NO_CREDENTIAL_ENROLLED
                else -> {
                    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    if (keyguard?.isDeviceSecure == true) {
                        BiometricAvailability.AVAILABLE
                    } else {
                        BiometricAvailability.UNAVAILABLE
                    }
                }
            }
        }

        // On API 26–29, check weak biometrics and KeyguardManager.isDeviceSecure separately.
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val hasDeviceLock = keyguard?.isDeviceSecure == true
        val biometricStatus = biometricManager.canAuthenticate(BIOMETRIC_WEAK)
        return when {
            biometricStatus == BiometricManager.BIOMETRIC_SUCCESS || hasDeviceLock ->
                BiometricAvailability.AVAILABLE
            keyguard != null || biometricStatus == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                BiometricAvailability.NO_CREDENTIAL_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }

    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (String?) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    val isUserCancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    onError(if (isUserCancelled) null else errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
            .build()

        prompt.authenticate(promptInfo)
    }
}
