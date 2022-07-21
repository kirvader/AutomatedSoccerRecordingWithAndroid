package app.pivo.android.basicsdkdemo

import android.app.Application
import android.util.Log
import app.pivo.android.basicsdk.PivoSdk
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.LogLevel
import com.elvishew.xlog.XLog
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.Printer
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.NeverBackupStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import java.io.*


/**
 * Created by murodjon on 2020/04/01
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        setupXLog()

        //initialize PivoSdk
        PivoSdk.init(this)
        try {
            PivoSdk.getInstance().unlockWithLicenseKey(getLicenseContent())
        } catch (ex: Exception) {
            XLog.e("Pivo key is expired or bad in some other way. Check it and try again.", ex)
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

        createLogFolder()

        val filePrinter: Printer =
            FilePrinter.Builder("$externalCacheDir/hawkeye")
                .fileNameGenerator(DateFileNameGenerator())
                .backupStrategy(NeverBackupStrategy())
                .build()

        XLog.init(config, androidPrinter, filePrinter)
    }

    private fun createLogFolder() {
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

    }

    private fun getLicenseContent(): String? {
        var inputStream = assets.open("licenceKey.json").bufferedReader().use { it.readText() }
        return readFile(inputStream)
    }

    @Throws(IOException::class)
    fun readFile(inputStream: InputStream?): String? {
        val str = StringBuilder()
        val br = BufferedReader(InputStreamReader(inputStream))
        var line: String?
        while (br.readLine().also { line = it } != null) {
            str.append(line)
        }
        br.close()
        return str.toString()
    }
}