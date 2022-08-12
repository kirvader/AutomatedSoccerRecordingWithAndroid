package app.hawkeye.balltracker.rotating

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.utils.createLogger
import app.hawkeye.balltracker.utils.pod.ScanResultsAdapter
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdk.events.PivoEvent
import app.pivo.android.basicsdk.events.PivoEventBus
import com.hawkeye.movement.interfaces.RotatableDevice
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions

import kotlin.math.abs


private val LOG = createLogger<PivoPodDevice>()

class PivoPodDevice(context: Context) : RotatableDevice {
    private var availableSpeeds: List<Int>

    private fun getLicenseContent(context: Context): String = context.assets.open("licenceKey.json").bufferedReader().use { it.readText() }

    init {
        PivoSdk.init(context)
        sdk = PivoSdk.getInstance()
        sdk.unlockWithLicenseKey(getLicenseContent(context))

        availableSpeeds = sdk.supportedSpeeds.filter { it in 20..200 }
    }

    override fun rotateBy(speed: Float, orientedAngle: Float) {
        if (orientedAngle > 0) {
            sdk.turnRight(abs(orientedAngle.toInt()))
        } else if (orientedAngle < 0) {
            sdk.turnLeft(abs(orientedAngle.toInt()))
        } else {
            sdk.stop()
        }
    }

    override fun stop() {
        sdk.stop()
    }

    override fun getTheMostAppropriateSpeedFromAvailable(speed: Float): Float {
        return availableSpeeds[availableSpeeds.binarySearch((360.0f / speed).toInt())].toFloat()
    }

    override fun getGradPerSecSpeedFromAvailable(availableDeviceSpeed: Float): Float {
        return 1.0f / 360.0f / availableDeviceSpeed
    }

    companion object {
        private lateinit var sdk: PivoSdk

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
                        sdk.connectTo(scanResult)
                    }
                }
            })

            PivoEventBus.subscribe(
                PivoEventBus.CONNECTION_COMPLETED, context
            ) {
                if (it is PivoEvent.ConnectionComplete) {
                    LOG.e("CONNECTION_COMPLETED")
                }
            }
            //subscribe to get scan device
            PivoEventBus.subscribe(
                PivoEventBus.SCAN_DEVICE, context
            ) {
                if (it is PivoEvent.Scanning) {
                    LOG.e("Result for scanning is updated")
                    pivoScanResultsAdapter.addScanResult(it.device)
                }
            }

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
                        sdk.scan()
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
}
