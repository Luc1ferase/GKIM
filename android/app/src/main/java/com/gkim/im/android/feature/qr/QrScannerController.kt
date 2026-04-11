package com.gkim.im.android.feature.qr

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class QrScannerController(
    val hasCameraPermission: Boolean,
    val requestPermission: () -> Unit,
    val previewContent: @Composable (Modifier) -> Unit,
)

typealias QrScannerControllerFactory = @Composable ((String) -> Unit) -> QrScannerController

@Composable
fun rememberQrScannerController(
    onPayloadScanned: (String) -> Unit,
): QrScannerController {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnPayloadScanned by rememberUpdatedState(onPayloadScanned)
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build(),
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val emittedPayload = remember { AtomicBoolean(false) }

    DisposableEffect(scanner, analysisExecutor) {
        onDispose {
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    val previewContent: @Composable (Modifier) -> Unit = { modifier ->
        val previewView = remember(context) {
            PreviewView(context).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        }

        DisposableEffect(previewView, lifecycleOwner, scanner) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage == null || emittedPayload.get()) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(inputImage)
                                .addOnSuccessListener { barcodes ->
                                    val payload = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                    if (!payload.isNullOrBlank() && emittedPayload.compareAndSet(false, true)) {
                                        latestOnPayloadScanned(payload)
                                    }
                                }
                                .addOnFailureListener { emittedPayload.set(false) }
                                .addOnCompleteListener { imageProxy.close() }
                        }
                    }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
            }

            cameraProviderFuture.addListener(listener, mainExecutor)

            onDispose {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }

        AndroidView(
            factory = { previewView },
            modifier = modifier,
        )
    }

    return QrScannerController(
        hasCameraPermission = hasCameraPermission,
        requestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        previewContent = previewContent,
    )
}
