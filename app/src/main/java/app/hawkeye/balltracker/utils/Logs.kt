package app.hawkeye.balltracker.utils

import com.elvishew.xlog.Logger
import com.elvishew.xlog.XLog


inline fun<reified T> createLogger(): Logger = XLog.tag(T::class.java.name).build()