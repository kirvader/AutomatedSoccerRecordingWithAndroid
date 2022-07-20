package app.pivo.android.basicsdkdemo

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import app.pivo.android.basicsdk.PivoSdk
import app.pivo.android.basicsdk.events.PivoEvent
import app.pivo.android.basicsdk.events.PivoEventBus
import com.nabinbhandari.android.permissions.PermissionHandler
import com.nabinbhandari.android.permissions.Permissions
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.activity_pivo_scanning.*

class PivoScanningActivity : AppCompatActivity() {

    private val TAG = "MainActivity"
    private lateinit var resultAdapter: ScanResultsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pivo_scanning)


        //initialize device scan adapter
        resultAdapter = ScanResultsAdapter()
        resultAdapter.setOnAdapterItemClickListener(object :
            ScanResultsAdapter.OnAdapterItemClickListener {
            override fun onAdapterViewClick(view: View?) {
                val scanResult = resultAdapter.getItemAtPosition(scan_results.getChildAdapterPosition(view!!))
                if (scanResult!=null){
                    PivoSdk.getInstance().connectTo(scanResult)
                }
            }
        })

        //prepare scan result listview
        scan_results.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@PivoScanningActivity)
            adapter = resultAdapter
        }
        //start scanning button
        scan_button.setOnClickListener {
            checkPermission()
        }
        //cancel scanning button
        cancel_button.setOnClickListener {
            scanning_bar.visibility = View.INVISIBLE
            PivoSdk.getInstance().stopScan()
            resultAdapter.clearScanResults()
        }
    }


    override fun onResume() {
        super.onResume()
        //subscibe pivo connection events
        PivoEventBus.subscribe(
            PivoEventBus.CONNECTION_COMPLETED, this, Consumer {
            scanning_bar.visibility = View.INVISIBLE
            if (it is PivoEvent.ConnectionComplete){
                Log.e(TAG, "CONNECTION_COMPLETED")
//                appendToLog("Connection to PIVO POD completed successfully. Going to the next step.")
                openController()
            }
        })
        //subscribe to get scan device
        PivoEventBus.subscribe(
            PivoEventBus.SCAN_DEVICE, this, Consumer {
            if (it is PivoEvent.Scanning){

                Log.e(TAG, "Result for scanning is updated")
                resultAdapter.addScanResult(it.device)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        //unregister pivo event bus
        PivoEventBus.unregister(this)
    }

    //open pivo controller screen
    private fun openController(){
//        startActivity(Intent(this, PivoControllerActivity::class.java))
        finishActivity(0)
    }

    //check permissions if they're granted start scanning, otherwise ask to user to grant permissions
    private fun checkPermission(){// alternative Permission library Dexter
        Permissions.check(this,
            permissionList, null, null,
            object : PermissionHandler() {
                override fun onGranted() {
                    scanning_bar.visibility = View.VISIBLE
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
        TODO("VERSION.SDK_INT < S")
    }

}
