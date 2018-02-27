package widget

import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

/**
 * Intent service to handle the update action of the widget.
 */
class FetchCursorDataIntentService(name: String = "FetchCursorDataIntentService") : IntentService(name) {

    companion object {
        val ACTION_UPDATE = "com.yabu.android.yabu.ACTION_UPDATE"

        /**
         * Sends the intent for the update action service.
         */
        fun startActionUpdateReviewWords(context: Context?) {
            val intent = Intent(context, FetchCursorDataIntentService::class.java)
            intent.action = ACTION_UPDATE
            context?.startService(intent)
        }
    }

    /**
     * Handles the sent intent
     */
    override fun onHandleIntent(intent: Intent?) {
        val action = intent?.action
        if (ACTION_UPDATE == action) {
            handleUpdateAction()
        }
    }

    private fun handleUpdateAction() {
        // get vals from the widget manager
        val appWidgetManager = AppWidgetManager.getInstance(this@FetchCursorDataIntentService)
        val appWidgetIds = appWidgetManager.
                getAppWidgetIds(ComponentName(this, YabuWidget::class.java))
        // Update all widgets
        YabuWidget.updateYabuWidgets(this, appWidgetManager, appWidgetIds)
    }
}