package com.pocketdev.advance_erp.feature.inventory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pocketdev.advance_erp.core.designsystem.ERPActionButton
import com.pocketdev.advance_erp.core.designsystem.ERPButtonVariant
import com.pocketdev.advance_erp.core.designsystem.PrimaryPurple
import com.pocketdev.advance_erp.core.designsystem.SecondaryTeal
import com.pocketdev.advance_erp.core.designsystem.StatusSuccess
import com.pocketdev.advance_erp.core.designsystem.TextMuted
import com.pocketdev.advance_erp.core.model.AppLanguage

@Composable
fun BarcodeQrScannerDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onCodeScanned: (code: String) -> Unit
) {
    var manualCodeInput by remember { mutableStateOf("") }
    var isFlashOn by remember { mutableStateOf(false) }
    var hasScanned by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

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

    // Infinite animation for scanning laser line
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
        if (!hasScanned) {
            hasScanned = true
            onCodeScanned(code.trim())
        }
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
                val scanner = remember { BarcodeScanning.getClient() }
                val previewView = remember {
                    PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                }
                DisposableEffect(lifecycleOwner) {
                    val providerFuture = ProcessCameraProvider.getInstance(context)
                    var provider: ProcessCameraProvider? = null
                    providerFuture.addListener({
                        try {
                            val p = providerFuture.get()
                            provider = p
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(analysisExecutor) { proxy ->
                                val media = proxy.image
                                if (media == null || hasScanned) {
                                    proxy.close()
                                } else {
                                    val img = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
                                    scanner.process(img)
                                        .addOnSuccessListener { codes ->
                                            val value = codes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                            if (value != null) {
                                                previewView.post { handleScan(value) }
                                            }
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                }
                            }
                            p.unbindAll()
                            camera = p.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(context))
                    onDispose {
                        camera = null
                        provider?.unbindAll()
                        scanner.close()
                        analysisExecutor.shutdown()
                    }
                }
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: Title & Close & Flash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .background(
                                if (isFlashOn) SecondaryTeal else Color.White.copy(alpha = 0.15f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }

                    Text(
                        text = if (isBangla) "বারকোড ও কিউআর স্ক্যানার" else "Barcode & QR Scanner",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cornerLength = 36f
                            val strokeW = 6f
                            val cornerColor = Color(0xFF299DB2)

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

                            // Laser Line
                            val laserY = h * laserFraction
                            drawLine(
                                color = Color(0xFFE91E63),
                                start = Offset(12f, laserY),
                                end = Offset(w - 12f, laserY),
                                strokeWidth = 3f,
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (permissionDenied) {
                            if (isBangla) "ক্যামেরার অনুমতি দিন (Settings থেকে) অথবা কোড লিখুন" else "Camera permission denied. Enable it in Settings or enter the code manually."
                        } else if (isBangla) "বারকোড বা কিউআর কোড ফ্রেমের মধ্যে রাখুন" else "Align barcode or QR code within the frame",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Bottom: Manual Code Entry & Quick Test triggers
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp)),
                    color = Color.White
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
