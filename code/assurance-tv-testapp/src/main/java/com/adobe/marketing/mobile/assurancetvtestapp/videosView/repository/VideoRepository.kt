/*
 * Copyright 2025 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurancetvtestapp.videosView.repository

import com.adobe.marketing.mobile.assurancetvtestapp.videosView.data.SampleVideos
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Log

class VideoRepository {
    fun getPopularVideos(@Suppress("UNUSED_PARAMETER") page: Int = 1): Flow<List<Video>> = flow {
        try {
            // Simulate potential network delay
            kotlinx.coroutines.delay(500)
            Log.d("VideoRepository", "Loading ${SampleVideos.videos.size} videos")
            emit(SampleVideos.videos)
        } catch (e: Exception) {
            Log.e("VideoRepository", "Error loading videos: ${e.message}", e)
            throw e // Propagate error instead of swallowing it
        }
    }
} 