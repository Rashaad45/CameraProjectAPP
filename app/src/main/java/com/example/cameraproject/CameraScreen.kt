package com.example.cameraproject

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import androidx.camera.core.Preview as CameraPreview

@Composable
fun CameraPermissionScreen() {
    val context = LocalContext.current
    // For newer Android versions, WRITE_EXTERNAL_STORAGE is not needed for MediaStore
    // if you're saving to your app's own directory or using MediaStore directly.
    // However, if you target older APIs or need broader access, it might be relevant.
    // For saving to MediaStore.Images.Media.EXTERNAL_CONTENT_URI, only CAMERA permission is usually sufficient
    // for Android 10 (API 29) and above due to Scoped Storage.
    val permissions = listOf(Manifest.permission.CAMERA)

    // Check if permission is already granted
    val isPermissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        // Check if all requested permissions are granted
        isPermissionGranted.value = perms.all { it.value }
        if (!isPermissionGranted.value) {
            Toast.makeText(context, "Camera permission not granted", Toast.LENGTH_SHORT).show()
        }
    }

    if (isPermissionGranted.value) {
        CameraScreen()
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Camera permission is required to use this feature.")
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                launcher.launch(permissions.toTypedArray())
            }) {
                Text(text = "Request Camera Permission")
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // State to hold the current camera selector (e.g., front or back)
    val cameraSelector = remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Setup camera
    LaunchedEffect(cameraSelector.value) { // Re-launch effect if cameraSelector changes
        try {
            val cameraProvider = context.getCameraProvider()
            val preview = CameraPreview.Builder().build()

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to the camera
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector.value, // Use the current camera selector state
                preview,
                imageCapture
            )

            // Connect the preview to the PreviewView
            preview.setSurfaceProvider(previewView.surfaceProvider)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Camera initialization failed", e)
            Toast.makeText(context, "Camera initialization failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Camera Preview
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {


            // Capture Photo Button
            IconButton(
                onClick = {
                    capturePhoto(context, imageCapture)
                },
                modifier = Modifier
                    .size(70.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.Red, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_camera),
                        contentDescription = "Capture Photo",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Camera switch button
            IconButton(
                onClick = {
                    cameraSelector.value = if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.DarkGray, CircleShape)
            ) {
                Icon(
                    painter = painterResource(id = android.R.drawable.ic_menu_rotate),
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

// Consolidated and corrected capturePhoto function
private fun capturePhoto(context: Context, imageCapture: ImageCapture) {
    val name = "Camera-Project_${System.currentTimeMillis()}.jpg"

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        // Use MediaStore.Images.Media.RELATIVE_PATH for Android 10 (API 29) and above
        // This puts the image in a "CameraProject-Images" subfolder within the Pictures directory.
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraProject-Images")
    }

    val outputOption = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOption,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri: Uri? = output.savedUri
                if (savedUri != null) {
                    Log.d("CameraCapture", "Photo saved: $savedUri")
                    Toast.makeText(context, "Photo saved successfully to: $savedUri", Toast.LENGTH_LONG).show()
                } else {
                    Log.e("CameraCapture", "Photo saved, but URI is null.")
                    Toast.makeText(context, "Photo saved, but could not retrieve URI.", Toast.LENGTH_LONG).show()
                }
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("CameraCapture", "Photo capture failed: ${exception.message}", exception)
                Toast.makeText(context, "Photo capture failed: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
    suspendCancellableCoroutine { continuation ->
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                continuation.resume(cameraProviderFuture.get())
            } catch (e: Exception) {
                Log.e("CameraProvider", "Failed to get camera provider", e)
                continuation.cancel(e) // Propagate the exception
            }
        }, ContextCompat.getMainExecutor(this))
    }

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CameraScreenPreview() {
    // Note: Preview won't show actual camera, just the UI layout
    CameraPermissionScreen()
}