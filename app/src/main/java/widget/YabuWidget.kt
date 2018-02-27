package widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.yabu.android.yabu.R

/**
 * Widget provider to handle update of data and binding to remote views.
 */
class YabuWidget: AppWidgetProvider() {

    companion object {

        val URL_ACTION = "com.yabu.android.yabu.URL_ACTION"
        val URL_EXTRA = "com.yabu.android.yabu.URL_EXTRA"

        /**
         * update widgets fun
         */
        fun updateYabuWidgets(context: Context, appWidgetManager: AppWidgetManager,
                              appWidgetIds: IntArray) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        /**
         * update a single widget with the manager
         */
        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            // update widget
            val views = getListViewRemoteView(context, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
        }

        /**
         * get the list view remote view and set the onclick pending template
         */
        private fun getListViewRemoteView(context: Context, appWidgetId: Int): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.yabu_widget_list)
            // set the listview widget service intent to act as the adapter for the listview
            val intent = Intent(context, ListViewWidgetService::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.widget_list_view, intent)

            // Set on click behaviour for each item
            val urlIntent = Intent(context, YabuWidget::class.java)
            urlIntent.action = YabuWidget.URL_ACTION
            val urlPendingIntent = PendingIntent.getBroadcast(context, 0, urlIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT)
            views.setPendingIntentTemplate(R.id.widget_list_view, urlPendingIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.empty_view)

            return views
        }
    }

    /**
     * Override function to update widget
     */
    override fun onUpdate(context: Context?, appWidgetManager: AppWidgetManager?,
                          appWidgetIds: IntArray?) {
        FetchCursorDataIntentService.startActionUpdateReviewWords(context)
        appWidgetManager?.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    /**
     * Override function to receive broadcast of pending intent
     */
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == URL_ACTION) {
            val url = intent.getStringExtra(YabuWidget.URL_EXTRA)
            if (url != null) {
                // Set the url with an intent.
                val webpage = Uri.parse(url)
                val intentWeb = Intent(Intent.ACTION_VIEW, webpage)
                if (intent.resolveActivity(context?.packageManager) != null) {
                    context?.startActivity(intentWeb)
                }
            }
        }
        super.onReceive(context, intent)
    }
}