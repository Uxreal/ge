package dev.lumen.launcher.feature.drawer

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

/**
 * D41: the gate on the hidden shelf, on the framework `BiometricPrompt` — present since API 29,
 * so no new dependency (§0's pinned list holds). `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` means
 * fingerprint, face, PIN, pattern or password all satisfy it: the shelf is exactly as protected
 * as the phone's lock screen, which is the reference behaviour.
 *
 * A device with no lock set at all fails enrollment checks inside `authenticate` and reports an
 * error; [onResult] false leaves the shelf closed. Locked means locked.
 */
internal fun authenticateHidden(context: Context, onResult: (Boolean) -> Unit) {
    var current: Context? = context
    while (current is ContextWrapper && current !is Activity) current = current.baseContext
    val activity = current as? Activity ?: return onResult(false)

    runCatching {
        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Hidden apps")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL,
            )
            .build()
        prompt.authenticate(
            CancellationSignal(),
            ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) =
                    onResult(true)

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) =
                    onResult(false)
            },
        )
    }.onFailure { onResult(false) }
}
