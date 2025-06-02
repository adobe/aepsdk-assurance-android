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

@file:Suppress("EXPERIMENTAL_IS_NOT_ENABLED")
@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.adobe.marketing.mobile.assurancetvtestapp.videosView.ui

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.media.MediaTrackingService
import com.adobe.marketing.mobile.assurancetvtestapp.ui.components.ErrorView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.concurrent.TimeUnit

data class Chapter(
    val name: String,
    val position: Int,
    val startTime: Long, // in milliseconds
    val duration: Long   // in milliseconds
)

data class AdBreak(
    val name: String,
    val position: Int,
    val startTime: Long, // in milliseconds
    val ads: List<Ad>
)

data class Ad(
    val name: String,
    val id: String,
    val position: Int,
    val duration: Long // in milliseconds
)

@Composable
fun VideoPlayerScreen(
    video: Video,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    
    // Adobe Media Tracking
    val mediaTrackingService = remember { MediaTrackingService() }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var showVolumePanel by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    
    // Chapter tracking state
    var currentChapter by remember { mutableStateOf(0) }
    var hasChapterStarted by remember { mutableStateOf(false) }
    
    // Ad tracking state
    var isAdPlaying by remember { mutableStateOf(false) }
    var currentAdBreak by remember { mutableStateOf(0) }
    var currentAdInBreak by remember { mutableStateOf(0) }
    var adStartTime by remember { mutableStateOf(0L) }
    var hasAdBreakStarted by remember { mutableStateOf(false) }
    
    // Define ad breaks (pre-roll, mid-roll, post-roll)
    val adBreaks = remember(duration) {
        if (duration > 0) {
            listOf(
                AdBreak("Pre-roll", 1, 0L, listOf(
                    Ad("Adobe Experience Platform Ad", "ad_001", 1, 15000L),
                    Ad("Adobe Analytics Ad", "ad_002", 2, 10000L)
                )),
                AdBreak("Mid-roll", 2, duration / 2, listOf(
                    Ad("Adobe Target Ad", "ad_003", 1, 20000L)
                )),
                AdBreak("Post-roll", 3, duration - 5000L, listOf(
                    Ad("Adobe Campaign Ad", "ad_004", 1, 12000L)
                ))
            )
        } else {
            emptyList()
        }
    }
    
    // Define chapters based on video duration (divide into 4 chapters)
    val chapters = remember(duration) {
        if (duration > 0) {
            val chapterDuration = duration / 4 // 4 chapters
            listOf(
                Chapter("Introduction", 1, 0L, chapterDuration),
                Chapter("Main Content", 2, chapterDuration, chapterDuration),
                Chapter("Climax", 3, chapterDuration * 2, chapterDuration),
                Chapter("Conclusion", 4, chapterDuration * 3, chapterDuration)
            )
        } else {
            emptyList()
        }
    }
    
    // Get max volume and current volume
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    var currentVolume by remember { mutableStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    
    val volumeSliderValue = remember(currentVolume, maxVolume) { 
        if (maxVolume > 0) currentVolume.toFloat() / maxVolume.toFloat() else 0f 
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(video.videoUrl)))
            setHandleAudioBecomingNoisy(true)
            volume = 1f // Ensure volume is set to maximum in ExoPlayer
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            isLoading = true
                            isBuffering = true
                            Log.d("VideoPlayer", "Buffering...")
                            mediaTrackingService.trackBufferStart()
                        }
                        Player.STATE_READY -> {
                            if (isBuffering) {
                                mediaTrackingService.trackBufferComplete()
                                isBuffering = false
                            }
                            isLoading = false
                            duration = this@apply.duration
                            Log.d("VideoPlayer", "Ready to play")
                            
                            // Start media session when ready
                            if (!mediaTrackingService.isSessionActive()) {
                                val durationSeconds = if (duration > 0) duration / 1000 else 0
                                mediaTrackingService.startSession(durationSeconds)
                            }
                        }
                        Player.STATE_ENDED -> {
                            isLoading = false
                            Log.d("VideoPlayer", "Playback ended")
                            
                            // Complete current ad if one was active
                            if (isAdPlaying && hasAdBreakStarted) {
                                mediaTrackingService.trackAdComplete()
                                mediaTrackingService.trackAdBreakComplete()
                                Log.d("VideoPlayer", "Final ad completed")
                            }
                            
                            // Complete current chapter if one was active
                            if (hasChapterStarted && chapters.isNotEmpty() && currentChapter < chapters.size) {
                                mediaTrackingService.trackChapterComplete()
                                Log.d("VideoPlayer", "Final chapter ${chapters[currentChapter].name} completed")
                            }
                            
                            mediaTrackingService.trackComplete()
                            mediaTrackingService.endSession()
                        }
                        Player.STATE_IDLE -> {
                            isLoading = false
                            Log.d("VideoPlayer", "Idle state")
                        }
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("VideoPlayer", "Error playing video: ${error.message}")
                    errorMessage = error.message
                    isLoading = false
                    mediaTrackingService.trackError(error.errorCode.toString())
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    val wasPlaying = isPlaying
                    isPlaying = isPlayingNow
                    
                    // Track play/pause events
                    if (mediaTrackingService.isSessionActive()) {
                        if (isPlayingNow && !wasPlaying) {
                            mediaTrackingService.trackPlay()
                        } else if (!isPlayingNow && wasPlaying) {
                            mediaTrackingService.trackPause()
                        }
                    }
                }
            })
        }
    }

    // Initialize media tracking when component starts
    LaunchedEffect(video) {
        mediaTrackingService.initializeTracking(video)
    }

    // Timer to update current position and playhead
    LaunchedEffect(Unit) {
        while (isActive) {
            val newPosition = exoPlayer.currentPosition
            currentPosition = newPosition
            
            // Update playhead for Adobe Media tracking
            if (mediaTrackingService.isSessionActive()) {
                mediaTrackingService.updatePlayhead(newPosition / 1000) // Convert to seconds
                
                // Chapter tracking logic
                if (chapters.isNotEmpty()) {
                    val newChapterIndex = chapters.indexOfFirst { chapter ->
                        newPosition >= chapter.startTime && newPosition < (chapter.startTime + chapter.duration)
                    }
                    
                    if (newChapterIndex != -1 && newChapterIndex != currentChapter) {
                        // End previous chapter if one was active
                        if (hasChapterStarted && currentChapter < chapters.size) {
                            mediaTrackingService.trackChapterComplete()
                            Log.d("VideoPlayer", "Chapter ${chapters[currentChapter].name} completed")
                        }
                        
                        // Start new chapter
                        currentChapter = newChapterIndex
                        val chapter = chapters[currentChapter]
                        mediaTrackingService.trackChapterStart(
                            chapterName = chapter.name,
                            chapterPosition = chapter.position,
                            chapterLength = chapter.duration / 1000, // Convert to seconds
                            chapterStartTime = chapter.startTime / 1000 // Convert to seconds
                        )
                        hasChapterStarted = true
                        Log.d("VideoPlayer", "Chapter ${chapter.name} started at position ${newPosition / 1000}s")
                    }
                }
                
                // Ad simulation logic
                if (adBreaks.isNotEmpty() && !isAdPlaying) {
                    // Check if we should start an ad break
                    val adBreakToStart = adBreaks.find { adBreak ->
                        newPosition >= adBreak.startTime && 
                        newPosition < (adBreak.startTime + 5000) && // 5 second window
                        currentAdBreak != adBreak.position
                    }
                    
                    if (adBreakToStart != null) {
                        // Start ad break
                        currentAdBreak = adBreakToStart.position
                        currentAdInBreak = 0
                        isAdPlaying = true
                        hasAdBreakStarted = true
                        adStartTime = newPosition
                        
                        mediaTrackingService.trackAdBreakStart(
                            adBreakName = adBreakToStart.name,
                            adBreakPosition = adBreakToStart.position,
                            adBreakStartTime = adBreakToStart.startTime / 1000
                        )
                        
                        // Start first ad in the break
                        if (adBreakToStart.ads.isNotEmpty()) {
                            val firstAd = adBreakToStart.ads[0]
                            mediaTrackingService.trackAdStart(
                                adName = firstAd.name,
                                adId = firstAd.id,
                                adPosition = firstAd.position,
                                adLength = firstAd.duration / 1000
                            )
                            Log.d("VideoPlayer", "Ad break ${adBreakToStart.name} started with ad ${firstAd.name}")
                        }
                    }
                }
                
                // Handle ad completion simulation
                if (isAdPlaying && hasAdBreakStarted) {
                    val currentAdBreakData = adBreaks.find { it.position == currentAdBreak }
                    if (currentAdBreakData != null && currentAdInBreak < currentAdBreakData.ads.size) {
                        val currentAd = currentAdBreakData.ads[currentAdInBreak]
                        val adElapsedTime = newPosition - adStartTime
                        
                        // Check if current ad should complete (simulate ad duration)
                        if (adElapsedTime >= currentAd.duration) {
                            mediaTrackingService.trackAdComplete()
                            Log.d("VideoPlayer", "Ad ${currentAd.name} completed")
                            
                            currentAdInBreak++
                            
                            // Check if there are more ads in this break
                            if (currentAdInBreak < currentAdBreakData.ads.size) {
                                // Start next ad in the break
                                val nextAd = currentAdBreakData.ads[currentAdInBreak]
                                adStartTime = newPosition
                                mediaTrackingService.trackAdStart(
                                    adName = nextAd.name,
                                    adId = nextAd.id,
                                    adPosition = nextAd.position,
                                    adLength = nextAd.duration / 1000
                                )
                                Log.d("VideoPlayer", "Next ad ${nextAd.name} started")
                            } else {
                                // All ads in break completed
                                mediaTrackingService.trackAdBreakComplete()
                                isAdPlaying = false
                                hasAdBreakStarted = false
                                Log.d("VideoPlayer", "Ad break ${currentAdBreakData.name} completed")
                            }
                        }
                    }
                }
                
                // Update QoE metrics every 10 seconds
                if (newPosition % 10000 < 1000) { // Every 10 seconds (with 1 second tolerance)
                    val bitrate = 2500000L // 2.5 Mbps (simulated)
                    val startupTime = 1200L // 1.2 seconds (simulated)
                    val fps = 30L // 30 FPS (simulated)
                    val droppedFrames = 0L // No dropped frames (simulated)
                    
                    mediaTrackingService.updateQoE(bitrate, startupTime, fps, droppedFrames)
                }
            }
            
            delay(1000) // Update every second
        }
    }

    // Clean up media tracking when component is disposed
    DisposableEffect(Unit) {
        onDispose {
            mediaTrackingService.cleanup()
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video Player
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                        this.player = exoPlayer
                        useController = false // Disable default controls
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading...",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            errorMessage?.let { message ->
                ErrorView(
                    error = message,
                    onRetry = {
                        // Reset error and try to reload the video
                        errorMessage = null
                        exoPlayer.prepare()
                        exoPlayer.playWhenReady = true
                    }
                )
            }

            // Control buttons in top-left corner
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Back button 
                Button(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(width = 160.dp, height = 50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D7AF6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "←",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "BACK",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Volume control button (now below back button)
                Button(
                    onClick = { showVolumePanel = !showVolumePanel },
                    modifier = Modifier
                        .size(width = 160.dp, height = 50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2D7AF6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔊",
                            fontSize = 16.sp
                        )
                        Text(
                            text = "VOLUME",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Chapter controls
                if (chapters.isNotEmpty()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Chapter tracking controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Chapter start button
                            Card(
                                onClick = {
                                    if (chapters.isNotEmpty()) {
                                        val currentChapterData = chapters[currentChapter]
                                        mediaTrackingService.trackChapterStart(
                                            chapterName = "Manual ${currentChapterData.name}",
                                            chapterPosition = currentChapterData.position,
                                            chapterLength = currentChapterData.duration / 1000,
                                            chapterStartTime = currentPosition / 1000
                                        )
                                        hasChapterStarted = true
                                    }
                                },
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(55.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF1B5E20)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 10.dp,
                                    focusedElevation = 12.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = CardDefaults.outlinedCardBorder(enabled = true).copy(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF4CAF50),
                                                    Color(0xFF2E7D32),
                                                    Color(0xFF1B5E20)
                                                ),
                                                radius = 120f
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "CHAPTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = 0.85f),
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "START",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                            
                            // Chapter end button
                            Card(
                                onClick = {
                                    if (hasChapterStarted) {
                                        mediaTrackingService.trackChapterComplete()
                                        hasChapterStarted = false
                                    }
                                },
                                enabled = hasChapterStarted,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(55.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (hasChapterStarted) Color(0xFF1B5E20) else Color(0xFF424242)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (hasChapterStarted) 6.dp else 2.dp,
                                    pressedElevation = 10.dp,
                                    focusedElevation = if (hasChapterStarted) 12.dp else 4.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = if (hasChapterStarted) {
                                    CardDefaults.outlinedCardBorder(enabled = true).copy(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )
                                } else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (hasChapterStarted) {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF4CAF50),
                                                        Color(0xFF2E7D32),
                                                        Color(0xFF1B5E20)
                                                    ),
                                                    radius = 120f
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF757575),
                                                        Color(0xFF616161),
                                                        Color(0xFF424242)
                                                    ),
                                                    radius = 120f
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "CHAPTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = if (hasChapterStarted) 0.85f else 0.5f),
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "END",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = if (hasChapterStarted) 1f else 0.5f),
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Chapter navigation controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Previous chapter button
                            Card(
                                onClick = {
                                    if (currentChapter > 0) {
                                        val prevChapter = chapters[currentChapter - 1]
                                        exoPlayer.seekTo(prevChapter.startTime)
                                        
                                        // Track chapter skip
                                        if (hasChapterStarted) {
                                            mediaTrackingService.trackChapterSkip()
                                        }
                                    }
                                },
                                enabled = currentChapter > 0,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(55.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentChapter > 0) Color(0xFF0D47A1) else Color(0xFF424242)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (currentChapter > 0) 6.dp else 2.dp,
                                    pressedElevation = 10.dp,
                                    focusedElevation = if (currentChapter > 0) 12.dp else 4.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = if (currentChapter > 0) {
                                    CardDefaults.outlinedCardBorder(enabled = true).copy(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )
                                } else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (currentChapter > 0) {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF2196F3),
                                                        Color(0xFF1976D2),
                                                        Color(0xFF0D47A1)
                                                    ),
                                                    radius = 120f
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF757575),
                                                        Color(0xFF616161),
                                                        Color(0xFF424242)
                                                    ),
                                                    radius = 120f
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "CHAPTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = if (currentChapter > 0) 0.85f else 0.5f),
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "◀ BACK",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = if (currentChapter > 0) 1f else 0.5f),
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }
                            
                            // Next chapter button
                            Card(
                                onClick = {
                                    if (currentChapter < chapters.size - 1) {
                                        val nextChapter = chapters[currentChapter + 1]
                                        exoPlayer.seekTo(nextChapter.startTime)
                                        
                                        // Track chapter skip
                                        if (hasChapterStarted) {
                                            mediaTrackingService.trackChapterSkip()
                                        }
                                    }
                                },
                                enabled = currentChapter < chapters.size - 1,
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(55.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (currentChapter < chapters.size - 1) Color(0xFF0D47A1) else Color(0xFF424242)
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = if (currentChapter < chapters.size - 1) 6.dp else 2.dp,
                                    pressedElevation = 10.dp,
                                    focusedElevation = if (currentChapter < chapters.size - 1) 12.dp else 4.dp
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = if (currentChapter < chapters.size - 1) {
                                    CardDefaults.outlinedCardBorder(enabled = true).copy(
                                        width = 1.5.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                        )
                                    )
                                } else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            if (currentChapter < chapters.size - 1) {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF2196F3),
                                                        Color(0xFF1976D2),
                                                        Color(0xFF0D47A1)
                                                    ),
                                                    radius = 120f
                                                )
                                            } else {
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0xFF757575),
                                                        Color(0xFF616161),
                                                        Color(0xFF424242)
                                                    ),
                                                    radius = 120f
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "CHAPTER",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color.White.copy(alpha = if (currentChapter < chapters.size - 1) 0.85f else 0.5f),
                                            letterSpacing = 0.6.sp
                                        )
                                        Text(
                                            text = "NEXT ▶",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White.copy(alpha = if (currentChapter < chapters.size - 1) 1f else 0.5f),
                                            letterSpacing = 0.8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Ad controls
                if (adBreaks.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Ad start button
                        Card(
                            onClick = {
                                // Manually trigger an ad break for testing
                                if (!isAdPlaying && adBreaks.isNotEmpty()) {
                                    val testAdBreak = adBreaks[0] // Use first ad break for testing
                                    currentAdBreak = testAdBreak.position
                                    currentAdInBreak = 0
                                    isAdPlaying = true
                                    hasAdBreakStarted = true
                                    adStartTime = currentPosition
                                    
                                    mediaTrackingService.trackAdBreakStart(
                                        adBreakName = "Manual Test ${testAdBreak.name}",
                                        adBreakPosition = testAdBreak.position,
                                        adBreakStartTime = currentPosition / 1000
                                    )
                                    
                                    if (testAdBreak.ads.isNotEmpty()) {
                                        val firstAd = testAdBreak.ads[0]
                                        mediaTrackingService.trackAdStart(
                                            adName = firstAd.name,
                                            adId = firstAd.id,
                                            adPosition = firstAd.position,
                                            adLength = firstAd.duration / 1000
                                        )
                                    }
                                }
                            },
                            enabled = !isAdPlaying,
                            modifier = Modifier
                                .width(90.dp)
                                .height(55.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!isAdPlaying) Color(0xFFE65100) else Color(0xFF424242)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (!isAdPlaying) 6.dp else 2.dp,
                                pressedElevation = 10.dp,
                                focusedElevation = if (!isAdPlaying) 12.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (!isAdPlaying) {
                                CardDefaults.outlinedCardBorder(enabled = true).copy(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                            } else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (!isAdPlaying) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFF9800),
                                                    Color(0xFFFF6F00),
                                                    Color(0xFFE65100)
                                                ),
                                                radius = 120f
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF757575),
                                                    Color(0xFF616161),
                                                    Color(0xFF424242)
                                                ),
                                                radius = 120f
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "AD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = if (!isAdPlaying) 0.85f else 0.5f),
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "▶ START",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = if (!isAdPlaying) 1f else 0.5f),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                        
                        // Ad complete button
                        Card(
                            onClick = {
                                if (isAdPlaying) {
                                    mediaTrackingService.trackAdComplete()
                                    mediaTrackingService.trackAdBreakComplete()
                                    isAdPlaying = false
                                    hasAdBreakStarted = false
                                }
                            },
                            enabled = isAdPlaying,
                            modifier = Modifier
                                .width(90.dp)
                                .height(55.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAdPlaying) Color(0xFFE65100) else Color(0xFF424242)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isAdPlaying) 6.dp else 2.dp,
                                pressedElevation = 10.dp,
                                focusedElevation = if (isAdPlaying) 12.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isAdPlaying) {
                                CardDefaults.outlinedCardBorder(enabled = true).copy(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                            } else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isAdPlaying) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFF9800),
                                                    Color(0xFFFF6F00),
                                                    Color(0xFFE65100)
                                                ),
                                                radius = 120f
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF757575),
                                                    Color(0xFF616161),
                                                    Color(0xFF424242)
                                                ),
                                                radius = 120f
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "AD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = if (isAdPlaying) 0.85f else 0.5f),
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "■ COMPLETE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = if (isAdPlaying) 1f else 0.5f),
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                        }
                        
                        // Ad skip button
                        Card(
                            onClick = {
                                if (isAdPlaying) {
                                    mediaTrackingService.trackAdSkip()
                                    mediaTrackingService.trackAdBreakComplete()
                                    isAdPlaying = false
                                    hasAdBreakStarted = false
                                }
                            },
                            enabled = isAdPlaying,
                            modifier = Modifier
                                .width(90.dp)
                                .height(55.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAdPlaying) Color(0xFFFF8F00) else Color(0xFF424242)
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isAdPlaying) 6.dp else 2.dp,
                                pressedElevation = 10.dp,
                                focusedElevation = if (isAdPlaying) 12.dp else 4.dp
                            ),
                            shape = RoundedCornerShape(14.dp),
                            border = if (isAdPlaying) {
                                CardDefaults.outlinedCardBorder(enabled = true).copy(
                                    width = 1.5.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                                    )
                                )
                            } else null
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (isAdPlaying) {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFFFFC107),
                                                    Color(0xFFFFB300),
                                                    Color(0xFFFF8F00)
                                                ),
                                                radius = 120f
                                            )
                                        } else {
                                            Brush.radialGradient(
                                                colors = listOf(
                                                    Color(0xFF757575),
                                                    Color(0xFF616161),
                                                    Color(0xFF424242)
                                                ),
                                                radius = 120f
                                            )
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "AD",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = if (isAdPlaying) 0.85f else 0.5f),
                                        letterSpacing = 0.6.sp
                                    )
                                    Text(
                                        text = "⏩ SKIP",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White.copy(alpha = if (isAdPlaying) 1f else 0.5f),
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Adobe Media Tracking indicator in top-right corner
            if (mediaTrackingService.isSessionActive()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .background(
                            Color(0xFFFA0F00).copy(alpha = 0.9f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Animated recording dot
                        var animatedAlpha by remember { mutableStateOf(1f) }
                        LaunchedEffect(Unit) {
                            while (isActive) {
                                animatedAlpha = if (animatedAlpha == 1f) 0.3f else 1f
                                delay(1000)
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color.White.copy(alpha = animatedAlpha),
                                    CircleShape
                                )
                        )
                        
                        Text(
                            text = "Adobe Media Tracking",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Video Info Panel with Progress Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Color(0xFF0A0A0A)
                )
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 16.dp)
        ) {
            // Time and Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Time indicators moved to top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),  // Add space between timestamps and progress bar
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDuration(currentPosition / 1000),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Duration text
                        Text(
                            text = formatDuration(duration / 1000),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Play/Pause button moved to right side of time display
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D7AF6),
                                contentColor = Color.White
                            )
                        ) {
                            // Custom play/pause graphics using Canvas
                            Canvas(modifier = Modifier.size(44.dp)) {
                                if (isPlaying) {
                                    // Draw pause icon (two smaller vertical bars)
                                    val barWidth = size.width * 0.15f
                                    val barHeight = size.height * 0.6f
                                    val spacing = size.width * 0.15f
                                    val startY = (size.height - barHeight) / 2
                                    
                                    // Left bar
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset((size.width / 2) - spacing - barWidth, startY),
                                        size = Size(barWidth, barHeight)
                                    )
                                    
                                    // Right bar
                                    drawRect(
                                        color = Color.White,
                                        topLeft = Offset((size.width / 2) + spacing, startY),
                                        size = Size(barWidth, barHeight)
                                    )
                                } else {
                                    // Draw play triangle (balanced size)
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        val centerX = size.width / 2
                                        val centerY = size.height / 2
                                        val radius = size.width / 2
                                        
                                        moveTo(centerX - (radius * 0.7f), centerY - (radius * 0.85f))
                                        lineTo(centerX - (radius * 0.7f), centerY + (radius * 0.85f))
                                        lineTo(centerX + (radius * 0.85f), centerY)
                                        close()
                                    }
                                    
                                    drawPath(
                                        path = path,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                
                    // Enhanced progress bar now clearly below timestamps
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        // Progress indicator
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f)
                                .background(Color(0xFF2D7AF6))
                        )
                        
                        // Chapter markers
                        if (chapters.isNotEmpty() && duration > 0) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                chapters.forEachIndexed { index, chapter ->
                                    if (index > 0) { // Skip first chapter marker (at 0)
                                        val markerPosition = chapter.startTime.toFloat() / duration.toFloat()
                                        val x = markerPosition * size.width
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.8f),
                                            start = Offset(x, 0f),
                                            end = Offset(x, size.height),
                                            strokeWidth = 4f
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Seekable slider overlay (transparent but handles input)
                        Slider(
                            value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { value ->
                                // Track seek start if not already seeking
                                if (!isSeeking && mediaTrackingService.isSessionActive()) {
                                    mediaTrackingService.trackSeekStart()
                                    isSeeking = true
                                }
                                
                                // Update position immediately in UI for better feedback
                                currentPosition = (value * duration).toLong()
                            },
                            onValueChangeFinished = {
                                // Actually seek when user finishes dragging
                                exoPlayer.seekTo(currentPosition)
                                
                                // Track seek complete
                                if (isSeeking && mediaTrackingService.isSessionActive()) {
                                    mediaTrackingService.trackSeekComplete()
                                    isSeeking = false
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2D7AF6),
                                activeTrackColor = Color.Transparent,
                                inactiveTrackColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp) // Taller touch target
                        )
                    }
                }
            }
            
            // Title and info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = video.title,
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Text(
                        text = video.description,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Info chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoChip(
                        text = "${video.width}x${video.height}",
                        icon = "🎥"
                    )
                    InfoChip(
                        text = formatDuration(video.duration.toLong()),
                        icon = "⏱️"
                    )
                    
                    // Chapter indicator
                    if (chapters.isNotEmpty() && currentChapter < chapters.size) {
                        InfoChip(
                            text = chapters[currentChapter].name,
                            icon = "📖"
                        )
                    }
                    
                    // Ad indicator
                    if (isAdPlaying && adBreaks.isNotEmpty()) {
                        val currentAdBreakData = adBreaks.find { it.position == currentAdBreak }
                        if (currentAdBreakData != null && currentAdInBreak < currentAdBreakData.ads.size) {
                            val currentAd = currentAdBreakData.ads[currentAdInBreak]
                            InfoChip(
                                text = "AD: ${currentAd.name}",
                                icon = "📺",
                                backgroundColor = Color(0xFFFF6B35)
                            )
                        }
                    }
                }
            }
        }
            
        // Volume panel (shown when volume control clicked)
        AnimatedVisibility(
            visible = showVolumePanel,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 184.dp, top = 90.dp)
                    .width(300.dp)
                    .height(200.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Volume Control",
                        fontSize = 18.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🔈",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        
                        Slider(
                            value = volumeSliderValue,
                            onValueChange = { value ->
                                val newVolume = (value * maxVolume).toInt()
                                audioManager.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    newVolume,
                                    0 // No flags
                                )
                                currentVolume = newVolume
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF2D7AF6),
                                activeTrackColor = Color(0xFF2D7AF6),
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Text(
                            text = "🔊",
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                    
                    Text(
                        text = "${(volumeSliderValue * 100).toInt()}%",
                        fontSize = 16.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoChip(
    text: String,
    icon: String,
    backgroundColor: Color = Color.White.copy(alpha = 0.1f)
) {
    Surface(
        modifier = Modifier.padding(vertical = 4.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            androidx.compose.material3.Text(
                text = text,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

private fun formatDuration(seconds: Long): String {
    return String.format(
        "%02d:%02d:%02d",
        TimeUnit.SECONDS.toHours(seconds),
        TimeUnit.SECONDS.toMinutes(seconds) % TimeUnit.HOURS.toMinutes(1),
        seconds % TimeUnit.MINUTES.toSeconds(1)
    )
} 