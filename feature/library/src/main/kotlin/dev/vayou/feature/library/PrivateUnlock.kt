package dev.vayou.feature.library

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity

/**
 * Asks for the phone's own lock before the folder opens, which is the whole of what makes it
 * private.
 *
 * The device credential and not a PIN of the app's own: one more secret to remember is one more to
 * forget, and the phone already holds one the viewer uses every day. A fingerprint stands in for it
 * where there is one.
 *
 * A phone with nothing enrolled is let through rather than shut out of its own files. There is no
 * lock to ask for, and refusing would make the folder a place films go and never come back from.
 */
@Composable
internal fun PrivateUnlockEffect(onUnlocked: () -> Unit, onCancelled: () -> Unit) {
    val context = LocalContext.current
    val title = stringResource(R.string.private_videos)

    LaunchedEffect(Unit) {
        val authenticators = BIOMETRIC_WEAK or DEVICE_CREDENTIAL
        val canAuthenticate = BiometricManager.from(context).canAuthenticate(authenticators)
        if (canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ||
            canAuthenticate == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
        ) {
            onUnlocked()
            return@LaunchedEffect
        }
        // The prompt is a fragment, so it needs the activity that can host one. Anything else is a
        // build that cannot ask, and the same reasoning applies: let it through rather than lock it.
        val activity = context as? FragmentActivity ?: run {
            onUnlocked()
            return@LaunchedEffect
        }
        BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onUnlocked()

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onCancelled()
            },
        ).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(authenticators)
                .build(),
        )
    }
}
