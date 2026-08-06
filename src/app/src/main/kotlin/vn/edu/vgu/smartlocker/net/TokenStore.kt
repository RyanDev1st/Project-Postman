package vn.edu.vgu.smartlocker.net

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Where the receiver's token lives on the phone.
 *
 * The token is a pass to somebody's parcels, so it is never written in plain
 * text and never logged. It is encrypted with a key held in the **Android
 * Keystore**, which on most phones lives in hardware the app cannot read - the
 * app asks the keystore to encrypt and decrypt, and never sees the key itself.
 * Copying this file off the phone therefore gets you ciphertext and nothing
 * else.
 *
 * No library. `androidx.security:security-crypto` is the usual answer and is
 * deprecated; the keystore underneath it is about sixty lines to use directly,
 * so this uses the thing rather than a wrapper around it.
 *
 * There is deliberately no `toString`, no logging, and no getter that hands
 * out the key. [read] returns the token because [Api] must send it, and that
 * is the only way it leaves this file.
 */
class TokenStore(context: Context) {

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** The token, or null if nobody is logged in. */
    fun read(): String? {
        val stored = prefs.getString(KEY_TOKEN, null) ?: return null
        return try {
            val blob = Base64.decode(stored, Base64.NO_WRAP)
            if (blob.size <= IV_BYTES) return null
            val iv = blob.copyOfRange(0, IV_BYTES)
            val body = blob.copyOfRange(IV_BYTES, blob.size)
            val cipher = Cipher.getInstance(TRANSFORM).apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
            }
            String(cipher.doFinal(body), Charsets.UTF_8)
        } catch (e: Exception) {
            // Unreadable means unusable: the keystore key is gone - the screen
            // lock changed, or the app was restored to another phone. Clear it
            // and let the person register again, which works. Keeping a token
            // that cannot be decrypted only fails later, further from here.
            clear()
            null
        }
    }

    /** Replace the stored token. */
    fun write(token: String) {
        if (token.isBlank()) {
            clear()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORM).apply {
                init(Cipher.ENCRYPT_MODE, key())
            }
            val body = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            // The IV is generated per encryption and stored in front of the
            // ciphertext. It is not a secret; reusing one with the same key
            // would be the mistake, and letting the cipher pick a fresh one
            // each time is what stops that.
            val blob = cipher.iv + body
            prefs.edit().putString(KEY_TOKEN, Base64.encodeToString(blob, Base64.NO_WRAP)).apply()
        } catch (e: Exception) {
            // Better to hold no token than a broken one. The person is asked
            // to register again, rather than shown a screen that never loads.
            clear()
        }
    }

    /** Forget the token. Used at log out, and whenever it stops making sense. */
    fun clear() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    /** Whether anybody is logged in on this phone. */
    fun hasToken(): Boolean = prefs.contains(KEY_TOKEN)

    /**
     * Fetch this app's keystore key, creating it the first time.
     *
     * `setUserAuthenticationRequired` is **off**. Turning it on would demand a
     * fingerprint before every single call, including the list of parcels, and
     * the phone is already behind a lock screen. It belongs on a banking app,
     * not on a locker that opens a box holding somebody's laundry.
     */
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val FILE = "vn.edu.vgu.smartlocker.session"
        const val KEY_TOKEN = "token"
        const val KEY_ALIAS = "smartlocker_token_key"
        const val PROVIDER = "AndroidKeyStore"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
