package vn.edu.vgu.smartlocker.server

import io.ktor.network.tls.certificates.buildKeyStore
import io.ktor.network.tls.certificates.saveToFile
import io.ktor.network.tls.extensions.HashAlgorithm
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.KeyStore

/**
 * The certificate the server serves TLS with.
 *
 * **HTTPS is not optional here and there is no flag to turn it off.** The
 * app refuses a URL that is not `https://` before it opens a socket
 * (`net/Http.kt`), and the cabinet screen does the same (`net.js`). Both were
 * written that way on purpose, and a development server that spoke plain HTTP
 * would mean loosening the one rule that is never loosened later.
 *
 * So development gets a real certificate that happens to be self-signed.
 *
 * **It is generated once and then reused.** Building a fresh one per run would
 * change the fingerprint every time, and nothing that has to trust it - a
 * phone, a cabinet screen, an ESP32 - could ever be set up. The folder is
 * git-ignored: a private key in a repo is a private key in every clone.
 */
object Tls {

    const val ALIAS = "vgu-locker"

    /**
     * Not a secret.
     *
     * A keystore password protects a file that is already git-ignored and
     * already only on this machine. The private key is the thing that matters
     * and it never leaves that folder. Writing a random one here would mean
     * storing it beside the file it protects, which protects nothing and adds
     * a way for a working setup to stop working.
     */
    private const val DEV_PASSWORD = "development"

    fun password(): CharArray = DEV_PASSWORD.toCharArray()

    fun keyStoreFile(dir: File) = File(dir, "locker.jks")

    /**
     * Load the development certificate, making it the first time.
     *
     * The names it is valid for matter more than they look. A certificate is
     * checked against the host in the URL, so every way the server will be
     * reached has to be in it before anything is installed anywhere:
     *
     * - `localhost` and `127.0.0.1` - curl and the cabinet screen on this machine
     * - `10.0.2.2` - what the Android emulator calls this machine
     * - every LAN address this machine has - a real phone, and the ESP32
     */
    fun keyStore(dir: File): KeyStore {
        val file = keyStoreFile(dir)
        if (file.exists()) {
            return KeyStore.getInstance("JKS").apply {
                file.inputStream().use { load(it, password()) }
            }
        }

        dir.mkdirs()
        val store = buildKeyStore {
            certificate(ALIAS) {
                password = DEV_PASSWORD
                hash = HashAlgorithm.SHA256

                // 3072 bits, and not the 1024 this builder defaults to.
                //
                // A 1024-bit key is refused outright by OpenSSL, by Chrome and
                // by Android - "EE certificate key too weak" - so the default
                // produces a certificate that starts a server nothing can
                // connect to. Found by a checker that verified properly
                // instead of skipping verification.
                keySizeInBits = 3072

                // Host names here, addresses below. An IP written as a domain
                // lands in the certificate as a DNS name and does not match
                // when a phone connects to the address.
                domains = listOf("localhost")
                ipAddresses = (listOf("127.0.0.1", "10.0.2.2") + lanAddresses())
                    .mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }

                daysValid = 825
            }
        }
        store.saveToFile(file, DEV_PASSWORD)
        exportCertificate(store, dir)
        return store
    }

    /**
     * Write the public certificate out on its own, in PEM.
     *
     * This is the file a person installs on a phone and on the cabinet screen.
     * It carries no private key, so it is the one part of this folder that is
     * safe to send to somebody over chat.
     */
    private fun exportCertificate(store: KeyStore, dir: File) {
        val cert = store.getCertificate(ALIAS) ?: return
        val base64 = java.util.Base64.getMimeEncoder(64, "\n".toByteArray())
            .encodeToString(cert.encoded)
        File(dir, "locker.crt").writeText(
            "-----BEGIN CERTIFICATE-----\n$base64\n-----END CERTIFICATE-----\n",
        )
    }

    /** Every IPv4 address this machine answers on, so a phone can reach it. */
    fun lanAddresses(): List<String> = try {
        NetworkInterface.getNetworkInterfaces().asSequence()
            .filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.asSequence() }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
            .distinct()
            .toList()
    } catch (e: Exception) {
        emptyList()
    }
}
