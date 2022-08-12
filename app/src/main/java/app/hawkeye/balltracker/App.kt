package app.hawkeye.balltracker

import android.app.Application
import android.util.Log
import app.hawkeye.balltracker.rotatable.PivoPodDevice
import app.hawkeye.balltracker.utils.RuntimeUtils
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import com.hawkeye.movement.interfaces.RotatableDevice
import java.io.File


/**
 * Created by murodjon on 2020/04/01
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        setupXLog()

        device = if (!RuntimeUtils.isEmulator()) {
            PivoPodDevice(this)
        } else {
            RotatableDevice.Dummy
        }
    }

    private fun setupXLog() {
        // XLog initialization
        val config = LogConfiguration.Builder()
            .logLevel(LogLevel.ALL)
            .tag("Hawk-eye") // default tag
            .enableThreadInfo()
            .enableBorder()
            .build()

        val androidPrinter: Printer = AndroidPrinter(true)

        val logFolder = createLogFolder()

        val filePrinter: Printer =
            FilePrinter.Builder(logFolder.path)
                .fileNameGenerator(DateFileNameGenerator())
                .backupStrategy(NeverBackupStrategy())
                .build()

        XLog.init(config, androidPrinter, filePrinter)
    }

    private fun createLogFolder(): File {
        val mediaStorageDir = File(externalCacheDir, "hawkeye")

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("App", "failed to create directory")
            } else {
                Log.d("App", "Success. Added folder ${mediaStorageDir.path}")
            }
        } else {
            Log.d("App", "Folder ${mediaStorageDir.path} already exists")
        }
        return mediaStorageDir
    }

    companion object {
        private lateinit var device: RotatableDevice

        fun getRotatableDevice() = device
    }
}