package cisneros.nota.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import cisneros.nota.MainActivity
import cisneros.nota.R

/**
 * Widget sencillo: botón que abre la app en "nueva nota".
 */
class QuickAddNoteWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_NEW_NOTE = "cisneros.nota.action.NEW_NOTE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_add_note)

            // Intent que abre MainActivity con una acción para nueva nota
            val intent = Intent(context, MainActivity::class.java).apply {
                action = ACTION_NEW_NOTE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(
                context,
                appWidgetId, // requestCode distinto por widget
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(R.id.btnQuickAdd, pi)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
