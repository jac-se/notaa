package cisneros.nota.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cisneros.nota.MainActivity
import cisneros.nota.data.ReminderEntity
import cisneros.nota.receiver.ReminderReceiver
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun schedule(context: Context, reminder: ReminderEntity) {
        if (reminder.completed || reminder.triggerAt <= System.currentTimeMillis()) return

        scheduleAlarm(context, reminder)
        scheduleWork(context, reminder)
    }

    fun cancel(context: Context, reminderId: Long) {
        cancelAlarm(context, reminderId)
        cancelWork(context, reminderId)
    }

    private fun scheduleAlarm(context: Context, reminder: ReminderEntity) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.REMINDER_ID, reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showAppPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            showAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = reminder.triggerAt

        if (ReminderNotificationHelper.canScheduleExactAlarms(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAt, showAppPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleWork(context: Context, reminder: ReminderEntity) {
        val delayMs = reminder.triggerAt - System.currentTimeMillis()
        if (delayMs <= 0) return
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(Data.Builder().putLong(ReminderWorker.REMINDER_ID, reminder.id).build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "reminder-" + reminder.id,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun cancelWork(context: Context, reminderId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder-" + reminderId)
    }

    fun scheduleTestAlarm(context: Context, delayMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAt = System.currentTimeMillis() + delayMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.REMINDER_ID, ReminderReceiver.TEST_NOTIFICATION_ID)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ReminderReceiver.TEST_NOTIFICATION_ID.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showAppIntent = Intent(context, MainActivity::class.java)
        val showAppPendingIntent = PendingIntent.getActivity(
            context,
            ReminderReceiver.TEST_NOTIFICATION_ID.toInt(),
            showAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (ReminderNotificationHelper.canScheduleExactAlarms(context)) {
            val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAt, showAppPendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }
}
