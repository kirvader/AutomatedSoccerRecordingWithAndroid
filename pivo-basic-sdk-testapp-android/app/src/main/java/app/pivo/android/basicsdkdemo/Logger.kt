package app.pivo.android.basicsdkdemo

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDateTime


fun appendToLog(text: String) {
    val logFile = File("sdcard/log.txt")
    if (!logFile.exists()) {
        try {
            logFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    try {
        //BufferedWriter for performance, true to set append to file flag
        val buf = BufferedWriter(FileWriter(logFile, true))
        buf.append(makeLogText(text))
        buf.newLine()
        buf.close()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun getCurrentTime(): String {
    val current = LocalDateTime.now()

    return current.toString()
}

fun makeLogText(text: String): String {
    return "${getCurrentTime().padEnd(35)} $text"
}