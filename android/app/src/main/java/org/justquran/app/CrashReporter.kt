package org.justquran.app

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashReporter {
    private const val FILE_NAME = "last_crash.txt"
    private const val PREFS = "crash_reporter"
    private const val KEY_SEEN_MILLIS = "seen_crash_millis"
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching { writeReport(appContext, thread, throwable) }
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable)
            } else {
                Process.killProcess(Process.myPid())
                System.exit(10)
            }
        }
    }

    private fun writeReport(app: Context, thread: Thread, throwable: Throwable) {
        val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
        val appVer = runCatching {
            val pInfo = app.packageManager.getPackageInfo(app.packageName, 0)
            "${pInfo.versionName} (${pInfo.longVersionCode})"
        }.getOrDefault("unknown")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()

        val sb = StringBuilder("JustQuran crash report\n")
            .append("Time: $timeStr\n")
            .append("App: $appVer\n")
            .append("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})\n")
            .append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
            .append("Thread: ${thread.name}\n\n")
            .append(stackTrace)

        reportFile(app).writeText(sb.toString())
    }

    private fun reportFile(context: Context): File =
        File(context.applicationContext.filesDir, FILE_NAME)

    fun hasReport(context: Context): Boolean = reportFile(context).isFile

    fun readReport(context: Context): String? = runCatching {
        val file = reportFile(context)
        if (file.isFile) file.readText() else null
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { reportFile(context).delete() }
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_SEEN_MILLIS).apply()
    }

    fun hasUnseenReport(context: Context): Boolean {
        val file = reportFile(context)
        if (!file.isFile) return false
        val seen = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_SEEN_MILLIS, -1L)
        return seen != file.lastModified()
    }

    fun markSeen(context: Context) {
        val file = reportFile(context)
        if (file.isFile) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_SEEN_MILLIS, file.lastModified()).apply()
        }
    }
}
