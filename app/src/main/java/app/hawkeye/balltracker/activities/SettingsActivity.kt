package app.hawkeye.balltracker.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import app.hawkeye.balltracker.R
import app.hawkeye.balltracker.rotatable.PivoPodDevice
import app.hawkeye.balltracker.utils.RuntimeUtils
import app.hawkeye.balltracker.utils.createLogger
import app.hawkeye.balltracker.views.configs.ModelSelectorAdapter
import app.hawkeye.balltracker.views.configs.ObjectExtractorAdapter
import app.hawkeye.balltracker.views.configs.TilingStrategyAdapter
import app.hawkeye.balltracker.views.configs.TrackingStrategyAdapter
import kotlinx.android.synthetic.main.activity_settings.*

private val LOG = createLogger<SettingsActivity>()

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        trackingStrategySelector.adapter = TrackingStrategyAdapter()
        trackingStrategySelector.layoutManager = LinearLayoutManager(this)

        tilingStrategySelector.adapter = TilingStrategyAdapter()
        tilingStrategySelector.layoutManager = LinearLayoutManager(this)

        modelSelectorsSelector.adapter = ModelSelectorAdapter()
        modelSelectorsSelector.layoutManager = LinearLayoutManager(this)

        objectExtractorsSelector.adapter = ObjectExtractorAdapter()
        objectExtractorsSelector.layoutManager = LinearLayoutManager(this)

        scan_for_controllable_devices_button.setOnClickListener {
            if (RuntimeUtils.isEmulator()) {
                LOG.i("Scan button pressed")
            } else {
                PivoPodDevice.scanForPivoDevices(this, layoutInflater)
            }
        }

        back_button.setOnClickListener{
            finish()
        }

    }
}