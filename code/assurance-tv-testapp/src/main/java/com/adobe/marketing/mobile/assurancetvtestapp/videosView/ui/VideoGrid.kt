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

package com.adobe.marketing.mobile.assurancetvtestapp.videosView.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.EdgeCallback
import com.adobe.marketing.mobile.EdgeEventHandle
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.EventSource
import com.adobe.marketing.mobile.EventType
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video
import android.util.Log

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoGrid(
    videos: List<Video>,
    onVideoClick: (Video) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
    ) {
        items(videos) { video ->
            VideoCard(
                video = video,
                onClick = { 
                    // Send an event to Adobe Mobile SDK if Big Buck Bunny is clicked
                    if (video.title == "Big Buck Bunny") {
                        sendVideoViewEvent(video)
                    }
                    
                    // Call the original onVideoClick function
                    onVideoClick(video)
                }
            )
        }
    }
}

/**
 * Sends an event to Adobe Experience Edge when a video is viewed
 */
private fun sendVideoViewEvent(video: Video) {
    // Create XDM data with video info
    val xdmData = HashMap<String, Any>()
    
    // Add event type
    xdmData["eventType"] = "media.videoClick"
    
    // Add video details
    val mediaDetails = HashMap<String, Any>()
    mediaDetails["name"] = video.title
    mediaDetails["id"] = video.id
    mediaDetails["length"] = video.duration
    mediaDetails["contentType"] = "VOD"
    mediaDetails["streamType"] = "video"
    mediaDetails["playerName"] = "Android TV Sample Player"
    mediaDetails["channel"] = "Android TV"
    mediaDetails["videoResolution"] = "${video.width}x${video.height}"
    
    // Add media details to XDM data
    xdmData["mediaCollection"] = mediaDetails
    
    // Additional custom data
    val customData = HashMap<String, Any>()
    customData["appSectionName"] = "video browser"
    customData["videoAction"] = "click"
    xdmData["customContext"] = customData
    
    // Create and send the Experience Event
    val experienceEvent = ExperienceEvent.Builder()
        .setXdmSchema(xdmData)
        .build()
    
    Edge.sendEvent(experienceEvent, object : EdgeCallback {
        override fun onComplete(handles: List<EdgeEventHandle>) {
            Log.d("VideoGrid", "Edge event completed: ${handles.size} responses")
            for (handle in handles) {
                Log.d("VideoGrid", "Edge response: ${handle.type} - ${handle.payload}")
            }
        }
    })
    
    // Also send a traditional event via MobileCore for backward compatibility
    val eventData = mapOf(
        "videoName" to video.title,
        "videoId" to video.id,
        "videoLength" to video.duration,
        "videoResolution" to "${video.width}x${video.height}",
        "videoAction" to "click"
    )
    
    val event = Event.Builder(
        "Video Click", 
        EventType.ANALYTICS, 
        EventSource.REQUEST_CONTENT
    ).setEventData(eventData).build()
    
    MobileCore.dispatchEvent(event)
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        colors = CardDefaults.colors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thumbnail image
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { error ->
                    Log.e("VideoCard", "Failed to load thumbnail for ${video.title}: ${error.result.throwable?.message}")
                },
                onSuccess = { 
                    Log.d("VideoCard", "Successfully loaded thumbnail for ${video.title}")
                }
            )
            
            // Gradient overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = 150f
                        )
                    )
            )
            
            // Title at the bottom with improved overflow handling
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(vertical = 8.dp, horizontal = 12.dp)
            ) {
                Text(
                    text = video.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Add a short description for context
                if (video.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = video.description,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Small indicator in top-right corner for video quality
            if (video.title.contains("4K") || video.title.contains("HD")) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF4D97FF))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (video.title.contains("4K")) "4K" else "HD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
} 