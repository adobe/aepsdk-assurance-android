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

package com.adobe.marketing.mobile.assurancetvtestapp.videosView.data

import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video

object SampleVideos {
    val videos = listOf(
        Video(
            id = "1",
            title = "Big Buck Bunny",
            description = "Big Buck Bunny tells the story of a giant rabbit with a heart bigger than himself.",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            duration = 596,
            width = 1920,
            height = 1080
        ),
        Video(
            id = "2",
            title = "Elephant Dream",
            description = "The first Blender Open Movie from 2006",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            duration = 653,
            width = 1920,
            height = 1080
        ),
        Video(
            id = "3",
            title = "Sintel",
            description = "Third Blender Open Movie from 2010",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            duration = 888,
            width = 1920,
            height = 1080
        ),
        Video(
            id = "4",
            title = "Tears of Steel",
            description = "Tears of Steel was realized with crowd-funding by users of the open source 3D creation tool Blender.",
            thumbnailUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            videoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            duration = 734,
            width = 1920,
            height = 1080
        )
    )
} 