package com.toylek.studio

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Process
import android.os.SystemClock
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.toylek.studio.di.AppContainer
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class ToylekApp : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        installCrashGuard()
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(VideoFrameDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.20).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("thumbs"))
                .maxSizeBytes(150L * 1024 * 1024)
                .build()
        }
        .crossfade(true)
        .build()

    /**
     * Надёжность на мероприятии: при непредвиденном сбое записываем журнал и,
     * если включён режим киоска, автоматически перезапускаем приложение.
     */
    private fun installCrashGuard() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                writeCrashLog(error)
                val prefs = getSharedPreferences("crash_guard", Context.MODE_PRIVATE)
                val last = prefs.getLong("last_crash", 0L)
                val now = System.currentTimeMillis()
                prefs.edit().putLong("last_crash", now).commit()
                val kiosk = container.settingsRepository.kioskEnabledNow()
                // Не перезапускаемся в цикле, если сбой повторяется сразу после старта.
                if (kiosk && now - last > 20_000L) scheduleRestart()
            } catch (_: Throwable) {
            }
            if (container.settingsRepository.kioskEnabledNow()) {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            } else {
                previous?.uncaughtException(thread, error)
            }
        }
    }

    private fun scheduleRestart() {
        val intent = Intent(this, MainActivity::class.java).addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        )
        val pi = PendingIntent.getActivity(
            this, 42, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
        )
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1500L, pi)
    }

    private fun writeCrashLog(error: Throwable) {
        try {
            val sw = StringWriter()
            error.printStackTrace(PrintWriter(sw))
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val file = File(filesDir, "crash.log")
            if (file.length() > 512 * 1024) file.delete()
            file.appendText("==== $stamp ====\n$sw\n")
        } catch (_: Throwable) {
        }
    }
}
