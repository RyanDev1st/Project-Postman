package otp

/**
 * Where a code goes. [send] returns true only when the carrier confirmed
 * delivery — a code that was never sent must not become usable.
 */
interface SmsProvider {
    fun send(toE164: String, content: String): Boolean
}
