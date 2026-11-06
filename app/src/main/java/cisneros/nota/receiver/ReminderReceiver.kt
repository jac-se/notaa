package cisneros.nota.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cisneros.nota.data.AppDb
import cisneros.nota.data.ReminderRepeat
import cisneros.nota.data.displayTitle
import cisneros.nota.work.ReminderNotificationHelper
import cisneros.nota.work.ReminderScheduleCalculator
import cisneros.nota.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(REMINDER_ID, -1L)
        if (reminderId < 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (reminderId == TEST_NOTIFICATION_ID) {
                    ReminderNotificationHelper.sendTestNotification(context.applicationContext)
                    return@launch
                }

                val dao = AppDb.get(context.applicationContext).reminderDao()
                val reminder = dao.getById(reminderId) ?: return@launch

                if (reminder.completed || reminder.triggerAt > System.currentTimeMillis() + 60_000L) {
                    return@launch
                }

                ReminderNotificationHelper.showNotification(
                    context = context.applicationContext,
                    notificationId = reminder.id.toInt(),
                    title = reminder.displayTitle(),
                    description = reminder.description
                )

                if (reminder.repeat == ReminderRepeat.NONE) {
                    dao.setCompleted(reminder.id, true)
                } else {
                    val next = ReminderScheduleCalculator.nextTrigger(reminder.triggerAt, reminder.repeat)
                    dao.updateTriggerAt(reminder.id, next)
                    ReminderScheduler.schedule(context.applicationContext, reminder.copy(triggerAt = next))
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val REMINDER_ID = "reminder_id"
        const val TEST_NOTIFICATION_ID = 9999L
    }
}
