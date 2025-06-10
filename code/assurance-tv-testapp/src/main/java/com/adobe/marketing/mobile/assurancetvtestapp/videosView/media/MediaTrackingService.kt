/*
 * Copyright 2023 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurancetvtestapp.videosView.media

import android.util.Log
import com.adobe.marketing.mobile.edge.media.Media
import com.adobe.marketing.mobile.edge.media.MediaConstants
import com.adobe.marketing.mobile.edge.media.MediaTracker
import com.adobe.marketing.mobile.assurancetvtestapp.videosView.model.Video

class MediaTrackingService {
    private var mediaTracker: MediaTracker? = null
    private var isSessionActive = false
    private var currentVideo: Video? = null
    
    companion object {
        private const val TAG = "MediaTrackingService"
        
        // Standard metadata keys
        private const val METADATA_VIDEO_SHOW = "a.media.show"
        private const val METADATA_VIDEO_SEASON = "a.media.season"
        private const val METADATA_VIDEO_EPISODE = "a.media.episode"
        private const val METADATA_VIDEO_GENRE = "a.media.genre"
        private const val METADATA_VIDEO_NETWORK = "a.media.network"
        private const val METADATA_VIDEO_ASSET_ID = "a.media.asset"
        private const val METADATA_VIDEO_RATING = "a.media.rating"
        private const val METADATA_VIDEO_ORIGINATOR = "a.media.originator"
        private const val METADATA_VIDEO_FRANCHISE = "a.media.franchise"
        private const val METADATA_VIDEO_MVPD = "a.media.pass.mvpd"
        private const val METADATA_VIDEO_AUTH = "a.media.pass.auth"
        private const val METADATA_VIDEO_DAY_PART = "a.media.dayPart"
        private const val METADATA_VIDEO_FEED = "a.media.feed"
        private const val METADATA_VIDEO_STREAM_FORMAT = "a.media.format"
    }
    
    /**
     * Initialize media tracking for a video
     */
    fun initializeTracking(video: Video) {
        try {
            Log.d(TAG, "Initializing media tracking for video: ${video.title}")
            currentVideo = video
            
            // Create media tracker configuration for Edge Network
            val config = mapOf(
                "config.channel" to "Assurance TV Test App",
                "config.downloadedcontent" to false,
                "config.appVersion" to "1.0.0"
            )
            
            // Create media tracker with configuration
            mediaTracker = Media.createTracker(config)
            
            // Create media object
            val mediaInfo = Media.createMediaObject(
                video.title,
                video.id,
                video.duration.toInt(),
                MediaConstants.StreamType.VOD,
                Media.MediaType.Video
            )
            
            // Create metadata
            val metadata = mapOf(
                MediaConstants.VideoMetadataKeys.SHOW to "Assurance TV Test App",
                MediaConstants.VideoMetadataKeys.EPISODE to video.title,
                "customKey" to "customValue"
            )
            
            // Start tracking session
            mediaTracker?.trackSessionStart(mediaInfo, metadata)
            
            Log.d(TAG, "Media tracking session started for: ${video.title}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing media tracking", e)
        }
    }
    
    /**
     * Start media session
     */
    fun startSession(videoDurationSeconds: Long, isResumed: Boolean = false) {
        try {
            val video = currentVideo ?: return
            val tracker = mediaTracker ?: return
            
            Log.d(TAG, "Starting media session for: ${video.title}")
            
            // Create media object
            val mediaInfo = Media.createMediaObject(
                video.title,
                video.id,
                videoDurationSeconds.toInt(),
                MediaConstants.StreamType.VOD,
                Media.MediaType.Video
            )
            
            // Add resumed information if applicable
            if (isResumed) {
                mediaInfo[MediaConstants.MediaObjectKey.RESUMED] = true
            }
            
            // Create metadata
            val metadata = createVideoMetadata(video)
            
            // Start tracking session
            tracker.trackSessionStart(mediaInfo, metadata)
            isSessionActive = true
            
            Log.d(TAG, "Media session started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting media session", e)
        }
    }
    
    /**
     * Track play event
     */
    fun trackPlay() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackPlay()
            Log.d(TAG, "Tracked play event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking play event", e)
        }
    }
    
    /**
     * Track pause event
     */
    fun trackPause() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackPause()
            Log.d(TAG, "Tracked pause event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking pause event", e)
        }
    }
    
    /**
     * Track seek start event
     */
    fun trackSeekStart() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.SeekStart, null, null)
            Log.d(TAG, "Tracked seek start event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking seek start event", e)
        }
    }
    
    /**
     * Track seek complete event
     */
    fun trackSeekComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.SeekComplete, null, null)
            Log.d(TAG, "Tracked seek complete event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking seek complete event", e)
        }
    }
    
    /**
     * Track buffer start event
     */
    fun trackBufferStart() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.BufferStart, null, null)
            Log.d(TAG, "Tracked buffer start event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking buffer start event", e)
        }
    }
    
    /**
     * Track buffer complete event
     */
    fun trackBufferComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.BufferComplete, null, null)
            Log.d(TAG, "Tracked buffer complete event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking buffer complete event", e)
        }
    }
    
    /**
     * Track bitrate change event
     */
    fun trackBitrateChange() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.BitrateChange, null, null)
            Log.d(TAG, "Tracked bitrate change event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking bitrate change event", e)
        }
    }
    
    /**
     * Track chapter start event
     */
    fun trackChapterStart(chapterName: String, chapterPosition: Int, chapterLength: Long, chapterStartTime: Long) {
        try {
            if (!isSessionActive) return
            
            val chapterInfo = Media.createChapterObject(
                chapterName,
                chapterPosition,
                chapterLength.toInt(),
                chapterStartTime.toInt()
            )
            
            val chapterMetadata = mapOf(
                "chapter.name" to chapterName,
                "chapter.position" to chapterPosition.toString(),
                "chapter.length" to chapterLength.toString(),
                "chapter.startTime" to chapterStartTime.toString()
            )
            
            mediaTracker?.trackEvent(Media.Event.ChapterStart, chapterInfo, chapterMetadata)
            Log.d(TAG, "Tracked chapter start: $chapterName (position: $chapterPosition, length: ${chapterLength}s, startTime: ${chapterStartTime}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking chapter start event", e)
        }
    }
    
    /**
     * Track chapter complete event
     */
    fun trackChapterComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.ChapterComplete, null, null)
            Log.d(TAG, "Tracked chapter complete event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking chapter complete event", e)
        }
    }
    
    /**
     * Track chapter skip event
     */
    fun trackChapterSkip() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.ChapterSkip, null, null)
            Log.d(TAG, "Tracked chapter skip event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking chapter skip event", e)
        }
    }
    
    /**
     * Track ad break start event
     */
    fun trackAdBreakStart(adBreakName: String, adBreakPosition: Int, adBreakStartTime: Long) {
        try {
            if (!isSessionActive) return
            
            val adBreakInfo = Media.createAdBreakObject(
                adBreakName,
                adBreakPosition,
                adBreakStartTime.toInt()
            )
            
            val adBreakMetadata = mapOf(
                "adbreak.name" to adBreakName,
                "adbreak.position" to adBreakPosition.toString(),
                "adbreak.startTime" to adBreakStartTime.toString()
            )
            
            mediaTracker?.trackEvent(Media.Event.AdBreakStart, adBreakInfo, adBreakMetadata)
            Log.d(TAG, "Tracked ad break start: $adBreakName (position: $adBreakPosition, startTime: ${adBreakStartTime}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking ad break start event", e)
        }
    }
    
    /**
     * Track ad break complete event
     */
    fun trackAdBreakComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.AdBreakComplete, null, null)
            Log.d(TAG, "Tracked ad break complete event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking ad break complete event", e)
        }
    }
    
    /**
     * Track ad start event
     */
    fun trackAdStart(adName: String, adId: String, adPosition: Int, adLength: Long) {
        try {
            if (!isSessionActive) return
            
            val adInfo = Media.createAdObject(
                adName,
                adId,
                adPosition,
                adLength.toInt()
            )
            
            val adMetadata = mapOf(
                "ad.name" to adName,
                "ad.id" to adId,
                "ad.position" to adPosition.toString(),
                "ad.length" to adLength.toString(),
                "ad.advertiser" to "Adobe Sample Advertiser",
                "ad.campaignId" to "Sample Campaign 2024",
                "ad.creativeId" to "Creative_${adId}",
                "ad.siteId" to "Sample Site",
                "ad.creativeUrl" to "https://example.com/ad/${adId}"
            )
            
            mediaTracker?.trackEvent(Media.Event.AdStart, adInfo, adMetadata)
            Log.d(TAG, "Tracked ad start: $adName (id: $adId, position: $adPosition, length: ${adLength}s)")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking ad start event", e)
        }
    }
    
    /**
     * Track ad complete event
     */
    fun trackAdComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.AdComplete, null, null)
            Log.d(TAG, "Tracked ad complete event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking ad complete event", e)
        }
    }
    
    /**
     * Track ad skip event
     */
    fun trackAdSkip() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackEvent(Media.Event.AdSkip, null, null)
            Log.d(TAG, "Tracked ad skip event")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking ad skip event", e)
        }
    }
    
    /**
     * Update current playhead position
     */
    fun updatePlayhead(positionSeconds: Long) {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.updateCurrentPlayhead(positionSeconds.toInt())
        } catch (e: Exception) {
            Log.e(TAG, "Error updating playhead", e)
        }
    }
    
    /**
     * Update Quality of Experience (QoE) information
     */
    fun updateQoE(bitrate: Long, startupTime: Long, fps: Long, droppedFrames: Long) {
        try {
            if (!isSessionActive) return
            
            val qoeInfo = Media.createQoEObject(bitrate.toInt(), startupTime.toInt(), fps.toInt(), droppedFrames.toInt())
            mediaTracker?.updateQoEObject(qoeInfo)
            
            Log.d(TAG, "Updated QoE: bitrate=$bitrate, startupTime=$startupTime, fps=$fps, droppedFrames=$droppedFrames")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating QoE", e)
        }
    }
    
    /**
     * Track error event
     */
    fun trackError(errorId: String) {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackError(errorId)
            Log.d(TAG, "Tracked error: $errorId")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking error event", e)
        }
    }
    
    /**
     * Track session complete
     */
    fun trackComplete() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackComplete()
            Log.d(TAG, "Tracked session complete")
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking complete event", e)
        }
    }
    
    /**
     * End media session
     */
    fun endSession() {
        try {
            if (!isSessionActive) return
            
            mediaTracker?.trackSessionEnd()
            isSessionActive = false
            
            Log.d(TAG, "Media session ended")
        } catch (e: Exception) {
            Log.e(TAG, "Error ending media session", e)
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        try {
            if (isSessionActive) {
                endSession()
            }
            mediaTracker = null
            currentVideo = null
            
            Log.d(TAG, "Media tracking cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
    
    /**
     * Create video metadata based on video information
     */
    private fun createVideoMetadata(video: Video): Map<String, String> {
        return mapOf(
            METADATA_VIDEO_SHOW to "Android TV Test App",
            METADATA_VIDEO_SEASON to "1",
            METADATA_VIDEO_EPISODE to video.id,
            METADATA_VIDEO_GENRE to "Technology",
            METADATA_VIDEO_NETWORK to "Adobe Experience Platform",
            METADATA_VIDEO_ASSET_ID to video.id,
            METADATA_VIDEO_RATING to "TV-G",
            METADATA_VIDEO_ORIGINATOR to "Adobe",
            METADATA_VIDEO_FRANCHISE to "Adobe Experience Platform Demo",
            METADATA_VIDEO_MVPD to "Adobe",
            METADATA_VIDEO_AUTH to "true",
            METADATA_VIDEO_DAY_PART to "primetime",
            METADATA_VIDEO_FEED to "live",
            METADATA_VIDEO_STREAM_FORMAT to "hd"
        )
    }
    
    /**
     * Check if session is currently active
     */
    fun isSessionActive(): Boolean = isSessionActive
} 