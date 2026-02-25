package ec.edu.uce.appproductosfinal.ui.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import ec.edu.uce.appproductosfinal.R
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

typealias LumaListener = (luma: Double) -> Unit
typealias BarcodeListener = (barcode: String) -> Unit

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    val imageCapture = remember { ImageCapture.Builder().build() }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    
    var luma by remember { mutableDoubleStateOf(0.0) }
    var lastScannedBarcode by remember { mutableStateOf("") }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isRecording by remember { mutableStateOf(false) }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(mainExecutor, MultiAnalyzer(
                    lumaListener = { l -> luma = l },
                    barcodeListener = { b -> 
                        if (lastScannedBarcode != b) {
                            lastScannedBarcode = b
                        }
                    }
                ))
            }
    }

    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (cameraPermissionState.status.isGranted) {
            CameraPreview(imageCapture, imageAnalysis, videoCapture, lensFacing)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.ArrowBack, "Volver", tint = Color.White)
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LightMode, null, tint = Color.Yellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Luma: ${"%.1f".format(luma)}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.Rounded.FlipCameraAndroid, "Cambiar Cámara", tint = Color.White)
                }
            }

            AnimatedVisibility(
                visible = lastScannedBarcode.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                BarcodeResultCard(
                    barcode = lastScannedBarcode,
                    onClose = { lastScannedBarcode = "" },
                    context = context
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No se pudo abrir la galería", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White)
                    }

                    CaptureButton(
                        isRecording = isRecording,
                        onPress = {
                            takePhoto(context, imageCapture, mainExecutor)
                        },
                        onLongPress = {
                            if (!isRecording) {
                                if (audioPermissionState.status.isGranted) {
                                    isRecording = true
                                    currentRecording = captureVideo(context, videoCapture, mainExecutor) { 
                                        isRecording = false
                                        currentRecording = null
                                    }
                                } else {
                                    audioPermissionState.launchPermissionRequest()
                                }
                            }
                        },
                        onRelease = {
                            if (isRecording) {
                                currentRecording?.stop()
                                currentRecording = null
                                isRecording = false
                            }
                        }
                    )

                    IconButton(onClick = { }) {
                        Icon(
                            if (isRecording) Icons.Rounded.StopCircle else Icons.Rounded.CameraAlt,
                            null,
                            tint = if (isRecording) Color.Red else Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                if (!isRecording) {
                    Text(
                        "Toca para Foto • Mantén para Video",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(top = 80.dp)
                    )
                }
            }

        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Camera, null, modifier = Modifier.size(80.dp), tint = Color.Gray)
                Spacer(Modifier.height(24.dp))
                Text("Acceso a la Cámara", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text(
                    "Para capturar productos y escanear códigos, necesitamos tu permiso.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Conceder Acceso")
                }
            }
        }
    }
}

@Composable
fun CaptureButton(
    isRecording: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit
) {
    val scale by animateFloatAsState(if (isRecording) {
        1.2f
    } else {
        1f
    }, label = "button_scale")
    val color = if (isRecording) Color.Red else Color.White
    
    Box(
        modifier = Modifier
            .size(84.dp)
            .scale(scale)
            .border(width = 4.dp, color = Color.White, shape = CircleShape)
            .padding(8.dp)
            .clip(CircleShape)
            .background(color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onPress() },
                    onLongPress = { onLongPress() },
                    onPress = {
                        tryAwaitRelease()
                        onRelease()
                    }
                )
            }
    ) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun BarcodeResultCard(barcode: String, onClose: () -> Unit, context: Context) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                if (barcode.startsWith("http")) Icons.Rounded.Language else Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Contenido Detectado",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                barcode,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cerrar")
                }
                
                Button(
                    onClick = {
                        if (barcode.startsWith("http")) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(barcode))
                            context.startActivity(intent)
                        } else {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("QR", barcode))
                            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (barcode.startsWith("http")) "Abrir" else "Copiar")
                }
            }
        }
    }
}

@Composable
fun CameraPreview(
    imageCapture: ImageCapture,
    imageAnalysis: ImageAnalysis,
    videoCapture: VideoCapture<Recorder>,
    lensFacing: Int
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx -> PreviewView(ctx) },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageCapture, imageAnalysis, videoCapture
                    )
                } catch (ex: Exception) {
                    Log.e("CameraScreen", "Error", ex)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, executor: Executor) {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
        }
    }
    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
    ).build()

    imageCapture.takePicture(outputOptions, executor, object : ImageCapture.OnImageSavedCallback {
        override fun onError(exc: ImageCaptureException) {
            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
        }
        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
            Toast.makeText(context, "¡Foto capturada!", Toast.LENGTH_SHORT).show()
        }
    })
}

private fun captureVideo(
    context: Context,
    videoCapture: VideoCapture<Recorder>,
    executor: Executor,
    onVideoFinished: () -> Unit
): Recording {
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
        }
    }
    val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
        context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ).setContentValues(contentValues).build()

    return videoCapture.output
        .prepareRecording(context, mediaStoreOutputOptions)
        .apply {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                withAudioEnabled()
            }
        }
        .start(executor) { recordEvent ->
            if (recordEvent is VideoRecordEvent.Finalize) {
                onVideoFinished()
                if (!recordEvent.hasError()) {
                    Toast.makeText(context, "Video guardado", Toast.LENGTH_SHORT).show()
                }
            }
        }
}

private class MultiAnalyzer(
    private val lumaListener: LumaListener,
    private val barcodeListener: BarcodeListener
) : ImageAnalysis.Analyzer {
    private val scanner: BarcodeScanner = BarcodeScanning.getClient()
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()
        val data = ByteArray(remaining())
        get(data)
        return data
    }

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val buffer = imageProxy.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        lumaListener(pixels.average())

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { barcodeListener(it) }
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }
}
