package otp.google

import java.security.interfaces.RSAPublicKey
import otp.google.TestTokens.CLIENT
import otp.google.TestTokens.KID
import otp.google.TestTokens.NOW
import otp.google.TestTokens.SUB
import otp.google.TestTokens.mint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Every check in GoogleTokens, attacked. Tokens are minted by TestTokens, so
 * a real Google token is never needed and nothing here touches the network.
 */
class GoogleTest {

    private var clock = NOW
    private var fetches = 0
    private var published: Map<String, RSAPublicKey> = TestTokens.published()

    private val verifier = GoogleTokens(
        clientId = CLIENT,
        jwks = Jwks { fetches++; published },
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
        val offline = GoogleTokens(CLIENT, Jwks { throw java.io.IOException("no route") })
        assertNull(offline.subjectOf(mint()))
    }
}
