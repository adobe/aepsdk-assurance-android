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

package com.adobe.marketing.mobile.assurancetvtestapp.homeView.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.ExperienceEvent
import com.adobe.marketing.mobile.MobileCore
//import com.adobe.marketing.mobile
import com.adobe.marketing.mobile.assurancetvtestapp.AssuranceTvTestAppConstants
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video
import com.adobe.marketing.mobile.assurancetvtestapp.sendEvent
import com.adobe.marketing.mobile.assurancetvtestapp.ui.components.ErrorView

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeView(
    videos: List<Video>,
    isLoading: Boolean,
    error: String?,
    onVideoClick: (Video) -> Unit,
    onRetry: () -> Unit = { Log.d("HomeView", "Retry button clicked - no retry handler provided") }
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFFFA0F00)
            )
        }
    } else if (error?.isNotEmpty() == true) {
        ErrorView(
            error = error,
            onRetry = onRetry
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Quick Connect Section
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.tv.material3.Text(
                    text = "Quick Connect",
                    style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                QuickActionCard(
                    title = "Connect to Assurance",
                    description = "Connect to Adobe Assurance",
                    icon = Icons.AutoMirrored.Filled.Send,
                    modifier = Modifier.fillMaxWidth(),
                    titleColor = Color.White,
                    onClick = {
                        Log.d("API_TEST", "Starting Assurance session...")
                        
                        try {
                            Assurance.startSession()
                            Log.d("API_TEST", "Assurance.startSession() called successfully")
                        } catch (e: Exception) {
                            Log.e("API_TEST", "Error starting Assurance session", e)
                        }
                    }
                )
            }
            
            // 2. Edge SendEvent and Analytics Section (side by side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Edge SendEvent Section
                Column(modifier = Modifier.weight(1f)) {
                    androidx.tv.material3.Text(
                        text = "Edge SendEvent",
                        style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    QuickActionCard(
                        title = "Send Edge Event",
                        description = "Send event using Edge.sendEvent API",
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.fillMaxWidth(),
                        titleColor = Color.White,
                        onClick = {
                            Log.d("API_TEST", "=== Starting Edge Event Send ===")
                            
                            try {
                                // Create sample XDM data
                                val xdmData = mapOf(
                                    "eventType" to "commerce.productViews",
                                    "commerce" to mapOf(
                                        "productViews" to mapOf(
                                            "value" to 1
                                        )
                                    ),
                                    "productListItems" to listOf(
                                        mapOf(
                                            "SKU" to "sample-sku-123",
                                            "name" to "Sample Product",
                                            "quantity" to 1,
                                            "priceTotal" to 99.99
                                        )
                                    ),
                                    "_id" to "sample-event-${System.currentTimeMillis()}"
                                )
                                
                                Log.d("API_TEST", "XDM Data created: $xdmData")
                                
                                // Create ExperienceEvent
                                val experienceEvent = ExperienceEvent.Builder()
                                    .setXdmSchema(xdmData)
                                    .build()
                                
                                Log.d("API_TEST", "ExperienceEvent created successfully")
                                
                                // Send event using Edge.sendEvent
                                Edge.sendEvent(experienceEvent) { handles ->
                                    Log.d("API_TEST", "=== Edge.sendEvent CALLBACK RECEIVED ===")
                                    Log.d("API_TEST", "Number of handles received: ${handles.size}")
                                    handles.forEachIndexed { index, handle ->
                                        Log.d("API_TEST", "Handle $index - Type: ${handle.type}")
                                        Log.d("API_TEST", "Handle $index - Payload: ${handle.payload}")
                                    }
                                    Log.d("API_TEST", "=== Edge.sendEvent CALLBACK COMPLETE ===")
                                }
                                
                                Log.d("API_TEST", "Edge.sendEvent() called successfully - waiting for callback...")
                            } catch (e: Exception) {
                                Log.e("API_TEST", "=== ERROR in Edge Event Send ===", e)
                                Log.e("API_TEST", "Error message: ${e.message}")
                                Log.e("API_TEST", "Error cause: ${e.cause}")
                            }
                        }
                    )
                }
                
                // Analytics Section
                Column(modifier = Modifier.weight(1f)) {
                    androidx.tv.material3.Text(
                        text = "Analytics",
                        style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    QuickActionCard(
                        title = "Track Action",
                        description = "Send action using MobileCore.trackAction",
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.fillMaxWidth(),
                        titleColor = Color.White,
                        onClick = {
                            Log.d("API_TEST", "Sending track action...")
                            
                            try {
                                // Create context data for the action
                                val contextData = mapOf(
                                    "action.name" to "tv_app_button_click",
                                    "action.type" to "user_interaction",
                                    "screen.name" to "home_view",
                                    "app.version" to "1.0.0",
                                    "device.type" to "android_tv",
                                    "timestamp" to System.currentTimeMillis().toString()
                                )
                                
                                // Send track action using MobileCore.trackAction
                                MobileCore.trackAction(
                                    "TV App - Track Action Button",
                                    contextData
                                )
                                
                                Log.d("API_TEST", "MobileCore.trackAction() called successfully")
                                Log.d("API_TEST", "Action: TV App - Track Action Button")
                                Log.d("API_TEST", "Context Data: $contextData")
                            } catch (e: Exception) {
                                Log.e("API_TEST", "Error sending track action", e)
                            }
                        }
                    )
                }
            }
            
            // 3. Event Chunking Section
            Column(modifier = Modifier.fillMaxWidth()) {
                androidx.tv.material3.Text(
                    text = "Event Chunking",
                    style = androidx.tv.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        title = "Small Payload",
                        description = "Send small event",
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Log.d(AssuranceTvTestAppConstants.TAG, "Sending small payload event...")
                            sendEvent(AssuranceTvTestAppConstants.SMALL_EVENT_PAYLOAD_FILE)
                        }
                    )
                    QuickActionCard(
                        title = "Large Payload",
                        description = "Send large event",
                        icon = Icons.AutoMirrored.Filled.Send,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            Log.d(AssuranceTvTestAppConstants.TAG, "Sending large payload event...")
                            sendEvent(AssuranceTvTestAppConstants.LARGE_EVENT_PAYLOAD_FILE)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun QuickActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    titleColor: Color = Color.White,
    onClick: (() -> Unit)? = null
) {
    androidx.tv.material3.Card(
        onClick = { 
            Log.d("QuickActionCard", "$title - Clicked")
            onClick?.invoke() 
        },
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                androidx.tv.material3.Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color(0xFFFA0F00),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                androidx.tv.material3.Text(
                    text = title,
                    style = androidx.tv.material3.MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = titleColor,
                    fontSize = 14.sp
                )
                androidx.tv.material3.Text(
                    text = description,
                    style = androidx.tv.material3.MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
            }
        }
    }
} 