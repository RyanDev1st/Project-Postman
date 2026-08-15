package vn.edu.vgu.smartlocker.pickup

import android.graphics.BitmapFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * The reader half of the pickup, on a real Android runtime.
 *
 *     ./gradlew :app:connectedDebugAndroidTest -Pemulator
 *
 * **Why this cannot be a JVM test.** `SessionCodeTest` covers every rule about
 * the string, and covers it in a millisecond, because those rules are plain
 * Kotlin. What it cannot touch is ML Kit: the detector is native code behind a
 * Play Services shim, and Robolectric has no stand-in for it. The question
 * "does the picture the cabinet draws actually decode on an Android phone" has
 * only ever had one honest answer, which is to run it on one.
 *
 * **What the picture is.** `assets/cabinet-qr.png` is the shipped cabinet
 * encoder's own output - `src/cabinet/vendor/qrcode.js`, the same file that
 * runs on the screen - and it was read back with `zxing-cpp`, an unrelated
 * decoder, before it was saved. It is a fixture, not a fake: nothing in it was
 * written by hand.
 *
 * To draw a new one:
 *
 *     python -c "import sys; sys.path.insert(0,'scripts'); import qrimage; \
 *       im, v = qrimage.draw('<payload>'); \
 *       im.save('src/app/src/androidTest/assets/cabinet-qr.png')"
 *
 * **What this does not prove.** No lens, no glass, no corridor. It proves the
 * code decodes and the app believes the right thing about it. Whether a phone
 * held at arm's length reads it off a lit screen is P5-02's real Verify, and
 * that needs a cabinet.
 */
@RunWith(AndroidJUnit4::class)
class ScanPipelineTest {

    /**
     * A code shaped the way the server issues them - `Sessions.format`.
     *
     * The fourth field is 43 characters because that is what 32 random bytes
     * come to in unpadded base64url, and its length is the point: it pushes
     * the code to QR version 5, which is the size a phone will actually be
     * asked to read off a lit screen. A shorter fixture would be an easier
     * picture than the real one.
     */
    private val payload =
        "VGU1|vgu-back-gate-01|1786690000|Zm9yLXRlc3RzLW9ubHktbm90LWEtcmVhbC1zZXNzaW9"
    private val issued = 1_786_690_000L

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )

    /** The bytes the camera would hand over, straight out of the detector. */
    private fun readAsset(): String? {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        val bitmap = ctx.assets.open("cabinet-qr.png").use { BitmapFactory.decodeStream(it) }
        val codes = Tasks.await(
            scanner.process(InputImage.fromBitmap(bitmap, 0)),
            30, TimeUnit.SECONDS,
        )
        return codes.firstOrNull()?.rawValue
    }

    /**
     * The whole reason ML Kit is a dependency at all. If this fails on a
     * device, ADR 0018 is wrong and zxing-cpp is the answer.
     */
    @Test
    fun mlKitReadsTheCabinetsOwnCode() {
        val raw = readAsset()
        assertEquals("ML Kit did not read the cabinet's QR back as it was written", payload, raw)
    }

    /**
     * Bundled ML Kit is documented as carrying its model in the APK. This
     * emulator has no Google Play Services installed at all, so a read here is
     * the strongest available evidence that the claim holds - and if it ever
     * stops holding, this is the test that says so.
     */
    @Test
    fun theReaderNeedsNoPlayServicesInstalled() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val gms = ctx.packageManager.getInstalledPackages(0)
            .any { it.packageName.startsWith("com.google.android.gms") }
        val raw = readAsset()
        assertEquals(payload, raw)
        println("read the code with Play Services installed = $gms")
    }

    /** Decoded, then judged. The two halves of a scan, joined. */
    @Test
    fun aFreshCodeIsActedOn() {
        val raw = readAsset()
        val read = readSessionCode(raw, issued + 5)
        assertEquals(Scanned.Ours("vgu-back-gate-01", issued), read)
    }

    /** The same picture, read too late. Not an error, and not a door opening. */
    @Test
    fun theSameCodeAnHourLaterIsStale() {
        val raw = readAsset()
        val read = readSessionCode(raw, issued + 3_600)
        assertTrue("a code an hour old was not stale: $read", read is Scanned.Stale)
    }
}
