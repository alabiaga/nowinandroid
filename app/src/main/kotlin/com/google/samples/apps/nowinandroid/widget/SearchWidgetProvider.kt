/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.google.samples.apps.nowinandroid.MainActivity
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SearchWidgetProvider : AppWidgetProvider() {

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_BOOKMARK = "com.google.samples.apps.nowinandroid.widget.ACTION_BOOKMARK"
        const val EXTRA_NEWS_ID = "extra_news_id"
        const val EXTRA_BOOKMARKED = "extra_bookmarked"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_BOOKMARK) {
            val newsId = intent.getStringExtra(EXTRA_NEWS_ID)
            val bookmarked = intent.getBooleanExtra(EXTRA_BOOKMARKED, false)
            if (newsId != null) {
                scope.launch {
                    userDataRepository.setNewsResourceBookmarked(newsId, !bookmarked)
                    // Update the widget to reflect the change
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, SearchWidgetProvider::class.java)
                    appWidgetManager.notifyAppWidgetViewDataChanged(
                        appWidgetManager.getAppWidgetIds(componentName),
                        R.id.search_results_list
                    )
                }
            }
        }
        super.onReceive(context, intent)
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val serviceIntent = Intent(context, SearchWidgetService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }

        val views = RemoteViews(context.packageName, R.layout.search_widget).apply {
            setRemoteAdapter(R.id.search_results_list, serviceIntent)
            setEmptyView(R.id.search_results_list, R.id.search_empty_view)

            // Search bar and button open the app's search screen
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                // Assuming MainActivity handles search deep links or we can pass an extra
                action = Intent.ACTION_SEARCH
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context,
                1,
                searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setOnClickPendingIntent(R.id.widget_search_bar, searchPendingIntent)
            setOnClickPendingIntent(R.id.widget_search_button, searchPendingIntent)

            // Click intent template for the list items (Article deep link)
            val clickIntent = Intent(Intent.ACTION_VIEW).apply {
                setClass(context, MainActivity::class.java)
            }
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                2,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            setPendingIntentTemplate(R.id.search_results_list, clickPendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
