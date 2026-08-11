package otp.password

import java.nio.file.Files
import kotlin.io.path.deleteRecursively
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountsTest {

    private val phone = "+84908619328"
    private var clock = 1_786_000_000_000L

    /** A store on a real file, so a restart can be acted out. */
    private fun <T> onDisk(block: (dir: java.nio.file.Path, open: () -> Accounts) -> T): T {
        val dir = Files.createTempDirectory("accounts-test")
        return try {
            block(dir) { Accounts(dir.resolve("accounts.db").toString()) { clock } }
        } finally {
            @OptIn(kotlin.io.path.ExperimentalPathApi::class)
            dir.deleteRecursively()
        }
    }

    @Test
    fun aPasswordThatWasSetLetsYouIn() = onDisk { _, open ->
        open().use { accounts ->
            assertNull(accounts.setPassword(phone, "a good password"))
            assertNull(accounts.check(phone, "a good password"))
        }
    }

    @Test
    fun theWrongPasswordDoesNot() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            assertEquals("WRONG_PASSWORD", accounts.check(phone, "a bad password"))
        }
    }

    @Test
    fun aNumberNobodyRegisteredAnswersTheSameWay() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            // Identical wording, so this endpoint cannot be used to ask
            // which phone numbers are registered here.
            assertEquals("WRONG_PASSWORD", accounts.check("+84900000000", "anything"))
            assertEquals("WRONG_PASSWORD", accounts.check(phone, "anything"))
            assertFalse(accounts.hasPassword("+84900000000"))
        }
    }

    @Test
    fun fiveWrongTriesLockTheAccountEvenAgainstTheRightPassword() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            repeat(5) { assertEquals("WRONG_PASSWORD", accounts.check(phone, "wrong")) }
            assertEquals(
                "WRONG_PASSWORD",
                accounts.check(phone, "a good password"),
                "the right password still worked after five wrong ones",
            )
        }
    }

    @Test
    fun theLockLiftsWhenItsTimeIsUp() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            repeat(5) { accounts.check(phone, "wrong") }
            assertEquals("WRONG_PASSWORD", accounts.check(phone, "a good password"))
            clock += 15 * 60 * 1000 + 1
            assertNull(accounts.check(phone, "a good password"))
        }
    }

    @Test
    fun theLockoutSurvivesTheServerBeingKilled() = onDisk { _, open ->
        // The rule this whole class exists for. A counter in memory resets
        // when the process dies, so anyone who can crash the server - or
        // just wait for a power cut - gets their five guesses back.
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            repeat(5) { accounts.check(phone, "wrong") }
        }
        open().use { restarted ->
            assertEquals(
                "WRONG_PASSWORD",
                restarted.check(phone, "a good password"),
                "the lockout was forgotten across a restart",
            )
        }
        clock += 15 * 60 * 1000 + 1
        open().use { later -> assertNull(later.check(phone, "a good password")) }
    }

    @Test
    fun thePasswordItselfSurvivesTheServerBeingKilled() = onDisk { _, open ->
        open().use { it.setPassword(phone, "a good password") }
        open().use { assertNull(it.check(phone, "a good password")) }
    }

    @Test
    fun countingStartsAgainAfterASuccess() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            repeat(4) { accounts.check(phone, "wrong") }
            assertNull(accounts.check(phone, "a good password"))
            // Four more should not lock, because the counter went back to nil.
            repeat(4) { assertEquals("WRONG_PASSWORD", accounts.check(phone, "wrong")) }
            assertNull(accounts.check(phone, "a good password"))
        }
    }

    @Test
    fun settingAPasswordClearsALockout() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "a good password")
            repeat(5) { accounts.check(phone, "wrong") }
            // Getting back in by one-time code is the reset path, and it
            // must not leave the person locked out of what they just set.
            assertNull(accounts.setPassword(phone, "a different password"))
            assertNull(accounts.check(phone, "a different password"))
        }
    }

    @Test
    fun aPasswordThatIsTooShortIsNeverStored() = onDisk { _, open ->
        open().use { accounts ->
            assertEquals("PASSWORD_TOO_SHORT", accounts.setPassword(phone, "short"))
            assertFalse(accounts.hasPassword(phone))
        }
    }

    @Test
    fun changingItRetiresTheOldOne() = onDisk { _, open ->
        open().use { accounts ->
            accounts.setPassword(phone, "the first password")
            accounts.setPassword(phone, "the second password")
            assertEquals("WRONG_PASSWORD", accounts.check(phone, "the first password"))
            assertNull(accounts.check(phone, "the second password"))
        }
    }

    @Test
    fun twoPhonesDoNotShareALockout() = onDisk { _, open ->
        open().use { accounts ->
            val other = "+84908619329"
            accounts.setPassword(phone, "a good password")
            accounts.setPassword(other, "another password")
            repeat(5) { accounts.check(phone, "wrong") }
            assertTrue(accounts.check(other, "another password") == null)
        }
    }
}
