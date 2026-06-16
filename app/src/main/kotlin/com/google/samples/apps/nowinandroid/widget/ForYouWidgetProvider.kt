/*
 * Copyright 2023 The Android Open Source Project
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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.google.samples.apps.nowinandroid.MainActivity
import com.google.samples.apps.nowinandroid.R

class ForYouWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val intent = Intent(context, ForYouWidgetService::class.java)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        intent.data = Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME))

        val views = RemoteViews(context.packageName, R.layout.for_you_widget)
        views.setRemoteAdapter(R.id.for_you_list, intent)
        views.setEmptyView(R.id.for_you_list, R.id.empty_view)

        // Click intent template for the list items - use ACTION_VIEW for deep linking
        val clickIntent = Intent(Intent.ACTION_VIEW)
        clickIntent.setClass(context, MainActivity::class.java)
        val clickPendingIntent = PendingIntent.getActivity(
            context,
            0,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE // Must be MUTABLE to allow fillInIntent to add data
        )
        views.setPendingIntentTemplate(R.id.for_you_list, clickPendingIntent)
        
        // Title click opens the app normally
        val titleIntent = Intent(context, MainActivity::class.java)
        val titlePendingIntent = PendingIntent.getActivity(
            context,
            0,
            titleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, titlePendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}
