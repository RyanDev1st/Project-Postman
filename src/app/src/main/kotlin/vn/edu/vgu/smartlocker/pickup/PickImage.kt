package vn.edu.vgu.smartlocker.pickup

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

/**
 * Read a QR out of a picture the person chose, instead of out of the camera.
 *
 * **Why this exists.** A phone with no working camera — borrowed, broken lens,
 * or an emulator on a desk — could not reach the scan path at all, and the
 * typed code is not a way in either: it is typed at the cabinet, not here
 * (P5-08). A picture of the cabinet screen taken by somebody standing next to
 * the person is the remaining way, and on a development machine it is the only
 * one.
 *
 * **It opens nothing extra.** The QR is a cabinet id, a clock reading and a
 * random field; it is not a key, and identity comes from the token in the app
 * — ADR 0003, and `SessionCode.kt` says the same. So a picked picture is worth
 * exactly what a pointed camera is worth, no more. The code still expires in
 * [CODE_GOOD_FOR_SECONDS], so a screenshot from last week is refused by the
 * same reader that refuses a stale frame, and the server refuses it again.
 *
 * **No permission is asked.** The system photo picker hands back one item the
 * person chose, and never the gallery. Asking for READ_MEDIA_IMAGES to do the
 * same job would be asking for the whole library to read one picture.
 *
 * Returns the thing to call when the button is tapped.
 */
@Composable
internal fun rememberPickedQr(
    /** A QR was found and it is one of ours, stale or not. The raw string. */
    onCode: (String) -> Unit,
    /** A picture came back with no cabinet code in it. The screen says so —
     * the person deliberately chose that file and is owed an answer, unlike
     * the camera, which sees things nobody pointed it at. */
    onNoCode: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val found by rememberUpdatedState(onCode)
    val missing by rememberUpdatedState(onNoCode)

    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    DisposableEffect(scanner) {
        onDispose { scanner.close() }
    }

    val pick = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        // Null means the person backed out of the picker. Backing out is not
        // a failure and says nothing on the screen.
        if (uri != null) read(context, scanner, uri, found, missing)
    }

    return {
        pick.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }
}

/**
 * One picture, once.
 *
 * Every way this can fail — an unreadable file, a picture with no code in it,
 * a detector that gave up — ends at the same sentence. They are one thing to
 * the person holding the phone: that picture did not work, choose another.
 */
private fun read(
    context: Context,
    scanner: BarcodeScanner,
    uri: Uri,
    onCode: (String) -> Unit,
    onNoCode: () -> Unit,
) {
    val image = try {
        InputImage.fromFilePath(context, uri)
    } catch (e: IOException) {
        onNoCode()
        return
    }

    scanner.process(image)
        .addOnSuccessListener { codes ->
            // A picture may hold more than one QR. The first ours wins, so a
            // photograph that caught a poster beside the cabinet screen still
            // finds the cabinet.
            val raw = codes.mapNotNull { it.rawValue }
                .firstOrNull { readSessionCode(it, seconds()) !is Scanned.NotOurs }
            if (raw == null) onNoCode() else onCode(raw)
        }
        .addOnFailureListener { onNoCode() }
}

private fun seconds(): Long = System.currentTimeMillis() / 1000
