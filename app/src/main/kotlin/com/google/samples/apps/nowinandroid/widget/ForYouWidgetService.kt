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

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.model.data.UserNewsResource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import androidx.core.net.toUri

@AndroidEntryPoint
class ForYouWidgetService : RemoteViewsService() {

    @Inject
    lateinit var userRepository: UserNewsResourceRepository

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ForYouRemoteViewsFactory(this.applicationContext, userRepository)
    }
}

class ForYouRemoteViewsFactory(
    private val context: Context,
    private val userRepository: UserNewsResourceRepository,
) : RemoteViewsService.RemoteViewsFactory {

    private var newsResources: List<UserNewsResource> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            try {
                newsResources = userRepository.observeAllForFollowedTopics().first()
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
            return RemoteViews(context.packageName, R.layout.for_you_widget_item)
        }

        val news = newsResources[position]
        val views = RemoteViews(context.packageName, R.layout.for_you_widget_item)
        views.setTextViewText(R.id.widget_item_title, news.title)
        views.setTextViewText(R.id.widget_item_description, news.content)

        // Create a deep link URI for this news resource.
        // The app is configured to handle this deep link and navigate to the article.
        val deepLinkUri =
            "https://www.nowinandroid.apps.samples.google.com/foryou/${news.id}".toUri()
        val fillInIntent = Intent().apply {
            data = deepLinkUri
            // Explicitly put the news ID extra as ForYouViewModel looks for it in SavedStateHandle
            putExtra("linkedNewsResourceId", news.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}
