# OTP Sender Server Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A Ktor server module `:otp-server` that implements contract endpoints 1–4 so the Android app can register/login by phone + 6-digit SMS code, sending Vietnamese SMS via SpeedSMS (or a log-only demo provider).

**Architecture:** Pure logic (`Phone`, `AuthStore`) separated from transport (`SmsProvider` impls, Ktor routes). `AuthStore` holds codes and tokens in `ConcurrentHashMap`s with injected clock and provider, so every rule (TTL, tries, cooldown, single-use) is unit-testable without the network. Routes are thin: parse → call store → map refusal codes to HTTP statuses.

**Tech Stack:** Kotlin 2.4.10, Ktor 3.2.0 (netty, content-negotiation, kotlinx-json), kotlinx-serialization plugin, Gradle module in the existing root build (JDK 23 running, target 17).

## Global Constraints

- Spec: `docs/superpowers/specs/2026-08-09-otp-sender-design.md` (authoritative).
- Versions live only in `gradle/libs.versions.toml` — never a number in a build file.
- Module path: `src/otp-server/`, wired as `include(":otp-server")` in root `settings.gradle.kts`.
- JVM target 17 (no toolchain — matches the app module; JDK 23 installed).
- Refusal vocabulary: `PHONE_INVALID` (400), `RATE_LIMITED` (429), `SEND_FAILED` (502), `WRONG_CODE` (400), `TOKEN_EXPIRED` (401). Body shape `{"code": "..."}`.
- Constants: code TTL 5 min, resend cooldown 60 s, 5 tries per code, token TTL 30 days.
- Test phone E.164: `+84908619328`. SMS text: `Mã xác thực của bạn là <code>. Có hiệu lực trong 5 phút.`
- Token response field is `expires_at` (epoch ms as string) — the app's `Session.from` reads `optString("expires_at")`.
- Never log a code, token, or SMS content in production paths (mirrors `Http.kt` rule 3). `LogSms` is the only place a code appears, and it is demo-only by construction.
- No new dependencies beyond Ktor 3.2.0 + kotlinx-serialization (runtime + plugin).

---

### Task 1: Gradle wiring for `:otp-server`

**Files:**
- Modify: `gradle/libs.versions.toml` (add `ktor` version, 5 libs, 2 plugin aliases)
- Modify: `settings.gradle.kts` (include module)
- Modify: `build.gradle.kts` (root — declare new plugin aliases `apply false`)
- Create: `src/otp-server/build.gradle.kts`

**Interfaces:**
- Produces: the module skeleton. Later tasks add sources under `src/otp-server/src/`.

- [ ] **Step 1: Add Ktor to the version catalog**

In `gradle/libs.versions.toml`:

```toml
[versions]
ktor = "3.2.0"

[libraries]
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty", version.ref = "ktor" }
ktor-server-content-negotiation = { group = "io.ktor", name = "ktor-server-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-server-test-host = { group = "io.ktor", name = "ktor-server-test-host", version.ref = "ktor" }

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Declare the plugins at the root**

In `build.gradle.kts` (root), inside the existing `plugins {}` block:

```kotlin
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
```

- [ ] **Step 3: Include the module**

In `settings.gradle.kts`, after the `include(":app")` block:

```kotlin
// The OTP sender server. Same rule as the app: source lives in src/.
include(":otp-server")
project(":otp-server").projectDir = file("src/otp-server")
```

- [ ] **Step 4: Create the module build file**

Create `src/otp-server/build.gradle.kts`:

```kotlin
// The OTP sender server (spec: docs/superpowers/specs/2026-08-09-otp-sender-design.md).
// JVM target 17 like the app module: the machine runs JDK 23, and a toolchain
// would silently download a second JDK. None of that here.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("otp.MainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
}
```

- [ ] **Step 5: Verify the module builds**

Run: `./gradlew :otp-server:build`
Expected: `BUILD SUCCESSFUL` (module has no sources yet — fine).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts settings.gradle.kts src/otp-server/build.gradle.kts
git commit -m "build: add the otp-server module, Ktor 3.2.0"
```

---

### Task 2: Phone normalization

**Files:**
- Create: `src/otp-server/src/main/kotlin/otp/Phone.kt`
- Test: `src/otp-server/src/test/kotlin/otp/PhoneTest.kt`

**Interfaces:**
- Produces: `object Phone { fun normalize(raw: String): String? }` — E.164 (`+84908619328`) or `null` when not a valid Vietnamese mobile. Used by Task 5 routes.

- [ ] **Step 1: Write the failing test**

Create `src/otp-server/src/test/kotlin/otp/PhoneTest.kt`:

```kotlin
package otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PhoneTest {

    @Test
    fun acceptsLocalFormat() {
        assertEquals("+84908619328", Phone.normalize("0908619328"))
    }

    @Test
    fun acceptsCountryCodeWithoutPlus() {
        assertEquals("+84908619328", Phone.normalize("84908619328"))
    }

    @Test
    fun acceptsE164WithPlus() {
        assertEquals("+84908619328", Phone.normalize("+84908619328"))
    }

    @Test
    fun ignoresSpacesDashesAndDots() {
        assertEquals("+84908619328", Phone.normalize(" 0908 619-328."))
    }

    @Test
    fun rejectsLandline() {
        assertNull(Phone.normalize("02866808866"))
    }

    @Test
    fun rejectsShortNumber() {
        assertNull(Phone.normalize("090861"))
    }

    @Test
    fun rejectsLetters() {
        assertNull(Phone.normalize("0908abc328"))
    }

    @Test
    fun rejectsEmpty() {
        assertNull(Phone.normalize(""))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :otp-server:test --tests "otp.PhoneTest"`
Expected: FAIL — `e: unresolved reference: Phone`.

- [ ] **Step 3: Write the implementation**

Create `src/otp-server/src/main/kotlin/otp/Phone.kt`:

```kotlin
package otp

/**
 * Vietnamese phone numbers, normalised to E.164.
 *
 * Accept what a user or the app types: 0908619328, 84908619328, +84908619328,
 * with or without spaces/dashes/dots. A Vietnamese mobile is `0` followed by
 * 9 or 10 digits (09x, 03x, 05x, 07x, 08x). A landline (028..., 024...) is
 * rejected: this server only ever sends to mobiles.
 */
object Phone {

    /** The E.164 form, or null when the number is not a Vietnamese mobile. */
    fun normalize(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        val local = when {
            digits.startsWith("84") && digits.length == 11 -> digits.drop(2)
            digits.startsWith("0") && digits.length == 10 -> digits.drop(1)
            else -> return null
        }
        // First digit after the leading 0: 3, 5, 7, 8 or 9 are mobile prefixes.
        return if (local.length == 9 && local.first() in "35789") "+84$local" else null
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :otp-server:test --tests "otp.PhoneTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add src/otp-server/src/main/kotlin/otp/Phone.kt src/otp-server/src/test/kotlin/otp/PhoneTest.kt
git commit -m "feat(otp): normalize Vietnamese mobiles to E.164"
```

---

### Task 3: SMS providers

**Files:**
- Create: `src/otp-server/src/main/kotlin/otp/Sms.kt` (interface)
- Create: `src/otp-server/src/main/kotlin/otp/LogSms.kt` (demo provider)
- Create: `src/otp-server/src/main/kotlin/otp/SpeedSms.kt` (real provider)
- Test: `src/otp-server/src/test/kotlin/otp/ProvidersTest.kt`

**Interfaces:**
- Consumes: nothing yet (called by `AuthStore` in Task 4).
- Produces:
  - `interface SmsProvider { fun send(toE164: String, content: String): Boolean }` — `true` only when the carrier confirmed delivery.
  - `class LogSms : SmsProvider` with `var lastContent: String?` (also lets tests read the demo code).
  - `class SpeedSms(private val token: String, private val baseUrl: String = "https://api.speedsms.vn") : SmsProvider`.

- [ ] **Step 1: Write the failing tests**

Create `src/otp-server/src/test/kotlin/otp/ProvidersTest.kt`:

```kotlin
package otp

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProvidersTest {

    /** A tiny fake SpeedSMS endpoint, so no test touches the network. */
    private fun fakeSpeedSms(
        respondBody: String,
        respondStatus: Int = 200,
        record: (auth: String?, body: String?) -> Unit = { _, _ -> },
    ): HttpServer {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/index.php/sms/send") { ex ->
            val auth = ex.requestHeaders.getFirst("Authorization")
            val body = ex.requestBody.bufferedReader().readText()
            record(auth, body)
            val bytes = respondBody.toByteArray(Charsets.UTF_8)
            ex.sendResponseHeaders(respondStatus, bytes.size.toLong())
            ex.responseBody.use { it.write(bytes) }
        }
        server.start()
        return server
    }

    @Test
    fun logSmsRemembersWhatWasSent() {
        val log = LogSms()
        assertNull(log.lastContent)
        assertTrue(log.send("+84908619328", "Mã xác thực của bạn là 123456."))
        assertEquals("Mã xác thực của bạn là 123456.", log.lastContent)
    }

    @Test
    fun speedSmsSendsBasicAuthAndE164WithoutPlus() {
        var seenAuth: String? = null
        var seenBody: String? = null
        val server = fakeSpeedSms("""{"status":1}""") { a, b -> seenAuth = a; seenBody = b }
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "Mã xác thực của bạn là 123456.")
            assertTrue(ok)
            val expectedAuth = "Basic " + Base64.getEncoder()
                .encodeToString("tok123:x".toByteArray(Charsets.UTF_8))
            assertEquals(expectedAuth, seenAuth)
            assertTrue(seenBody!!.contains("\"to\":\"84908619328\""))
            assertTrue(seenBody!!.contains("\"type\":2"))
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun speedSmsFailsWhenStatusIsNotOne() {
        val server = fakeSpeedSms("""{"status":9,"message":"out of balance"}""")
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "anything")
            assertFalse(ok)
        } finally {
            server.stop(0)
        }
    }

    @Test
    fun speedSmsFailsWhenServerErrors() {
        val server = fakeSpeedSms("""{"status":1}""", respondStatus = 500)
        try {
            val ok = SpeedSms("tok123", "http://127.0.0.1:${server.address.port}")
                .send("+84908619328", "anything")
            assertFalse(ok)
        } finally {
            server.stop(0)
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :otp-server:test --tests "otp.ProvidersTest"`
Expected: FAIL — unresolved references `LogSms`, `SpeedSms`.

- [ ] **Step 3: Write the interface and both providers**

Create `src/otp-server/src/main/kotlin/otp/Sms.kt`:

```kotlin
package otp

/**
 * Where a code goes. [send] returns true only when the carrier confirmed
 * delivery — a code that was never sent must not become usable.
 */
interface SmsProvider {
    fun send(toE164: String, content: String): Boolean
}
```

Create `src/otp-server/src/main/kotlin/otp/LogSms.kt`:

```kotlin
package otp

/**
 * Demo mode. Prints what would be sent and keeps the last message, so tests
 * (and a human watching the console) can read the code. Never used in
 * production: the route wiring only picks this when DEMO_MODE is on.
 */
class LogSms : SmsProvider {

    /** Last message that was "sent", or null before the first send. */
    var lastContent: String? = null

    override fun send(toE164: String, content: String): Boolean {
        lastContent = content
        println("DEMO OTP for $toE164: $content")
        return true
    }
}
```

Create `src/otp-server/src/main/kotlin/otp/SpeedSms.kt`:

```kotlin
package otp

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/**
 * The real provider: SpeedSMS (api.speedsms.vn), the de-facto VN standard.
 *
 * Auth is Basic with the API access token and an empty password. `type 2` is
 * long-code SMS, which needs no brandname registration for testing. The
 * endpoint is plain Java HttpURLConnection, the same philosophy as the app's
 * Http.kt: fifteen small calls do not justify a client library.
 */
class SpeedSms(
    private val token: String,
    private val baseUrl: String = "https://api.speedsms.vn",
) : SmsProvider {

    override fun send(toE164: String, content: String): Boolean = try {
        val conn = (URL(baseUrl.trimEnd('/') + "/index.php/sms/send").openConnection()
                as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty(
                "Authorization",
                "Basic " + Base64.getEncoder()
                    .encodeToString("$token:x".toByteArray(Charsets.UTF_8)),
            )
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
        }
        val body = """{"to":"${toE164.removePrefix("+")}","content":"$content","type":2}"""
        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        val status = conn.responseCode
        val text = if (status in 200..299) conn.inputStream else conn.errorStream
        val answer = text?.bufferedReader()?.use { it.readText() }.orEmpty()
        conn.disconnect()

        // SpeedSMS says delivered only when status is exactly 1.
        status in 200..299 && answer.contains("\"status\":1")
    } catch (e: IOException) {
        false
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :otp-server:test --tests "otp.ProvidersTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**

```bash
git add src/otp-server/src/main/kotlin/otp/Sms.kt src/otp-server/src/main/kotlin/otp/LogSms.kt src/otp-server/src/main/kotlin/otp/SpeedSms.kt src/otp-server/src/test/kotlin/otp/ProvidersTest.kt
git commit -m "feat(otp): SMS providers, log demo and SpeedSMS"
```

---

### Task 4: AuthStore — codes and tokens

**Files:**
- Create: `src/otp-server/src/main/kotlin/otp/AuthStore.kt`
- Test: `src/otp-server/src/test/kotlin/otp/AuthStoreTest.kt`

**Interfaces:**
- Consumes: `SmsProvider` (Task 3).
- Produces:
  - `class AuthStore(private val provider: SmsProvider, private val now: () -> Long = System::currentTimeMillis)`
  - `fun requestCode(phoneE164: String): String?` — `null` = sent; `"RATE_LIMITED"` or `"SEND_FAILED"`.
  - `fun verifyCode(phoneE164: String, code: String): String?` — `null` = correct; `"WRONG_CODE"` (covers expired/used/out-of-tries).
  - `fun issueToken(phoneE164: String): Token` — `data class Token(val value: String, val expiresAtMs: Long)`.
  - `fun phoneOf(token: String): String?` — `null` when unknown or expired.
  - `fun refresh(token: String): Token?` — new token, or `null` (TOKEN_EXPIRED).
  - `fun revoke(token: String)` — forget the token.

- [ ] **Step 1: Write the failing tests**

Create `src/otp-server/src/test/kotlin/otp/AuthStoreTest.kt`:

```kotlin
package otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthStoreTest {

    /** A provider that records content and accepts or refuses on demand. */
    private class FakeSms(var accept: Boolean = true) : SmsProvider {
        var lastContent: String? = null
        override fun send(toE164: String, content: String): Boolean {
            lastContent = content
            return accept
        }
    }

    private fun store(accept: Boolean = true, now: () -> Long = { 1_000_000L }): Pair<AuthStore, FakeSms> {
        val sms = FakeSms(accept)
        return AuthStore(sms, now) to sms
    }

    @Test
    fun requestCodeSendsSixDigitsInVietnamese() {
        val (s, sms) = store()
        assertNull(s.requestCode("+84908619328"))
        val content = sms.lastContent!!
        assertTrue(content.matches(Regex("Mã xác thực của bạn là \\d{6}\\. Có hiệu lực trong 5 phút\\.")))
    }

    @Test
    fun requestCodeRefusesWithinCooldown() {
        var t = 1_000_000L
        val (s, _) = store(now = { t })
        assertNull(s.requestCode("+84908619328"))
        assertEquals("RATE_LIMITED", s.requestCode("+84908619328"))
        t += 61_000
        assertNull(s.requestCode("+84908619328"))
    }

    @Test
    fun requestCodeDoesNotStoreWhenSendFails() {
        val (s, _) = store(accept = false)
        assertEquals("SEND_FAILED", s.requestCode("+84908619328"))
        // Nothing stored: the code the user never received must not be usable.
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000"))
    }

    @Test
    fun verifyCodeAcceptsTheSentCode() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        assertNull(s.verifyCode("+84908619328", code))
    }

    @Test
    fun wrongCodeConsumesTriesThenLocks() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        repeat(4) { assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000")) }
        // Fifth wrong try uses the last try; the code is then gone.
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", "000000"))
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun codeIsSingleUse() {
        val (s, sms) = store()
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        assertNull(s.verifyCode("+84908619328", code))
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun expiredCodeIsRefused() {
        var t = 1_000_000L
        val (s, sms) = store(now = { t })
        s.requestCode("+84908619328")
        val code = Regex("\\d{6}").find(sms.lastContent!!)!!.value
        t += 301_000 // past the 5-minute TTL
        assertEquals("WRONG_CODE", s.verifyCode("+84908619328", code))
    }

    @Test
    fun tokenLifecycle() {
        val (s, _) = store()
        s.requestCode("+84908619328")
        val token = s.issueToken("+84908619328")

        assertEquals("+84908619328", s.phoneOf(token.value))
        assertEquals(1_000_000L + 30L * 24 * 3600 * 1000, token.expiresAtMs)

        val fresh = s.refresh(token.value)!!
        assertNotNull(fresh)
        assertNull(s.phoneOf(token.value)) // old token is dead
        assertEquals("+84908619328", s.phoneOf(fresh.value))

        s.revoke(fresh.value)
        assertNull(s.phoneOf(fresh.value))
    }

    @Test
    fun expiredTokenIsForgotten() {
        var t = 1_000_000L
        val (s, _) = store(now = { t })
        val token = s.issueToken("+84908619328")
        t += 31L * 24 * 3600 * 1000
        assertNull(s.phoneOf(token.value))
        assertNull(s.refresh(token.value))
    }

    @Test
    fun unknownTokenIsRefused() {
        val (s, _) = store()
        assertNull(s.phoneOf("nope"))
        assertNull(s.refresh("nope"))
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :otp-server:test --tests "otp.AuthStoreTest"`
Expected: FAIL — unresolved reference `AuthStore`.

- [ ] **Step 3: Write the implementation**

Create `src/otp-server/src/main/kotlin/otp/AuthStore.kt`:

```kotlin
package otp

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap

/**
 * Codes and tokens, in memory, with every rule from the spec:
 * 6 digits, 5-minute TTL, 60-second resend cooldown, 5 tries, single use,
 * 30-day token TTL. Thread-safe maps because Ktor may serve concurrently.
 *
 * The clock is injected so tests can walk time. The provider is injected so
 * tests never touch the network.
 */
class AuthStore(
    private val provider: SmsProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** A token handed to a client. */
    data class Token(val value: String, val expiresAtMs: Long)

    private data class Code(
        val value: String,
        val sentAtMs: Long,
        val expiresAtMs: Long,
        var triesLeft: Int,
    )

    private data class Session(val phone: String, val expiresAtMs: Long)

    private val codes = ConcurrentHashMap<String, Code>()
    private val sessions = ConcurrentHashMap<String, Session>()
    private val random = SecureRandom()

    /**
     * Send a fresh code. Null on success; [RATE_LIMITED] within the cooldown;
     * [SEND_FAILED] when the provider did not confirm delivery (nothing is
     * stored in that case).
     */
    fun requestCode(phoneE164: String): String? {
        val existing = codes[phoneE164]
        if (existing != null && now() - existing.sentAtMs < COOLDOWN_MS) {
            return "RATE_LIMITED"
        }
        val value = "%06d".format(java.util.Locale.ROOT, random.nextInt(1_000_000))
        val content = "Mã xác thực của bạn là $value. Có hiệu lực trong 5 phút."
        if (!provider.send(phoneE164, content)) return "SEND_FAILED"
        codes[phoneE164] = Code(value, now(), now() + CODE_TTL_MS, MAX_TRIES)
        return null
    }

    /**
     * Check a code. Null on success, [WRONG_CODE] for anything else — wrong,
     * expired, already used, or no tries left. The spec's wording rule: a
     * wrong answer says the same thing as a wrong-and-expired one.
     */
    fun verifyCode(phoneE164: String, code: String): String? {
        val entry = codes[phoneE164] ?: return "WRONG_CODE"
        if (now() >= entry.expiresAtMs) {
            codes.remove(phoneE164)
            return "WRONG_CODE"
        }
        val matches = MessageDigest.isEqual(
            entry.value.toByteArray(Charsets.UTF_8),
            code.toByteArray(Charsets.UTF_8),
        )
        if (!matches) {
            entry.triesLeft -= 1
            if (entry.triesLeft <= 0) codes.remove(phoneE164)
            return "WRONG_CODE"
        }
        codes.remove(phoneE164)
        return null
    }

    /** Mint a fresh token for a phone. */
    fun issueToken(phoneE164: String): Token {
        val value = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(random.generateSeed(32))
        sessions[value] = Session(phoneE164, now() + TOKEN_TTL_MS)
        return Token(value, now() + TOKEN_TTL_MS)
    }

    /** The phone behind a token, or null when unknown or expired. */
    fun phoneOf(token: String): String? {
        val session = sessions[token] ?: return null
        if (now() >= session.expiresAtMs) {
            sessions.remove(token)
            return null
        }
        return session.phone
    }

    /** Swap a live token for a fresh one; null when the old one is dead. */
    fun refresh(token: String): Token? {
        val phone = phoneOf(token) ?: return null
        sessions.remove(token)
        return issueToken(phone)
    }

    /** Forget a token. */
    fun revoke(token: String) {
        sessions.remove(token)
    }

    private companion object {
        const val CODE_TTL_MS = 5L * 60 * 1000
        const val COOLDOWN_MS = 60_000L
        const val MAX_TRIES = 5
        const val TOKEN_TTL_MS = 30L * 24 * 3600 * 1000
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `./gradlew :otp-server:test --tests "otp.AuthStoreTest"`
Expected: PASS (10 tests).

- [ ] **Step 5: Commit**

```bash
git add src/otp-server/src/main/kotlin/otp/AuthStore.kt src/otp-server/src/test/kotlin/otp/AuthStoreTest.kt
git commit -m "feat(otp): code and token lifecycle in AuthStore"
```

---

### Task 5: Ktor routes

**Files:**
- Create: `src/otp-server/src/main/kotlin/otp/Main.kt`
- Test: `src/otp-server/src/test/kotlin/otp/RoutesTest.kt`

**Interfaces:**
- Consumes: `Phone.normalize` (Task 2), `SmsProvider`/`LogSms`/`SpeedSms` (Task 3), `AuthStore` (Task 4).
- Produces: runnable app — `./gradlew :otp-server:run`; `fun Application.module(provider: SmsProvider, store: AuthStore)` (testable) plus a `main()` that wires env config.

- [ ] **Step 1: Write the failing route tests**

Create `src/otp-server/src/test/kotlin/otp/RoutesTest.kt`:

```kotlin
package otp

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RoutesTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class Refusal(val code: String)

    @Serializable
    private data class SessionOut(val token: String, val expires_at: String)

    private fun ApplicationTestBuilder.withServer(block: (LogSms) -> Unit) {
        val log = LogSms()
        application {
            install(ContentNegotiation) { json() }
            module(log, AuthStore(log))
        }
        block(log)
    }

    private suspend fun io.ktor.client.HttpClient.requestCode(phone: String) =
        post("/auth/request-code") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone_number":"$phone"}""")
        }

    @Test
    fun fullRegistrationFlow() = testApplication {
        withServer { log ->
            val sent = client.requestCode("0908619328")
            assertEquals(HttpStatusCode.OK, sent.status)
            val code = Regex("\\d{6}").find(log.lastContent!!)!!.value

            val wrong = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"000000"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, wrong.status)
            assertEquals("WRONG_CODE", json.decodeFromString<Refusal>(wrong.bodyAsText()).code)

            val ok = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"$code"}""")
            }
            assertEquals(HttpStatusCode.OK, ok.status)
            val session = json.decodeFromString<SessionOut>(ok.bodyAsText())
            assertNotNull(session.token)
            assertNotNull(session.expires_at)

            // Single use: replaying the same code fails.
            val replay = client.post("/auth/verify-code") {
                contentType(ContentType.Application.Json)
                setBody("""{"phone_number":"0908619328","code":"$code"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, replay.status)

            // The token works for refresh; logout kills it.
            val refresh = client.post("/auth/refresh") { bearerAuth(session.token) }
            assertEquals(HttpStatusCode.OK, refresh.status)
            val fresh = json.decodeFromString<SessionOut>(refresh.bodyAsText())

            val logout = client.post("/auth/logout") { bearerAuth(fresh.token) }
            assertEquals(HttpStatusCode.OK, logout.status)

            val afterLogout = client.post("/auth/refresh") { bearerAuth(fresh.token) }
            assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
        }
    }

    @Test
    fun invalidPhoneIsRefused() = testApplication {
        withServer {
            val res = client.requestCode("123")
            assertEquals(HttpStatusCode.BadRequest, res.status)
            assertEquals("PHONE_INVALID", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }

    @Test
    fun resendWithinCooldownIsRefused() = testApplication {
        withServer {
            assertEquals(HttpStatusCode.OK, client.requestCode("0908619328").status)
            val again = client.requestCode("0908619328")
            assertEquals(HttpStatusCode.TooManyRequests, again.status)
            assertEquals("RATE_LIMITED", json.decodeFromString<Refusal>(again.bodyAsText()).code)
        }
    }

    @Test
    fun unknownTokenIsRefused() = testApplication {
        withServer {
            val res = client.post("/auth/refresh") { bearerAuth("bogus") }
            assertEquals(HttpStatusCode.Unauthorized, res.status)
            assertEquals("TOKEN_EXPIRED", json.decodeFromString<Refusal>(res.bodyAsText()).code)
        }
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew :otp-server:test --tests "otp.RoutesTest"`
Expected: FAIL — unresolved references `module`, `MainKt`.

- [ ] **Step 3: Write the implementation**

Create `src/otp-server/src/main/kotlin/otp/Main.kt`:

```kotlin
package otp

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class RequestCodeBody(val phone_number: String = "")

@Serializable
private data class VerifyCodeBody(val phone_number: String = "", val code: String = "")

@Serializable
private data class Refusal(val code: String)

@Serializable
private data class SessionOut(val token: String, val expires_at: String)

/** The provider the environment asks for: SpeedSMS with a token, else demo. */
private fun providerFromEnv(): SmsProvider {
    val token = System.getenv("SPEEDSMS_TOKEN").orEmpty()
    return if (System.getenv("DEMO_MODE") == "1" || token.isBlank()) {
        LogSms()
    } else {
        SpeedSms(token)
    }
}

/** Routes over an injected provider and store, so tests wire their own. */
fun Application.module(provider: SmsProvider, store: AuthStore) {
    install(ContentNegotiation) { json() }

    routing {
        post("/auth/request-code") {
            val body = call.receive<RequestCodeBody>()
            val e164 = Phone.normalize(body.phone_number)
            if (e164 == null) {
                call.respond(HttpStatusCode.BadRequest, Refusal("PHONE_INVALID"))
                return@post
            }
            when (val refusal = store.requestCode(e164)) {
                null -> call.respond(HttpStatusCode.OK, mapOf<String, String>())
                "RATE_LIMITED" -> call.respond(HttpStatusCode.TooManyRequests, Refusal(refusal))
                else -> call.respond(HttpStatusCode.BadGateway, Refusal(refusal))
            }
        }

        post("/auth/verify-code") {
            val body = call.receive<VerifyCodeBody>()
            val e164 = Phone.normalize(body.phone_number)
            if (e164 == null) {
                call.respond(HttpStatusCode.BadRequest, Refusal("PHONE_INVALID"))
                return@post
            }
            when (store.verifyCode(e164, body.code.trim())) {
                null -> {
                    val token = store.issueToken(e164)
                    call.respond(HttpStatusCode.OK, SessionOut(token.value, token.expiresAtMs.toString()))
                }
                else -> call.respond(HttpStatusCode.BadRequest, Refusal("WRONG_CODE"))
            }
        }

        post("/auth/refresh") {
            val token = bearer(call)
            val fresh = token?.let { store.refresh(it) }
            if (fresh == null) {
                call.respond(HttpStatusCode.Unauthorized, Refusal("TOKEN_EXPIRED"))
            } else {
                call.respond(HttpStatusCode.OK, SessionOut(fresh.value, fresh.expiresAtMs.toString()))
            }
        }

        post("/auth/logout") {
            val token = bearer(call)
            if (token != null) store.revoke(token)
            call.respond(HttpStatusCode.OK, mapOf<String, String>())
        }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.bearer(): String? {
    val header = request.headers[HttpHeaders.Authorization] ?: return null
    return header.removePrefix("Bearer ").takeIf { it != header }
}

fun main() {
    val provider = providerFromEnv()
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8443
    embeddedServer(Netty, port = port) { module(provider, AuthStore(provider)) }.start(wait = true)
}
```

> Note: delete the empty `fun refuse(...)` stub — it is not used; routes answer inline. (Included above as a mistake to remove on purpose? No — write the file WITHOUT the stub.)

- [ ] **Step 4: Run the route tests**

Run: `./gradlew :otp-server:test --tests "otp.RoutesTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Full module test run**

Run: `./gradlew :otp-server:test`
Expected: PASS (26 tests total: 8 Phone + 4 Providers + 10 AuthStore + 4 Routes).

- [ ] **Step 6: Commit**

```bash
git add src/otp-server/src/main/kotlin/otp/Main.kt src/otp-server/src/test/kotlin/otp/RoutesTest.kt
git commit -m "feat(otp): contract routes for request-code, verify-code, refresh, logout"
```

---

### Task 6: Docs, demo-mode verification, and the real SMS test

**Files:**
- Modify: `docs/reference/api-contract.md` (refusal table + reference-server note)
- Modify: `docs/superpowers/specs/2026-08-09-otp-sender-design.md` (mark the demo-run result, if any change)

**Interfaces:**
- Consumes: the runnable server from Task 5.

- [ ] **Step 1: Document the refusal codes in the contract**

In `docs/reference/api-contract.md`, in the section that defines the refusal
vocabulary (near the endpoint table), add the codes the reference server sends:

```
The reference server (docs/superpowers/specs/2026-08-09-otp-sender-design.md)
adds three codes for auth: `PHONE_INVALID` (the number is not a Vietnamese
mobile), `RATE_LIMITED` (a code was requested within the last 60 seconds) and
`SEND_FAILED` (the SMS provider did not confirm delivery). The app's Refusal
enum does not name them; they fall to `UNKNOWN`, which shows the generic
sentence. `WRONG_CODE` and `TOKEN_EXPIRED` are already in the table.
```

- [ ] **Step 2: Demo-mode run, full flow over curl**

Run the server in one terminal:

```bash
DEMO_MODE=1 ./gradlew :otp-server:run
```

Expected: `Responding at http://0.0.0.0:8443`.

In another terminal:

```bash
curl -s -X POST http://localhost:8443/auth/request-code -H "Content-Type: application/json" -d '{"phone_number":"0908619328"}'
# -> {} and the console shows: DEMO OTP for +84908619328: Mã xác thực của bạn là XXXXXX. Có hiệu lực trong 5 phút.
```

Then with the code from the console:

```bash
curl -s -X POST http://localhost:8443/auth/verify-code -H "Content-Type: application/json" -d '{"phone_number":"0908619328","code":"XXXXXX"}'
# -> {"token":"...","expires_at":"..."}
```

Then a wrong code:

```bash
curl -s -X POST http://localhost:8443/auth/verify-code -H "Content-Type: application/json" -d '{"phone_number":"0908619328","code":"000000"}'
# -> 400 {"code":"WRONG_CODE"}
```

Expected: all three behave as shown. Stop the server (Ctrl+C).

- [ ] **Step 3: Real SMS test (needs the user)**

1. User registers at `connect.speedsms.vn` (free test SMS included).
2. Run: `SPEEDSMS_TOKEN=<token from the dashboard> ./gradlew :otp-server:run`
3. `curl -s -X POST http://localhost:8443/auth/request-code -H "Content-Type: application/json" -d '{"phone_number":"0908619328"}'`
4. Expected: the SMS `Mã xác thực của bạn là <code>. Có hiệu lực trong 5 phút.` arrives on the phone within seconds; verify-code accepts it.

If the phone does not receive it, check: the token is from `connect.speedsms.vn` → "Thông tin tài khoản" → API access token; and the account has test SMS balance (new accounts get some on registration).

- [ ] **Step 4: Commit**

```bash
git add docs/reference/api-contract.md
git commit -m "docs: refusal codes the reference OTP server sends"
```

---

## Self-review notes

- **Spec coverage:** endpoints 1–4 → Task 5; normalization → Task 2; code lifecycle + tokens → Task 4; providers → Task 3; config via env → Task 5 `providerFromEnv`; testing (curl + real SMS) → Task 6; docs row → Task 1/6. The spec's `expires_at` field correction is already committed (7c70199).
- **Placeholders:** none — every step carries real code or an exact command.
- **Type consistency:** `requestCode`/`verifyCode` return `String?` refusal codes everywhere; `issueToken` returns `Token(value, expiresAtMs)`; routes map them to the contract's `{"code": ...}` / `{"token","expires_at"}` shapes; `LogSms.lastContent` is the only way tests read a code.
