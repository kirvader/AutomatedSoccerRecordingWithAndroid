package app.pivo.android.basicsdkdemo.devices.rotating

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdk.events.PivoEvent
import app.pivo.android.basicsdk.events.PivoEventBus
import app.pivo.android.basicsdkdemo.R
import app.pivo.android.basicsdkdemo.utils.ScanResultsAdapter
import app.pivo.android.basicsdkdemo.utils.createLogger
import com.example.movementcontrollingmodule.movementController.RotatingDevice
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import io.reactivex.functions.Consumer
import kotlin.math.abs

private val LOG = createLogger<PivoPodRotatingImpl>()

class PivoPodRotatingImpl(private val context: Context) : RotatingDevice {
    private lateinit var availableSpeeds: List<Int>

    private fun getLicenseContent(context: Context): String = context.assets.open("licenceKey.json").bufferedReader().use { it.readText() }

    override fun init() {
        super.init()
        PivoSdk.init(context)
        PivoSdk.getInstance().unlockWithLicenseKey(getLicenseContent(context))

        availableSpeeds = PivoSdk.getInstance().supportedSpeeds.filter { it in 20..200 }
    }

    override fun rotateBy(speed: Float, orientedAngle: Float) {
        super.rotateBy(speed, orientedAngle)
        if (orientedAngle > 0) {
            PivoSdk.getInstance().turnRight(abs(orientedAngle.toInt()))
        } else if (orientedAngle < 0) {
            PivoSdk.getInstance().turnLeft(abs(orientedAngle.toInt()))
        } else {
            PivoSdk.getInstance().stop()
        }
    }

    override fun stop() {
        super.stop()
        PivoSdk.getInstance().stop()
    }

    override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float {
        return availableSpeeds[availableSpeeds.binarySearch((360.0f / speed).toInt())].toFloat()
    }

    override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float {
        return 1.0f / 360.0f / availableDeviceSpeed
    }

    fun scanForPivoDevices(context: Context, layoutInflater: LayoutInflater) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Pivo scan results")
        val dialogLayout = layoutInflater.inflate(R.layout.pivo_scan_results, null)
        val scanResults = dialogLayout.findViewById<RecyclerView>(R.id.scan_results)

        //initialize device scan adapter
        val pivoScanResultsAdapter = ScanResultsAdapter()
        pivoScanResultsAdapter.setOnAdapterItemClickListener(object :
            ScanResultsAdapter.OnAdapterItemClickListener {
            override fun onAdapterViewClick(view: View?) {
                val scanResult = pivoScanResultsAdapter.getItemAtPosition(
                    scanResults.getChildAdapterPosition(view!!)
                )
                if (scanResult != null) {
                    PivoSdk.getInstance().connectTo(scanResult)
                }
            }
        })

        PivoEventBus.subscribe(
            PivoEventBus.CONNECTION_COMPLETED, context, Consumer {
                if (it is PivoEvent.ConnectionComplete) {
                    LOG.e("CONNECTION_COMPLETED")
                }
            })
        //subscribe to get scan device
        PivoEventBus.subscribe(
            PivoEventBus.SCAN_DEVICE, context, Consumer {
                if (it is PivoEvent.Scanning) {
                    LOG.e("Result for scanning is updated")
                    pivoScanResultsAdapter.addScanResult(it.device)
                }
            })

        scanResults.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
            adapter = pivoScanResultsAdapter
        }

        checkPermission(context)

        builder.setView(dialogLayout)
        builder.show()
    }

    //check permissions if they're granted start scanning, otherwise ask to user to grant permissions
    private fun checkPermission(context: Context) {// alternative Permission library Dexter
        Permissions.check(context,
            permissionList, null, null,
            object : PermissionHandler() {
                override fun onGranted() {
                    PivoSdk.getInstance().scan()
                }
            })
    }

    //permissions which are required for bluetooth
    private var permissionList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}
