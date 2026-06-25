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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.data.repository.NewsRepository
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import com.google.samples.apps.nowinandroid.core.model.data.mapToUserNewsResources
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SearchWidgetService : RemoteViewsService() {

    @Inject
    lateinit var newsRepository: NewsRepository

    @Inject
    lateinit var userDataRepository: UserDataRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return SearchRemoteViewsFactory(this.applicationContext, newsRepository, userDataRepository)
    }
}

class SearchRemoteViewsFactory(
    private val context: Context,
    private val newsRepository: NewsRepository,
    private val userDataRepository: UserDataRepository
) : RemoteViewsService.RemoteViewsFactory {

    private var newsResources: List<UserNewsResource> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                // For this widget, we'll show the most recent articles as the "archives"
                // since we don't have a way to pass a search query from the widget UI easily.
                val userData = userDataRepository.userData.first()
                val news = newsRepository.getNewsResources().first()
                newsResources = news.mapToUserNewsResources(userData).sortedByDescending { it.publishDate }
            } catch (e: Exception) {
                newsResources = emptyList()
            }
        }
    }

    override fun onDestroy() {
        newsResources = emptyList()
    }

    override fun getCount(): Int = newsResources.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= newsResources.size) {
            return RemoteViews(context.packageName, R.layout.search_widget_item)
        }

        val news = newsResources[position]
        val views = RemoteViews(context.packageName, R.layout.search_widget_item)
        views.setTextViewText(R.id.search_widget_item_title, news.title)
        views.setTextViewText(R.id.search_widget_item_description, news.content)

        // Set bookmark icon based on state
        val bookmarkIcon = if (news.isSaved) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        views.setImageViewResource(R.id.widget_bookmark_button, bookmarkIcon)

        // Bookmark action (Broadcast)
        val bookmarkIntent = Intent(context, SearchWidgetProvider::class.java).apply {
            action = SearchWidgetProvider.ACTION_BOOKMARK
            putExtra(SearchWidgetProvider.EXTRA_NEWS_ID, news.id)
            putExtra(SearchWidgetProvider.EXTRA_BOOKMARKED, news.isSaved)
            // Use data to make the intent unique so extras aren't merged
            data = Uri.parse("bookmark://${news.id}")
        }
        val bookmarkPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            bookmarkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_bookmark_button, bookmarkPendingIntent)

        // Click action (Fill-in intent for the template in Provider)
        val deepLinkUri = Uri.parse("https://www.nowinandroid.apps.samples.google.com/foryou/${news.id}")
        val fillInIntent = Intent().apply {
            data = deepLinkUri
            putExtra("linkedNewsResourceId", news.id)
        }
        views.setOnClickFillInIntent(R.id.search_widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
