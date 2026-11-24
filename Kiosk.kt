package com.toylek.studio.kiosk

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context

class KioskAdminReceiver : DeviceAdminReceiver()

/**
 * Режим киоска.
 * • Обычный режим: закрепление экрана (Android попросит подтверждение один раз).
 * • Если приложение назначено владельцем устройства (dpm set-device-owner), блокировка полная и без подтверждений.
 */
object KioskController {

    fun isDeviceOwner(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isLocked(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
    }

    fun enter(activity: Activity) {
        try {
            if (isDeviceOwner(activity)) {
                val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                val admin = ComponentName(activity, KioskAdminReceiver::class.java)
                dpm.setLockTaskPackages(admin, arrayOf(activity.packageName))
            }
            if (!isLocked(activity)) activity.startLockTask()
        } catch (_: Exception) {
            // Некоторые прошивки запрещают закрепление — приложение продолжает работать без него.
        }
    }

    fun exit(activity: Activity) {
        try {
            if (isLocked(activity)) activity.stopLockTask()
        } catch (_: Exception) {
        }
    }
}
