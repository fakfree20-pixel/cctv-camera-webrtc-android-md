package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.data.model.CameraLens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

class CameraManager(private val context: Context) {
    private val TAG = "CameraManager"

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    private var previewUseCase: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var imageCapture: ImageCapture? = null

    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    var currentLens: CameraLens = CameraLens.BACK
        private set
    var isTorchOn: Boolean = false
        private set

    // Motion Detection
    var motionDetectionEnabled: Boolean = true
    private var previousFrameLuma: ByteArray? = null
    private var lastMotionTriggerTime = 0L
    private val motionCooldownMs = 3000L // 3 seconds between motion alert events

    // JPEG Frame Broadcast
    private val frameListeners = CopyOnWriteArrayList<(ByteArray) -> Unit>()
    var onMotionDetected: ((motionPercentage: Float) -> Unit)? = null

    private val isProcessingFrame = AtomicBoolean(false)
    private val lastFrameTime = AtomicLong(0L)
    private val minFrameIntervalMs = 40L // ~25 FPS max

    @Volatile
    var latestJpegFrame: ByteArray? = null
        private set

    fun addFrameListener(listener: (ByteArray) -> Unit) {
        frameListeners.add(listener)
    }

    fun removeFrameListener(listener: (ByteArray) -> Unit) {
        frameListeners.remove(listener)
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null,
        onReady: (() -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases(lifecycleOwner, previewView)
                onReady?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun bindCameraUseCases(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView? = null
    ) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = if (currentLens == CameraLens.BACK) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }

        // 1. Preview
        previewUseCase = Preview.Builder()
            .build()

        previewView?.let {
            previewUseCase?.surfaceProvider = it.surfaceProvider
        }

        // 2. ImageAnalysis for MJPEG streaming & motion detection
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(Size(640, 480))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }

        // 3. ImageCapture for high quality snapshot
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()

        try {
            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageAnalysis,
                imageCapture
            )
            cameraControl = camera?.cameraControl
            cameraInfo = camera?.cameraInfo

            // Restore torch state if back camera
            if (currentLens == CameraLens.BACK && isTorchOn) {
                cameraControl?.enableTorch(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastFrameTime.get() < minFrameIntervalMs) {
            imageProxy.close()
            return
        }

        if (!isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        try {
            lastFrameTime.set(now)
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees

            // Convert YUV image to JPEG byte array
            val jpegBytes = imageProxyToJpeg(imageProxy, rotationDegrees)
            if (jpegBytes != null) {
                latestJpegFrame = jpegBytes
                for (listener in frameListeners) {
                    try {
                        listener(jpegBytes)
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame listener error", e)
                    }
                }
            }

            // Motion detection analysis on Y-plane (luminance)
            if (motionDetectionEnabled) {
                detectMotion(imageProxy)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing camera frame", e)
        } finally {
            isProcessingFrame.set(false)
            imageProxy.close()
        }
    }

    private fun detectMotion(imageProxy: ImageProxy) {
        val yBuffer = imageProxy.planes[0].buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = imageProxy.planes[0].rowStride
        val pixelStride = imageProxy.planes[0].pixelStride

        // Downsample grid (32x24) for super-fast lightweight motion detection
        val sampleW = 32
        val sampleH = 24
        val currentLuma = ByteArray(sampleW * sampleH)

        val stepX = (width / sampleW).coerceAtLeast(1)
        val stepY = (height / sampleH).coerceAtLeast(1)

        var idx = 0
        for (y in 0 until sampleH) {
            val sourceY = (y * stepY).coerceAtMost(height - 1)
            for (x in 0 until sampleW) {
                val sourceX = (x * stepX).coerceAtMost(width - 1)
                val bufferPos = sourceY * rowStride + sourceX * pixelStride
                if (bufferPos < yBuffer.capacity()) {
                    currentLuma[idx++] = yBuffer.get(bufferPos)
                }
            }
        }

        val prev = previousFrameLuma
        if (prev != null && prev.size == currentLuma.size) {
            var diffPixels = 0
            val threshold = 28 // sensitivity threshold

            for (i in currentLuma.indices) {
                val diff = abs((currentLuma[i].toInt() and 0xFF) - (prev[i].toInt() and 0xFF))
                if (diff > threshold) {
                    diffPixels++
                }
            }

            val motionPct = (diffPixels.toFloat() / currentLuma.size.toFloat()) * 100f
            // Trigger if more than 12% pixels changed
            if (motionPct > 12f) {
                val now = System.currentTimeMillis()
                if (now - lastMotionTriggerTime > motionCooldownMs) {
                    lastMotionTriggerTime = now
                    onMotionDetected?.invoke(motionPct)
                }
            }
        }

        previousFrameLuma = currentLuma
    }

    private fun imageProxyToJpeg(imageProxy: ImageProxy, rotationDegrees: Int): ByteArray? {
        val yBuffer = imageProxy.planes[0].buffer
        val uBuffer = imageProxy.planes[1].buffer
        val vBuffer = imageProxy.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        val pixelStride = imageProxy.planes[2].pixelStride
        val rowStride = imageProxy.planes[2].rowStride

        // Convert U & V planes to NV21 format
        var pos = ySize
        val uvHeight = imageProxy.height / 2
        val uvWidth = imageProxy.width / 2

        for (row in 0 until uvHeight) {
            for (col in 0 until uvWidth) {
                val vPos = row * rowStride + col * pixelStride
                val uPos = row * rowStride + col * pixelStride
                if (vPos < vSize && uPos < uSize) {
                    nv21[pos++] = vBuffer.get(vPos)
                    nv21[pos++] = uBuffer.get(uPos)
                }
            }
        }

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 75, out)
        val unrotatedJpeg = out.toByteArray()

        if (rotationDegrees == 0) {
            return unrotatedJpeg
        }

        // Rotate bitmap if required
        val bitmap = BitmapFactory.decodeByteArray(unrotatedJpeg, 0, unrotatedJpeg.size) ?: return unrotatedJpeg
        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        if (currentLens == CameraLens.FRONT) {
            matrix.postScale(-1f, 1f) // Mirror front camera
        }
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val rotatedOut = ByteArrayOutputStream()
        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 75, rotatedOut)
        bitmap.recycle()
        rotatedBitmap.recycle()
        return rotatedOut.toByteArray()
    }

    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView? = null) {
        currentLens = if (currentLens == CameraLens.BACK) CameraLens.FRONT else CameraLens.BACK
        isTorchOn = false // Reset torch on switch
        bindCameraUseCases(lifecycleOwner, previewView)
    }

    fun toggleTorch(): Boolean {
        if (currentLens == CameraLens.BACK && cameraInfo?.hasFlashUnit() == true) {
            isTorchOn = !isTorchOn
            cameraControl?.enableTorch(isTorchOn)
            return isTorchOn
        }
        return false
    }

    fun setTorch(enabled: Boolean) {
        if (currentLens == CameraLens.BACK && cameraInfo?.hasFlashUnit() == true) {
            isTorchOn = enabled
            cameraControl?.enableTorch(enabled)
        }
    }

    suspend fun takeSnapshot(outputFile: File): Boolean = withContext(Dispatchers.IO) {
        val capture = imageCapture
        if (capture == null) {
            // Fallback: write latest frame
            latestJpegFrame?.let {
                outputFile.writeBytes(it)
                return@withContext true
            }
            return@withContext false
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        return@withContext kotlin.coroutines.suspendCoroutine { cont ->
            capture.takePicture(
                outputOptions,
                cameraExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        cont.resumeWith(Result.success(true))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Snapshot capture failed", exception)
                        latestJpegFrame?.let {
                            outputFile.writeBytes(it)
                            cont.resumeWith(Result.success(true))
                        } ?: cont.resumeWith(Result.success(false))
                    }
                }
            )
        }
    }

    fun release() {
        try {
            cameraExecutor.shutdown()
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing CameraManager", e)
        }
    }
}
