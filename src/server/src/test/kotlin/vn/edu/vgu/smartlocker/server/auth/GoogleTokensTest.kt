package vn.edu.vgu.smartlocker.server.auth

import vn.edu.vgu.smartlocker.server.auth.TestTokens.CLIENT
import vn.edu.vgu.smartlocker.server.auth.TestTokens.KID
import vn.edu.vgu.smartlocker.server.auth.TestTokens.NOW
import vn.edu.vgu.smartlocker.server.auth.TestTokens.SUB
import vn.edu.vgu.smartlocker.server.auth.TestTokens.mint
import java.security.interfaces.RSAPublicKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every check in GoogleTokens, attacked. Tokens are minted by TestTokens, so
 * a real Google token is never needed and nothing here touches the network.
 */
class GoogleTokensTest {

    private var clock = NOW
    private var fetches = 0
    private var published: Map<String, RSAPublicKey> = TestTokens.published()

    private val verifier = GoogleTokens(
        clientId = CLIENT,
        jwks = Jwks { fetches++; published },
        allowedDomains = TestTokens.ALLOWED,
        now = { clock },
    )

    @Test
    fun aGenuineTokenYieldsItsSubject() {
        assertEquals(SUB, verifier.subjectOf(mint()))
    }

    @Test
    fun algNoneIsRefused() {
        // Signed properly, but declaring "none". A verifier that trusts the
        // header would take this, so the assert really does rest on the alg
        // check rather than on the signature failing anyway.
        assertNull(verifier.subjectOf(mint(alg = "none")))

        // And the classic shape: no signature at all.
        val header = TestTokens.b64("""{"alg":"none","kid":"$KID","typ":"JWT"}""")
        val claims = TestTokens.b64(
            TestTokens.claims("https://accounts.google.com", CLIENT, SUB, NOW / 1000 + 3600),
        )
        assertNull(verifier.subjectOf("$header.$claims."))
    }

    @Test
    fun swappingRsaForHmacIsRefused() {
        // The classic: claim HS256 so a lazy verifier checks the token with
        // the public key as a shared secret, which anybody can read.
        assertNull(verifier.subjectOf(mint(alg = "HS256")))
    }

    @Test
    fun aTokenMintedForAnotherAppIsRefused() {
        assertNull(verifier.subjectOf(mint(aud = "999.apps.googleusercontent.com")))
    }

    @Test
    fun aTokenFromAnotherIssuerIsRefused() {
        assertNull(verifier.subjectOf(mint(iss = "https://accounts.evil.example")))
    }

    @Test
    fun bothOfGooglesIssuerSpellingsAreAccepted() {
        assertEquals(SUB, verifier.subjectOf(mint(iss = "accounts.google.com")))
        assertEquals(SUB, verifier.subjectOf(mint(iss = "https://accounts.google.com")))
    }

    @Test
    fun anExpiredTokenIsRefused() {
        val expiresAt = clock / 1000 + 1
        assertEquals(SUB, verifier.subjectOf(mint(expSec = expiresAt)))
        clock += 2_000
        assertNull(verifier.subjectOf(mint(expSec = expiresAt)))
    }

    @Test
    fun aTokenSignedByAnybodyElseIsRefused() {
        assertNull(verifier.subjectOf(mint(signWith = TestTokens.attacker)))
    }

    @Test
    fun changingTheClaimsAfterSigningIsRefused() {
        assertNull(verifier.subjectOf(mint(tamperedSub = "somebody-else")))
    }

    @Test
    fun rubbishIsRefused() {
        listOf("", "...", "a.b", "a.b.c.d", "not a token", "$%^.&*(.)_+")
            .forEach { assertNull(verifier.subjectOf(it), "accepted <$it>") }
    }

    @Test
    fun madeUpKeyIdsCannotBeUsedToHammerGoogle() {
        assertEquals(SUB, verifier.subjectOf(mint()))
        assertEquals(1, fetches)

        repeat(20) { assertNull(verifier.subjectOf(mint(kid = "made-up-$it"))) }
        assertEquals(1, fetches, "an unknown key id fetched again inside the minute")

        clock += 60_001
        assertNull(verifier.subjectOf(mint(kid = "still-unknown")))
        assertEquals(2, fetches, "a minute on, one more fetch should be allowed")
    }

    @Test
    fun aRotatedKeyIsPickedUp() {
        published = emptyMap()
        assertNull(verifier.subjectOf(mint()))
        published = TestTokens.published()
        clock += 60_001
        assertEquals(SUB, verifier.subjectOf(mint()))
    }

    @Test
    fun aKeySourceThatIsDownRefusesRatherThanThrows() {
        val offline = GoogleTokens(
            CLIENT,
            Jwks { throw java.io.IOException("no route") },
            TestTokens.ALLOWED,
        )
        assertNull(offline.subjectOf(mint()))
    }

    // --- The hosted domain. Task P2-11, ADR 0026 ---------------------------

    /**
     * Staff and students, and the dot between them.
     *
     * `student.vgu.edu.vn` is not in the allow-list and is accepted anyway,
     * because it sits under a domain that is. That is the rule the whole
     * university runs on: one entry, both populations.
     */
    @Test
    fun `both university domains sign in`() {
        assertEquals(SUB, verifier.subjectOf(mint(hd = "vgu.edu.vn")))
        assertEquals(SUB, verifier.subjectOf(mint(hd = "student.vgu.edu.vn")))
        assertEquals(SUB, verifier.subjectOf(mint(hd = "STUDENT.VGU.EDU.VN")))
    }

    /**
     * The one that pays for this test file.
     *
     * `notvgu.edu.vn` is a real domain anybody can register for a few
     * dollars, and a bare `endsWith("vgu.edu.vn")` accepts it. So does a
     * `contains`. Both would hand a stranger an account on this locker.
     * `vgu.edu.vn.example.com` is the same trick from the other end, for a
     * check written as `startsWith`.
     */
    @Test
    fun `a domain that merely looks like ours is refused`() {
        listOf(
            "notvgu.edu.vn",
            "vgu.edu.vn.example.com",
            "vgu-edu-vn.example.com",
            "student.vgu.edu.vn.evil.example",
            "gmail.com",
        ).forEach { assertNull(verifier.subjectOf(mint(hd = it)), "accepted <$it>") }
    }

    /**
     * A personal Google account carries no `hd` at all, so requiring one is
     * the whole gate. The claim present but empty is the same refusal.
     */
    @Test
    fun `a token with no hosted domain is refused`() {
        assertNull(verifier.subjectOf(mint(hd = null)))
        assertNull(verifier.subjectOf(mint(hd = "")))
    }

    /**
     * The domain refusal is told apart from every other one - the single
     * exception in this file, argued in `Refusal.GOOGLE_DOMAIN`. A student
     * who tapped the button with their personal Gmail has to be told which
     * account to use; somebody probing a signature is told nothing.
     */
    @Test
    fun `only the domain refusal names itself`() {
        assertEquals(GoogleTokens.Check.WrongDomain, verifier.check(mint(hd = "gmail.com")))
        assertEquals(
            GoogleTokens.Check.Invalid,
            verifier.check(mint(hd = "gmail.com", signWith = TestTokens.attacker)),
        )
        assertEquals(
            GoogleTokens.Check.Invalid,
            verifier.check(mint(aud = "999.apps.googleusercontent.com")),
        )
    }

    /**
     * The domain is read from claims that were **signed**, never from the
     * ones that arrived. A token minted for a personal account and then
     * edited to say `vgu.edu.vn` is invalid, not merely wrong-domain: the
     * signature covers the honest claims.
     */
    @Test
    fun `a hosted domain edited in after signing is refused`() {
        val header = TestTokens.b64("""{"alg":"RS256","kid":"$KID","typ":"JWT"}""")
        val exp = NOW / 1000 + 3600
        val honest = TestTokens.b64(
            TestTokens.claims("https://accounts.google.com", CLIENT, SUB, exp, "gmail.com"),
        )
        val forged = TestTokens.b64(
            TestTokens.claims("https://accounts.google.com", CLIENT, SUB, exp, "vgu.edu.vn"),
        )
        val signature = java.security.Signature.getInstance("SHA256withRSA").run {
            initSign(TestTokens.google.private)
            update("$header.$honest".toByteArray(Charsets.US_ASCII))
            TestTokens.b64(sign())
        }
        assertEquals(GoogleTokens.Check.Invalid, verifier.check("$header.$forged.$signature"))
    }
}
