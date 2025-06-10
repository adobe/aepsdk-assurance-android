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

package com.adobe.marketing.mobile.assurancetvtestapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.*
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.edge.bridge.EdgeBridge
import com.adobe.marketing.mobile.LoggingMode
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.Event
import com.adobe.marketing.mobile.Edge
import com.adobe.marketing.mobile.Lifecycle
import com.adobe.marketing.mobile.edge.identity.Identity
import com.adobe.marketing.mobile.edge.media.Media
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.repository.VideoRepository
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.ui.VideoGrid
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.ui.VideoPlayerScreen
import com.adobe.marketing.mobile.assurancetvtestapp.homeView.ui.HomeView
import com.adobe.marketing.mobile.assurancetvtestapp.aboutView.ui.AboutView
import com.adobe.marketing.mobile.assurancetvtestapp.ui.components.ErrorView
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.viewmodel.VideoViewModel

// Navigation items for the sidebar
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val id: String
)

@OptIn(ExperimentalTvMaterial3Api::class)
class TvHomeActivity : ComponentActivity() {
    private lateinit var viewModel: VideoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MobileCore.setLogLevel(LoggingMode.VERBOSE)
        
        // Initialize Mobile extensions

        MobileCore.initialize(
            this.application,
            "YOUR_APP_ID",
        ) {
            Log.d("TAG", "MobileCore Initialized")
        }

        // Initialize ViewModel
        viewModel = VideoViewModel(VideoRepository())

        setContent {
            androidx.tv.material3.MaterialTheme {
                androidx.tv.material3.Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    VideoScreen(viewModel)
                }
            }
        }
    }
}

/**
 * Send an event with the payload read from the resourceName provided.
 * This matches the implementation from AssuranceTestAppViewModel.
 */
fun sendEvent(resourceName: String) {
    val contentToSend = TvHomeActivity::class.java.classLoader?.getResource(resourceName)?.readText()
    if (contentToSend != null) {
        MobileCore.dispatchEvent(
            Event.Builder(
                AssuranceTvTestAppConstants.CHUNKED_EVENT_NAME,
                AssuranceTvTestAppConstants.CHUNKED_EVENT_TYPE,
                AssuranceTvTestAppConstants.CHUNKED_EVENT_SOURCE
            ).setEventData(
                mapOf(
                    AssuranceTvTestAppConstants.CHUNKED_EVENT_PAYLOAD_KEY to contentToSend
                )
            ).build()
        )
    } else {
        Log.e(AssuranceTvTestAppConstants.TAG, "Cannot send event. Failed to read content from: $resourceName")
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun VideoScreen(viewModel: VideoViewModel) {
    val videos = viewModel.videos.collectAsStateWithLifecycle()
    val isLoading = viewModel.isLoading.collectAsStateWithLifecycle()
    val error = viewModel.error.collectAsStateWithLifecycle()
    var selectedVideo by remember { mutableStateOf<Video?>(null) }
    var selectedNavItem by remember { mutableStateOf("home") }

    // Navigation items
    val navigationItems = listOf(
        NavigationItem("Home", Icons.Default.Home, "home"),
        NavigationItem("Videos", Icons.Default.PlayArrow, "videos"),
        NavigationItem("About", Icons.Default.Info, "about")
    )

    if (selectedVideo != null) {
        VideoPlayerScreen(
            video = selectedVideo!!,
            onBackClick = { selectedVideo = null }
        )
    } else {
        // Professional gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1A1A1A),
                            Color(0xFF0A0A0A)
                        )
                    )
                )
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Sidebar Navigation (YouTube-style)
                LeftSidebar(
                    navigationItems = navigationItems,
                    selectedItem = selectedNavItem,
                    onItemSelected = { selectedNavItem = it }
                )
                
                // Main content area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp, start = 24.dp, end = 48.dp)
                ) {
                    // App header with Adobe branding
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        // Adobe red accent bar
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(40.dp)
                                .background(Color(0xFFFA0F00))
                        )
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Enhanced title with custom typography
                        Column {
                            androidx.tv.material3.Text(
                                text = "Android Sample TV App",
                                style = androidx.tv.material3.MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = Color.White
                            )
                        }
                    }
                    
                    // Content area based on selected navigation
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 8.dp)
                    ) {
                        when (selectedNavItem) {
                            "home" -> {
                                HomeView(
                                    videos = videos.value,
                                    isLoading = isLoading.value,
                                    error = error.value,
                                    onVideoClick = { video -> selectedVideo = video },
                                    onRetry = { viewModel.retry() }
                                )
                            }
                            "videos" -> {
                                if (isLoading.value) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = Color(0xFFFA0F00)
                                    )
                                } else if (error.value?.isNotEmpty() == true) {
                                    ErrorView(
                                        error = error.value ?: "",
                                        onRetry = { viewModel.retry() }
                                    )
                                } else {
                                    VideoGrid(
                                        videos = videos.value,
                                        onVideoClick = { video -> selectedVideo = video }
                                    )
                                }
                            }
                            "about" -> {
                                AboutView()
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LeftSidebar(
    navigationItems: List<NavigationItem>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .fillMaxHeight()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D2D2D),
                        Color(0xFF1F1F1F),
                        Color(0xFF0F0F0F)
                    )
                )
            )
            .padding(vertical = 24.dp, horizontal = 20.dp)
    ) {
        // App title section
        Column(
            modifier = Modifier.padding(bottom = 40.dp)
        ) {
            androidx.tv.material3.Text(
                text = "TV APP",
                style = androidx.tv.material3.MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 2.sp
            )
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFA0F00),
                                Color(0xFFFF6B35)
                            )
                        )
                    )
            )
        }
        
        // Navigation items with custom styling
        navigationItems.forEachIndexed { index, item ->
            CustomNavigationItem(
                item = item,
                isSelected = selectedItem == item.id,
                onClick = { onItemSelected(item.id) },
                index = index
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        Spacer(modifier = Modifier.weight(1f))
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CustomNavigationItem(
    item: NavigationItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    androidx.tv.material3.Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = androidx.tv.material3.CardDefaults.colors(
            containerColor = when {
                isSelected -> Color(0xFFFA0F00).copy(alpha = 0.2f)
                else -> Color.Transparent
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Left accent line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .background(
                        color = if (isSelected) Color(0xFFFA0F00) else Color.Transparent,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Icon with background circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected) Color(0xFFFA0F00).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                androidx.tv.material3.Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    modifier = Modifier.size(20.dp),
                    tint = if (isSelected) Color(0xFFFA0F00) else Color.White.copy(alpha = 0.8f)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Text
            androidx.tv.material3.Text(
                text = item.title,
                style = androidx.tv.material3.MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}

