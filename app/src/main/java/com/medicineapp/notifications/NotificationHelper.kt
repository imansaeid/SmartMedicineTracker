package com.medicineapp.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object NotificationHelper {

    private const val TAG = "NotificationHelper"
    private const val PREFS_REMINDERS = "medicine_reminders"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "medicine_reminders",
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders to take your medicines"
                enableVibration(true)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(
        context: Context,
        medicineId: Int,
        medicineName: String,
        timeStr: String,
        instruction: String,
        doseIndex: Int
    ) {
        try {
            val parts  = timeStr.split(":")
            val hour   = parts[0].toIntOrNull() ?: 8
            val minute = parts[1].toIntOrNull() ?: 0

            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_YEAR, 1)
            }

            val body = if (instruction.isNotEmpty() && instruction != "No Special Instruction")
                "Time to take $medicineName ($instruction) 💊"
            else
                "Time to take $medicineName 💊"

            val requestCode = medicineId * 10 + doseIndex

            val intent = Intent(context, ReminderReceiver::class.java).apply {
                putExtra("medicine_name", medicineName)
                putExtra("message", body)
                putExtra("medicine_id", medicineId)
                putExtra("time_str", timeStr)
                putExtra("dose_index", doseIndex)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
                        )
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent
                    )
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            }

            saveReminderData(context, requestCode, medicineId, medicineName, timeStr, instruction, doseIndex)
            Log.d(TAG, "Scheduled: $medicineName at $timeStr")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule: ${e.message}")
        }
    }

    fun cancelReminder(context: Context, medicineId: Int, doseIndex: Int) {
        val requestCode = medicineId * 10 + doseIndex
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(it)
        }
        removeReminderData(context, requestCode)
    }

    private fun saveReminderData(
        context: Context, requestCode: Int, medicineId: Int,
        medicineName: String, timeStr: String, instruction: String, doseIndex: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("${requestCode}_medicineId", medicineId)
            .putString("${requestCode}_name", medicineName)
            .putString("${requestCode}_time", timeStr)
            .putString("${requestCode}_instruction", instruction)
            .putInt("${requestCode}_doseIndex", doseIndex)
            .putBoolean("${requestCode}_active", true)
            .apply()

        val existing = prefs.getString("active_codes", "") ?: ""
        val codes = existing.split(",").filter { it.isNotEmpty() }.toMutableSet()
        codes.add(requestCode.toString())
        prefs.edit().putString("active_codes", codes.joinToString(",")).apply()
    }

    private fun removeReminderData(context: Context, requestCode: Int) {
        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        prefs.edit().remove("${requestCode}_active").apply()
        val existing = prefs.getString("active_codes", "") ?: ""
        val codes = existing.split(",").filter { it.isNotEmpty() && it != requestCode.toString() }
        prefs.edit().putString("active_codes", codes.joinToString(",")).apply()
    }

    fun rescheduleAllAfterBoot(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_REMINDERS, Context.MODE_PRIVATE)
        val codesStr = prefs.getString("active_codes", "") ?: ""
        if (codesStr.isEmpty()) return

        Log.d(TAG, "Rescheduling after boot...")
        for (codeStr in codesStr.split(",").filter { it.isNotEmpty() }) {
            val code = codeStr.toIntOrNull() ?: continue
            if (prefs.getBoolean("${code}_active", false)) {
                val medicineId  = prefs.getInt("${code}_medicineId", 0)
                val name        = prefs.getString("${code}_name", "") ?: ""
                val timeStr     = prefs.getString("${code}_time", "") ?: ""
                val instruction = prefs.getString("${code}_instruction", "") ?: ""
                val doseIndex   = prefs.getInt("${code}_doseIndex", 0)
                if (medicineId > 0 && timeStr.isNotEmpty()) {
                    scheduleReminder(context, medicineId, name, timeStr, instruction, doseIndex)
                }
            }
        }
    }
}
