package com.pocketdev.advance_erp.feature.inventory

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Size
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.model.AppLanguage
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SuppressLint("ClickableViewAccessibility")
@Composable
fun BarcodeQrScannerDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCodeScanned: (code: String) -> Unit
) {
    var manualCodeInput by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var isScanningPaused by remember { mutableStateOf(false) }
    var isSuccessHighlight by remember { mutableStateOf(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var tapFocusPoint by remember { mutableStateOf<Offset?>(null) }

    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val isBangla = language == AppLanguage.BANGLA

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDenied by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        permissionDenied = !granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(isFlashOn, camera) {
        camera?.takeIf { it.cameraInfo.hasFlashUnit() }?.cameraControl?.enableTorch(isFlashOn)
    }

    LaunchedEffect(zoomLevel, camera) {
        camera?.cameraControl?.setZoomRatio(zoomLevel)
    }

    // Auto-resume scanner after 2.5 seconds if dialog is still active
    LaunchedEffect(isScanningPaused) {
        if (isScanningPaused) {
            delay(2500)
            isSuccessHighlight = false
            isScanningPaused = false
        }
    }

    // Clear tap focus indicator after 1 second
    LaunchedEffect(tapFocusPoint) {
        if (tapFocusPoint != null) {
            delay(1000)
            tapFocusPoint = null
        }
    }

    // Laser line animation
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserFraction by infiniteTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserFraction"
    )

    fun handleScan(code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty() || isScanningPaused) return
        isScanningPaused = true
        lastScannedCode = trimmed
        isSuccessHighlight = true
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Exception) {}
        onCodeScanned(trimmed)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (hasCameraPermission) Color.Black else Color.Black.copy(alpha = 0.92f))
        ) {
            if (hasCameraPermission) {
                val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
                val barcodeOptions = remember {
                    BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_ALL_FORMATS
                        )
                        .build()
                }
                val scanner = remember(barcodeOptions) { BarcodeScanning.getClient(barcodeOptions) }
                val previewView = remember {
                    PreviewView(context).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                }

                DisposableEffect(lifecycleOwner) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    var provider: ProcessCameraProvider? = null

                    providerFuture.addListener({
                        try {
                            val p = providerFuture.get()
                            provider = p

                            // Set high resolution for crisp barcode scanning
                            val targetResolution = Size(1280, 720)

                            val preview = Preview.Builder()
                                .setTargetResolution(targetResolution)
                                .build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            val analysis = ImageAnalysis.Builder()
                                .setTargetResolution(targetResolution)
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            analysis.setAnalyzer(analysisExecutor) { proxy ->
                                val media = proxy.image
                                if (media == null || isScanningPaused) {
                                    proxy.close()
                                } else {
                                    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                    scanner.process(img)
                                        .addOnSuccessListener { codes ->
                                            for (barcode in codes) {
                                                val raw = barcode.rawValue?.trim()?.ifEmpty { null }
                                                    ?: barcode.displayValue?.trim()?.ifEmpty { null }
                                                if (!raw.isNullOrBlank()) {
                                                    previewView.post { handleScan(raw) }
                                                    break
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                }
                            }

                            p.unbindAll()
                            val boundCamera = p.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                            camera = boundCamera

                            // Initial center auto-focus
                            previewView.post {
                                if (previewView.width > 0 && previewView.height > 0) {
                                    val factory = previewView.meteringPointFactory
                                    val centerPoint = factory.createPoint(
                                        previewView.width / 2f,
                                        previewView.height / 2f
                                    )
                                    val focusAction = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF)
                                        .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                        .build()
                                    boundCamera.cameraControl.startFocusAndMetering(focusAction)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))

                    // Setup tap-to-focus on previewView
                    previewView.setOnTouchListener { v, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val cam = camera
                            if (cam != null) {
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x, event.y)
                                val action = FocusMeteringAction.Builder(
                                    point,
                                    FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
                                )
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                cam.cameraControl.startFocusAndMetering(action)
                                tapFocusPoint = Offset(event.x, event.y)
                            }
                            v.performClick()
                            true
                        } else {
                            true
                        }
                    }

                    onDispose {
                        camera = null
                        provider?.unbindAll()
                        scanner.close()
                        analysisExecutor.shutdown()
                    }
                }

                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }

            // Tap Focus Indicator Overlay
            tapFocusPoint?.let { pos ->
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SecondaryTeal,
                        radius = 36f,
                        center = pos,
                        style = Stroke(width = 3f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Flashlight, Zoom & Close
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Flashlight Button
                        IconButton(
                            onClick = { isFlashOn = !isFlashOn },
                            modifier = Modifier
                                .background(
                                    if (isFlashOn) SecondaryTeal else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .size(42.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = "Flashlight",
                                tint = if (isFlashOn) Color.Black else Color.White
                            )
                        }

                        // Zoom Toggle Button (1x / 2x / 3x)
                        IconButton(
                            onClick = {
                                zoomLevel = when (zoomLevel) {
                                    1.0f -> 2.0f
                                    2.0f -> 3.0f
                                    else -> 1.0f
                                }
                            },
                            modifier = Modifier
                                .background(
                                    if (zoomLevel > 1.0f) PrimaryPurple else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                                .size(42.dp)
                        ) {
                            Text(
                                text = "${zoomLevel.toInt()}x",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        text = if (isBangla) "বারকোড ও কিউআর স্ক্যানার" else "Barcode & QR Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Middle: Viewfinder Reticle with Laser Beam
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cornerLength = 40f
                            val strokeW = 7f
                            val cornerColor = if (isSuccessHighlight) StatusSuccess else Color(0xFF299DB2)

                            // Top-Left Corner
                            drawLine(cornerColor, Offset(0f, 0f), Offset(cornerLength, 0f), strokeW, StrokeCap.Round)
                            drawLine(cornerColor, Offset(0f, 0f), Offset(0f, cornerLength), strokeW, StrokeCap.Round)

                            // Top-Right Corner
                            drawLine(cornerColor, Offset(w, 0f), Offset(w - cornerLength, 0f), strokeW, StrokeCap.Round)
                            drawLine(cornerColor, Offset(w, 0f), Offset(w, cornerLength), strokeW, StrokeCap.Round)

                            // Bottom-Left Corner
                            drawLine(cornerColor, Offset(0f, h), Offset(cornerLength, h), strokeW, StrokeCap.Round)
                            drawLine(cornerColor, Offset(0f, h), Offset(0f, h - cornerLength), strokeW, StrokeCap.Round)

                            // Bottom-Right Corner
                            drawLine(cornerColor, Offset(w, h), Offset(w - cornerLength, h), strokeW, StrokeCap.Round)
                            drawLine(cornerColor, Offset(w, h), Offset(w, h - cornerLength), strokeW, StrokeCap.Round)

                            // Animated Laser Line
                            val laserY = h * laserFraction
                            drawLine(
                                color = if (isSuccessHighlight) StatusSuccess else Color(0xFFE91E63),
                                start = Offset(14f, laserY),
                                end = Offset(w - 14f, laserY),
                                strokeWidth = if (isSuccessHighlight) 5f else 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (permissionDenied) {
                            if (isBangla) "ক্যামেরার অনুমতি দিন (Settings থেকে) অথবা কোড লিখুন"
                            else "Camera permission denied. Enable it in Settings or enter code manually."
                        } else if (isSuccessHighlight && lastScannedCode != null) {
                            if (isBangla) "স্ক্যান সফল: $lastScannedCode" else "Scanned: $lastScannedCode"
                        } else {
                            if (isBangla) "বারকোড বা কিউআর কোড ফ্রেমের মধ্যে রাখুন (ফোকাস করতে ট্যাপ করুন)"
                            else "Align code within frame (Tap to focus)"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSuccessHighlight) StatusSuccess else Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )

                    if (isScanningPaused && lastScannedCode != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                isScanningPaused = false
                                isSuccessHighlight = false
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "পুনরায় স্ক্যান করতে ট্যাপ করুন" else "Tap to scan again",
                                color = SecondaryTeal,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom: Manual Code Entry & Quick Test triggers
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isBangla) "অথবা কোড সরাসরি লিখুন" else "Or Enter Code Manually",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryPurple
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualCodeInput,
                                onValueChange = { manualCodeInput = it },
                                placeholder = {
                                    Text(
                                        text = if (isBangla) "বারকোড বা SKU লিখুন..." else "Enter Barcode or SKU...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextMuted
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        focusManager.clearFocus()
                                        if (manualCodeInput.isNotBlank()) {
                                            handleScan(manualCodeInput)
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = Color(0xFFE2E8F0)
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            ERPActionButton(
                                text = if (isBangla) "খুঁজুন" else "Lookup",
                                onClick = {
                                    focusManager.clearFocus()
                                    if (manualCodeInput.isNotBlank()) {
                                        handleScan(manualCodeInput)
                                    }
                                },
                                variant = ERPButtonVariant.PRIMARY,
                                modifier = Modifier.height(52.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Test shortcuts for testing
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = { handleScan("6901234567890") }
                            ) {
                                Text("Test Barcode", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                            }
                            TextButton(
                                onClick = { handleScan("TWS-PRO5") }
                            ) {
                                Text("Test SKU", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                            }
                            TextButton(
                                onClick = { handleScan("ADV-ERP:PRD:prod-earbuds-01") }
                            ) {
                                Text("Test QR", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                            }
                        }
                    }
                }
            }
        }
    }
}
