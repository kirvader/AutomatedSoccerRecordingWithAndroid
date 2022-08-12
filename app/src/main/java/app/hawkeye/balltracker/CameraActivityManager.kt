package app.hawkeye.balltracker

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.provider.MediaStore
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import androidx.test.internal.runner.junit4.statement.UiThreadStatement.runOnUiThread
import app.hawkeye.balltracker.controllers.FootballTrackingSystemController
import app.hawkeye.balltracker.utils.AdaptiveRect
import app.hawkeye.balltracker.utils.ClassifiedBox
import app.hawkeye.balltracker.utils.TimeKeeper
import app.hawkeye.balltracker.utils.createLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private val LOG = createLogger<CameraManager>()

class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val updateUIOnStopRecording: () -> Unit,
    private val updateUIOnStartRecording: () -> Unit,
    private val updateUIOnImageAnalyzerFinished: (AdaptiveRect?, String) -> Unit,
    private val getPreviewSurfaceProvider: () -> Preview.SurfaceProvider,

) {
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var preview: Preview? = null

    private var imageAnalysis: ImageAnalysis? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var selector: QualitySelector? = null
    private var mediaStoreOutputOptions: MediaStoreOutputOptions? = null

    private var recorder: Recorder? = null

    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val backgroundExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private val scope = CoroutineScope(Job() + Dispatchers.Default)

    private var objectDetectorImageAnalyzer: ObjectDetectorImageAnalyzer? = null

    private var timeKeeper = TimeKeeper()
    private var movementControllerDevice: FootballTrackingSystemController =
        FootballTrackingSystemController(
            App.getRotatableDevice()
        )


    private val recordingListener = Consumer<VideoRecordEvent> { event ->
        when (event) {
            is VideoRecordEvent.Start -> {
                val msg = "Capture Started"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                LOG.i(msg)
            }

            is VideoRecordEvent.Finalize -> {
                val msg = if (!event.hasError()) {
                    "Video capture succeeded: ${event.outputResults.outputUri}"
                } else {
                    recording?.close()
                    recording = null
                    "Video capture ends with error: ${event.error}"
                }

                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                LOG.i(msg)
            }
        }
    }

    init {
        objectDetectorImageAnalyzer = ObjectDetectorImageAnalyzer(context, ::updateTrackingSystemState)
    }


    fun setImageProcessor(imageProcessorsChoice: ImageProcessorsChoice) {
        objectDetectorImageAnalyzer?.setCurrentImageProcessor(imageProcessorsChoice)
    }

    private fun setAnalyzer() {
        scope.launch {
            imageAnalysis?.clearAnalyzer()
            try {

                objectDetectorImageAnalyzer?.let {
                    imageAnalysis?.setAnalyzer(backgroundExecutor, it)
                }
            } catch (e: Exception) {
                LOG.d("Analyzer setup failed. Using model best2.pt", e)
            }
            if (imageAnalysis != null)
                LOG.i("Analyzer has been successfully set up.")
            else
                LOG.e("Analyzer is null.")
        }
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        cameraProviderFuture.addListener({
            try {
                selector = QualitySelector
                    .from(
                        Quality.UHD,
                        FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                    )

                recorder = Recorder.Builder()
                    .setExecutor(cameraExecutor).setQualitySelector(selector!!)
                    .build()

                videoCapture = VideoCapture.withOutput(recorder!!)

                preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()

                imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                setAnalyzer()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, videoCapture, imageAnalysis
                )

                preview?.setSurfaceProvider(getPreviewSurfaceProvider())
            } catch (ex: Exception) {
                if (ex.message != null) {
                    LOG.e(ex.toString())
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleCameraRecording() {
        if (recording != null) {
            stopCapturing()
            updateUIOnStopRecording()
        } else {
            startCapture()
            updateUIOnStartRecording()
        }
    }

    private fun startCapture() {
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }

        mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            LOG.i("Not all permissions granted.")
            return
        }
        recording = videoCapture?.output
            ?.prepareRecording(context, mediaStoreOutputOptions!!)
            ?.withAudioEnabled()
            ?.start(ContextCompat.getMainExecutor(context), recordingListener)!!
    }

    private fun stopCapturing() {
        recording?.stop()
        recording = null
    }

    private fun updateTrackingSystemState(result: List<ClassifiedBox>) {
        if (result.isEmpty()) {
            LOG.i("No appropriate objects found")
            movementControllerDevice.updateBallModelWithClassifiedBox(
                null,
                timeKeeper.getCurrentCircleStartTime()
            )
            return
        }
        LOG.d("Found objects. The best is at x = %f", result[0].adaptiveRect.center.x)
        movementControllerDevice.updateBallModelWithClassifiedBox(
            result[0],
            timeKeeper.getCurrentCircleStartTime()
        )
        timeKeeper.registerCircle()
        updateUIOnImageAnalyzerFinished(result.firstOrNull()?.adaptiveRect, timeKeeper.getInfoAboutLastCircle())
    }

    fun destroy() {
        backgroundExecutor.shutdown()
        ProcessCameraProvider.getInstance(context).get().unbindAll()
        LOG.i("OnDestroy called")
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}