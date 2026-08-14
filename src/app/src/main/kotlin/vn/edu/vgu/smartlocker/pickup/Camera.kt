package vn.edu.vgu.smartlocker.pickup

import android.annotation.SuppressLint
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

/**
 * The camera, and a reader looking at every frame it produces.
 *
 * The screen this sits behind used to be a drawing: a dark box with four
 * animated corners and a sweep, and no camera anywhere. It looked exactly
 * like a scanner and could not scan, which is the same fault the one-time
 * code field had - a picture of an input.
 *
 * **QR only.** `setBarcodeFormats(FORMAT_QR_CODE)` is not tidiness; it is
 * speed. Told one format, the detector stops trying the dozen others on
 * every frame, and the frames are arriving from a phone held up in a
 * corridor where every millisecond of latency is a wobble.
 *
 * **KEEP_ONLY_LATEST.** Frames arrive faster than they can be read. The
 * other policy queues them, and a queue means reading a picture of where the
 * phone was pointing a second ago - the scanner appears to lag, then fires
 * on something the person has already moved away from.
 */
@Composable
internal fun QrCamera(
    onCode: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onCode)

    val view = remember { PreviewView(context) }

    // One background thread for the reader. Analysis must not run on the main
    // thread - it would stall the very frames it is reading, and the sweep
    // above it would stutter in a way a person reads as "broken".
    val worker = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    DisposableEffect(Unit) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            val provider = future.get()
            bind(provider, owner, view, worker, scanner) { latest(it) }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            ProcessCameraProvider.getInstance(context).get().unbindAll()
            scanner.close()
            worker.shutdown()
        }
    }

    AndroidView(factory = { view }, modifier = modifier)
}

private fun bind(
    provider: ProcessCameraProvider,
    owner: androidx.lifecycle.LifecycleOwner,
    view: PreviewView,
    worker: java.util.concurrent.ExecutorService,
    scanner: BarcodeScanner,
    onCode: (String) -> Unit,
) {
    val preview = Preview.Builder().build().also {
        it.surfaceProvider = view.surfaceProvider
    }

    // 1280x720 is asked for, not demanded - CameraX picks the nearest the
    // device actually offers. Enough detail to resolve the modules of a QR
    // from arm's length, and small enough that a mid-range phone reads every
    // frame instead of falling behind.
    val analysis = ImageAnalysis.Builder()
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        android.util.Size(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    )
                )
                .build()
        )
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()

    analysis.setAnalyzer(worker) { frame -> read(frame, scanner, onCode) }

    provider.unbindAll()
    provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
}

/**
 * One frame.
 *
 * `close()` runs whatever happens. An unclosed frame holds its buffer, the
 * pool runs dry, and the camera stops delivering - the picture freezes with
 * no error anywhere, which is the worst way for this to fail.
 */
@SuppressLint("UnsafeOptInUsageError")
private fun read(frame: ImageProxy, scanner: BarcodeScanner, onCode: (String) -> Unit) {
    val media = frame.image
    if (media == null) {
        frame.close()
        return
    }
    val image = InputImage.fromMediaImage(media, frame.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { codes ->
            codes.firstOrNull()?.rawValue?.let(onCode)
        }
        .addOnCompleteListener { frame.close() }
}

/** Whether the camera may be opened at all. Read before the screen draws, so
 * a refusal shows a sentence rather than a dead black rectangle. */
internal fun cameraAllowed(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
