package com.medicineapp.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.medicineapp.MainActivity
import com.medicineapp.R
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicine_name") ?: "Medicine"
        val message      = intent.getStringExtra("message") ?: "Time to take your medicine 💊"
        val medicineId   = intent.getIntExtra("medicine_id", 0)
        val timeStr      = intent.getStringExtra("time_str") ?: ""
        val doseIndex    = intent.getIntExtra("dose_index", 0)

        // أظهر الإشعار
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, medicineId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, "medicine_reminders")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("💊 Medicine Reminder")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(medicineId * 10 + doseIndex, notification)

        // جدول اليوم الجاي يدوياً (لأن setExactAndAllowWhileIdle بيطلق مرة وحدة فقط)
        if (timeStr.isNotEmpty()) {
            rescheduleForNextDay(context, intent, timeStr, medicineId, doseIndex)
        }
    }

    private fun rescheduleForNextDay(
        context: Context,
        originalIntent: Intent,
        timeStr: String,
        medicineId: Int,
        doseIndex: Int
    ) {
        try {
            val parts  = timeStr.split(":")
            val hour   = parts[0].toIntOrNull() ?: return
            val minute = parts[1].toIntOrNull() ?: return

            val tomorrow = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val requestCode = medicineId * 10 + doseIndex
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, originalIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, tomorrow.timeInMillis, pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, tomorrow.timeInMillis, pendingIntent
                        )
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, tomorrow.timeInMillis, pendingIntent
                    )
                }
                else -> {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, tomorrow.timeInMillis, pendingIntent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ReminderReceiver", "Reschedule failed: ${e.message}")
        }
    }
}
