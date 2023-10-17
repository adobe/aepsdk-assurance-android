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

package com.adobe.marketing.mobile.assurance

import android.app.Activity
import android.graphics.Color
import android.graphics.LightingColorFilter
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.ProgressBar
import android.widget.TextView
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.assurance.AssuranceConstants.AssuranceConnectionError
import com.adobe.marketing.mobile.services.Log
import java.util.concurrent.Executors

class AssuranceQuickConnectActivity : Activity() {

    private companion object {
        private const val LOG_SOURCE = "AssuranceQuickConnectActivity"
    }

    private lateinit var connectButtonView: View
    private lateinit var connectButton: ProgressButton
    private lateinit var cancelButtonView: View
    private lateinit var errorDetailTextView: TextView
    private lateinit var errorTitleTextView: TextView
    private val sessionStatusListener = object : AssuranceSessionStatusListener {
        override fun onSessionConnected() {
            Log.trace(Assurance.LOG_TAG, LOG_SOURCE, "Session Connected. Finishing activity.")
            finish()
        }

        override fun onSessionDisconnected(error: AssuranceConnectionError?) {
            Log.trace(Assurance.LOG_TAG, LOG_SOURCE, "Session disconnected.")
            error?.let {
                showError(error)
            }
        }

        override fun onSessionTerminated(error: AssuranceConnectionError?) {
            Log.trace(Assurance.LOG_TAG, LOG_SOURCE, "Session terminated.")
            error?.let {
                showError(error)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        hideSystemUI()
        setContentView(R.layout.quick_connect_screen_layout)

        val assuranceStateManager = AssuranceComponentRegistry.assuranceStateManager
        val sessionUIOperationHandler = AssuranceComponentRegistry.sessionUIOperationHandler

        if (!AssuranceUtil.isDebugBuild(application)) {
            Log.warning(Assurance.LOG_TAG, LOG_SOURCE, "QuickConnect cannot be initiated. Application is not in debug mode.")
            finish()
            return
        }

        if (assuranceStateManager == null || sessionUIOperationHandler == null) {
            Log.warning(Assurance.LOG_TAG, LOG_SOURCE, "Required components for QuickConnect are unavailable.")
            finish()
            return
        }

        // Initialize UI elements for QuickConnect screen
        setupQuickConnectScreen()

        val quickConnectCallback = object : QuickConnectCallback {
            override fun onError(error: AssuranceConnectionError) {
                showError(error)
            }

            override fun onSuccess(sessionUUID: String, token: String) {
                sessionUIOperationHandler.onConnect(sessionUUID, token, AssuranceConstants.AssuranceEnvironment.PROD, sessionStatusListener, SessionAuthorizingPresentationType.QUICK_CONNECT)
            }
        }

        val quickConnectManager = QuickConnectManager(
            assuranceStateManager,
            Executors.newSingleThreadScheduledExecutor(),
            quickConnectCallback
        )

        configureConnectButton(connectButtonView, quickConnectManager)
        configureCancelButton(cancelButtonView, quickConnectManager)
    }

    /**
     * Initializes components of the Quick Connect UI.
     */
    private fun setupQuickConnectScreen() {
        connectButtonView = findViewById(R.id.connectButton)
        connectButton = ProgressButton("Connect", connectButtonView)

        cancelButtonView = findViewById<View>(R.id.cancelButton).also {
            it.setBackgroundResource(R.drawable.shape_custom_button_outlined)
            it.findViewById<TextView>(R.id.buttonText).also { button ->
                button.text = getString(R.string.quick_connect_button_cancel)
            }

            it.findViewById<ProgressBar>(R.id.progressBar).also { progressBar ->
                progressBar.visibility = View.GONE
            }
        }

        errorTitleTextView = findViewById<TextView>(R.id.errorTitleTextView).also {
            it.visibility = View.GONE
        }

        errorDetailTextView = findViewById<TextView>(R.id.errorDetailTextView).also {
            it.visibility = View.GONE
        }
    }

    /**
     * Configures the "Connect" button on the UI to trigger QuickConnect based on current state.
     */
    private fun configureConnectButton(
        connectionButtonView: View,
        quickConnectManager: QuickConnectManager
    ) {
        connectionButtonView.setOnClickListener {
            when (connectButton.state) {
                ProgressButton.State.IDLE -> {
                    connectButton.waiting()
                    quickConnectManager.registerDevice()
                }

                ProgressButton.State.RETRY -> {
                    hideError()
                    connectButton.waiting()
                    quickConnectManager.registerDevice()
                }

                else -> {
                    // All other states are affected internally based on connection.
                    // So do nothing.
                }
            }
        }
    }

    /**
     * Configures the "Connect" button on the UI to cancel ongoing QuickConnect
     * and finish the activity.
     */
    private fun configureCancelButton(
        cancelButtonView: View,
        quickConnectManager: QuickConnectManager
    ) {
        cancelButtonView.setOnClickListener {
            quickConnectManager.cancel()
            AssuranceComponentRegistry.sessionUIOperationHandler?.onCancel()
            finish()
        }
    }

    /**
     * Hides the contents of [errorDetailTextView] and [errorDetailTextView]
     */
    private fun hideError() {
        runOnUiThread {
            errorTitleTextView.text = ""
            errorTitleTextView.visibility = View.GONE
            errorDetailTextView.text = ""
            errorDetailTextView.visibility = View.GONE
        }
    }

    /**
     * Displays [errorDetailTextView] and [errorDetailTextView] based on the
     * [connectionError]
     */
    private fun showError(connectionError: AssuranceConnectionError) {
        runOnUiThread {
            errorTitleTextView.text = connectionError.error
            errorTitleTextView.visibility = View.VISIBLE
            errorDetailTextView.text = connectionError.description
            errorDetailTextView.visibility = View.VISIBLE
            if (connectionError.isRetryable) {
                connectButton.retry()
            } else {
                connectButtonView.visibility = View.GONE
            }
        }
    }

    private fun hideSystemUI() {
        this.window
            .decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            )
    }

    /**
     * A wrapper for managing states of a "progress button" which is essentially a View composed of
     * a TextView and a progress spinner.
     */
    private class ProgressButton(private val initialLabel: String, private val view: View) {
        private val progressBar: ProgressBar = view.findViewById<ProgressBar?>(R.id.progressBar)
            .also { it.visibility = View.GONE }
        private val text: TextView = view.findViewById<TextView?>(R.id.buttonText)
            .also { it.text = initialLabel }
        internal var state: State = State.IDLE
            private set

        init {
            idle()
        }

        internal enum class State {
            IDLE,
            WAITING,
            RETRY
        }

        fun idle() {
            state = State.IDLE
            text.text = view.resources.getString(R.string.quick_connect_button_connect)
            progressBar.visibility = View.GONE
            view.setBackgroundResource(R.drawable.shape_custom_button_filled)
        }

        fun waiting() {
            state = State.WAITING
            text.text = view.resources.getString(R.string.quick_connect_button_waiting)

            // Using a ColorFilter instead of a tint because it is not supported for Api 19
            progressBar.indeterminateDrawable.colorFilter =
                LightingColorFilter(Color.rgb(6, 142, 228), Color.TRANSPARENT)
            progressBar.visibility = View.VISIBLE
            view.setBackgroundResource(R.drawable.shape_custom_button_inactive)
        }

        fun retry() {
            state = State.RETRY
            text.text = view.resources.getString(R.string.quick_connect_button_retry)
            progressBar.visibility = View.GONE
            view.setBackgroundResource(R.drawable.shape_custom_button_filled)
        }
    }
}
