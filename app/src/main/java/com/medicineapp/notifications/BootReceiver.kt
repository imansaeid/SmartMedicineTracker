package com.medicineapp.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // أعد جدولة كل التذكيرات بعد إعادة تشغيل الجهاز
            NotificationHelper.rescheduleAllAfterBoot(context)
        }
    }
}
