package cisneros.nota.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cisneros.nota.data.AppDb
import cisneros.nota.data.ReminderRepeat
import cisneros.nota.work.ReminderScheduleCalculator
import cisneros.nota.work.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.LOCKED_BOOT_COMPLETED"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = AppDb.get(context.applicationContext).reminderDao()
                    val pendingReminders = dao.getPending()
                    val now = System.currentTimeMillis()

                    for (reminder in pendingReminders) {
                        if (reminder.triggerAt > now) {
                            ReminderScheduler.schedule(context.applicationContext, reminder)
                        } else if (reminder.repeat != ReminderRepeat.NONE) {
                            var next = reminder.triggerAt
                            while (next <= now) {
                                next = ReminderScheduleCalculator.nextTrigger(next, reminder.repeat)
                            }
                            dao.updateTriggerAt(reminder.id, next)
                            ReminderScheduler.schedule(
                                context.applicationContext,
                                reminder.copy(triggerAt = next)
                            )
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
