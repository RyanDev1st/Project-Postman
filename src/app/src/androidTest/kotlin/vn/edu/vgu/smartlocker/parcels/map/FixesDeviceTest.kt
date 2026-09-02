package vn.edu.vgu.smartlocker.parcels.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The **fused** half of [Fixes], on a runtime that actually has Play Services.
 *
 *     ./gradlew :app:connectedDebugAndroidTest -Pemulator
 *
 * **Why this exists.** `Fixes.kt` forks twice on `fusedAvailable()`, once in
 * [freshCachedFix] and once in [locationUpdates]. Until 2026-09-02 only the
 * `else` leg had ever run: the `parity` AVD is an AOSP image with no Play
 * Services, so every location figure this project has ever quoted came from
 * the `LocationManager` fallback. The fused leg compiled and shipped
 * unexecuted.
 *
 * This runs on `parity-gms`, an identical AVD on
 * `system-images;android-36;google_apis;x86_64`. [playServicesArePresent] is
 * first on purpose: if it fails, the rest of this file is measuring the
 * fallback again and the numbers mean nothing.
 *
 * **How to read the fix count.** The interesting number is not printed here.
 * The test holds a listener open for [WINDOW_MS] and logs each delivery under
 * [TAG]; what the platform was asked for is read from outside, while this is
 * running:
 *
 *     adb shell dumpsys location | grep -A3 fused
 *     adb logcat -s FixesDeviceTest
 *
 * Positions are injected from the host, not from here - `adb emu geo fix`
 * takes **longitude first**, delivers once rather than continuously, and its
 * first fix after a boot does not land.
 *
 * **What this does not prove.** No real GPS, no real radio, no cold start in a
 * building. An emulator's fused provider is Play Services reading a synthetic
 * NMEA feed, so it proves the branch is wired and bounded - not what a phone
 * in a pocket on the campus costs.
 */
@RunWith(AndroidJUnit4::class)
class FixesDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Grant location to the app under test, from inside the run.
     *
     * **This is not ceremony.** A `pm grant` typed at a shell does not
     * survive: `connectedDebugAndroidTest` reinstalls the APK on every
     * invocation and runtime permissions go with the old uid. Without this,
     * [locationUpdates] takes its `!hasLocationPermission` branch, closes the
     * flow, and the test reports **zero fixes and passes** - a check that
     * cannot fail, measuring nothing. That happened here on 2026-09-02 and
     * three green results had to be thrown away.
     *
     * `uiAutomation.grantRuntimePermission` rather than `GrantPermissionRule`,
     * which would mean adding `androidx.test:rules` for one line.
     */
    @Before
    fun grantLocation() {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val pkg = context.packageName
        automation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION)
        automation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION)
        assertEquals(
            "the grant did not take - every fix count below would be a false zero",
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION),
        )
    }

    /**
     * The premise. `Fixes.fusedAvailable` is private, so this asserts the same
     * condition against the same API rather than reaching into it: if this
     * passes, the two forks below take their fused leg.
     */
    @Test
    fun playServicesArePresent() {
        val status = GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context)
        assertEquals(
            "not a Google APIs image - every figure here would be the " +
                "LocationManager fallback again",
            ConnectionResult.SUCCESS,
            status,
        )
    }

    /**
     * The cached fix, through `FusedLocationProviderClient.lastLocation`.
     *
     * Null is a pass. The age gate in [freshCachedFix] is the whole point of
     * it, and a fresh emulator has nothing young enough to return; what is
     * being checked is that the fused call **answers** rather than hanging on
     * a Task nobody completes, which is the failure this leg could have had
     * all along without anyone noticing.
     */
    @Test
    fun theCachedFusedFixAnswers() {
        val answered = runBlocking {
            withTimeoutOrNull(ANSWER_MS) {
                freshCachedFix(context)
                true
            }
        }
        assertTrue(
            "lastLocation did not answer within ${ANSWER_MS}ms",
            answered == true,
        )
    }

    /**
     * A stationary phone, listening, for a minute and a half.
     *
     * The fallback's figure for this window was **zero fixes**, because
     * `MIN_MOVE_M` gates delivery on movement. Fused merges sensors the
     * fallback never saw, so this is the number that had to be measured rather
     * than assumed. Every delivery is logged with the millisecond it arrived,
     * so the spacing can be read against `MIN_INTERVAL_MS` afterwards.
     */
    @Test
    fun aStationaryPhoneIsNotPolled() {
        var fixes = 0
        val started = System.currentTimeMillis()
        runBlocking {
            withTimeoutOrNull(WINDOW_MS) {
                locationUpdates(context).collectLatest { here ->
                    fixes++
                    Log.i(
                        TAG,
                        "fix $fixes at +${System.currentTimeMillis() - started}ms " +
                            "lat=${here.latitude} lon=${here.longitude} " +
                            "provider=${here.provider} accuracy=${here.accuracy}",
                    )
                }
            }
        }
        Log.i(TAG, "window closed: $fixes fixes in ${WINDOW_MS}ms")
        // Not an equality. The point is a ceiling: whatever fused decides to
        // deliver, it must not be delivering faster than the request allows.
        val ceiling = (WINDOW_MS / MIN_INTERVAL_FLOOR_MS).toInt() + 1
        assertTrue(
            "$fixes fixes in ${WINDOW_MS}ms is above the $ceiling the " +
                "request permits - the interval is not being honoured",
            fixes <= ceiling,
        )
    }

    /**
     * **Not a test of shipped code.** A control, to tell two explanations of a
     * zero apart.
     *
     * [aStationaryPhoneIsNotPolled] measured zero fixes, and `adb emu geo fix`
     * was injected during its window and did not arrive. Two things could
     * cause that, and they mean opposite things: the emulator is not
     * delivering injected positions at all, or the app's own request is not
     * asking for them hard enough. The dump says the second - with
     * `PRIORITY_BALANCED_POWER_ACCURACY` the only registration Play Services
     * puts on the gps provider is `PASSIVE`, so nothing drives GPS and an
     * injected fix has no active client to land on.
     *
     * This asks the same fused client for [Priority.PRIORITY_HIGH_ACCURACY],
     * which does drive GPS. If fixes arrive here and not there, priority is
     * the whole difference, and the shipped request is untestable on an
     * emulator rather than broken.
     */
    @Test
    fun highAccuracyIsWhatMakesAnInjectedFixArrive() {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 1_000L,
        ).setMinUpdateDistanceMeters(0f).build()
        var fixes = 0
        val started = System.currentTimeMillis()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let {
                    fixes++
                    Log.i(
                        TAG,
                        "HIGH_ACCURACY fix $fixes at " +
                            "+${System.currentTimeMillis() - started}ms " +
                            "lat=${it.latitude} lon=${it.longitude} " +
                            "provider=${it.provider}",
                    )
                }
            }
        }
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        try {
            runBlocking { withTimeoutOrNull(CONTROL_MS) { awaitCancellation() } }
        } finally {
            client.removeLocationUpdates(callback)
        }
        Log.i(TAG, "control closed: $fixes HIGH_ACCURACY fixes in ${CONTROL_MS}ms")
    }

    /**
     * **Not a test of shipped code.** The second control, and the one that
     * decides what the zeros mean.
     *
     * This bypasses Play Services entirely and asks `LocationManager` for the
     * gps provider directly - the same call the fallback leg makes, and the
     * one that did receive `adb emu geo fix` on the AOSP `parity` AVD. If this
     * also reports zero, then this emulator is not delivering injected
     * positions to anything and no conclusion about fused can be drawn from
     * the other two. If it reports fixes, the difference is Play Services.
     */
    @Test
    fun theManagerGpsProviderReceivesAnInjectedFix() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var fixes = 0
        val started = System.currentTimeMillis()
        val listener = LocationListener {
            fixes++
            Log.i(
                TAG,
                "MANAGER fix $fixes at +${System.currentTimeMillis() - started}ms " +
                    "lat=${it.latitude} lon=${it.longitude} provider=${it.provider}",
            )
        }
        lm.requestLocationUpdates(
            LocationManager.GPS_PROVIDER, 0L, 0f, listener, Looper.getMainLooper(),
        )
        try {
            runBlocking { withTimeoutOrNull(CONTROL_MS) { awaitCancellation() } }
        } finally {
            lm.removeUpdates(listener)
        }
        Log.i(TAG, "manager control closed: $fixes gps fixes in ${CONTROL_MS}ms")
    }

    private companion object {
        const val TAG = "FixesDeviceTest"

        /** The control window. Long enough to inject into from the host. */
        const val CONTROL_MS = 40_000L

        /** The stationary window. Matches what the fallback was measured over. */
        const val WINDOW_MS = 90_000L

        /** How long `lastLocation` gets to come back before it counts as hung. */
        const val ANSWER_MS = 10_000L

        /**
         * `MIN_INTERVAL_MS` in `Fixes.kt`, repeated because it is private
         * there. If that constant moves, this ceiling moves with it.
         */
        const val MIN_INTERVAL_FLOOR_MS = 10_000L
    }
}
