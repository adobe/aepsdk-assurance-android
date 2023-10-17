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

package com.adobe.marketing.mobile.assurance.internal.ui.floatingbutton

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.MobileCore
import com.adobe.marketing.mobile.assurance.R
import com.adobe.marketing.mobile.assurance.internal.ui.AssuranceActivity
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.Log
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.services.ui.FloatingButton
import com.adobe.marketing.mobile.services.ui.Presentable
import com.adobe.marketing.mobile.services.ui.PresentationError
import com.adobe.marketing.mobile.services.ui.floatingbutton.FloatingButtonEventListener
import com.adobe.marketing.mobile.services.ui.floatingbutton.FloatingButtonSettings
import com.adobe.marketing.mobile.util.DefaultPresentationUtilityProvider

/**
 * Floating button for displaying Assurance connectivity.
 * @param appContextService [AppContextService] to use for the application context
 */
internal class AssuranceFloatingButton(appContextService: AppContextService) {

    private companion object {
        private const val LOG_TAG = "AssuranceFloatingButton"
        private const val GRAPHIC_HEIGHT_DP = 80
        private const val GRAPHIC_WIDTH_DP = 80
        private const val GRAPHIC_CORNER_RADIUS = 10f
    }

    /**
     * Backup graphic to use in case the Assurance graphic cannot be loaded
     */
    private val backupGraphic = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    /**
     * Initial graphic to use when the floating button is created
     */
    private val initialGraphic: Bitmap =
        getGraphic(appContextService.applicationContext, R.drawable.ic_assurance_active)

    private val floatingButtonSettings: FloatingButtonSettings =
        FloatingButtonSettings.Builder()
            .height(GRAPHIC_HEIGHT_DP)
            .width(GRAPHIC_WIDTH_DP)
            .cornerRadius(GRAPHIC_CORNER_RADIUS)
            .initialGraphic(initialGraphic)
            .build()

    /**
     * Event listener for the floating button. The only event we care about is the tap event.
     * Configured to launch the Assurance activity when the button is tapped.
     */
    private val floatingButtonEventListener: FloatingButtonEventListener =
        object : FloatingButtonEventListener {
            override fun onDismiss(presentable: Presentable<FloatingButton>) {}
            override fun onError(
                presentable: Presentable<FloatingButton>,
                error: PresentationError
            ) {
            }

            override fun onHide(presentable: Presentable<FloatingButton>) {}
            override fun onPanDetected(presentable: Presentable<FloatingButton>) {}
            override fun onShow(presentable: Presentable<FloatingButton>) {}
            override fun onTapDetected(presentable: Presentable<FloatingButton>) {
                val hostApplication: Context? = MobileCore.getApplication()
                val intent = Intent(hostApplication, AssuranceActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                hostApplication?.startActivity(intent) ?: run {
                    Log.debug(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Failed to launch Assurance activity on floating button tap. " +
                            "Host application is null"
                    )
                }
            }
        }

    private val floatingButtonPresentation =
        FloatingButton(floatingButtonSettings, floatingButtonEventListener)

    private val floatingButtonPresentable: Presentable<FloatingButton> =
        ServiceProvider.getInstance().uiService.create(
            floatingButtonPresentation,
            DefaultPresentationUtilityProvider()
        )

    /**
     * Makes the floating button visible on the screen.
     */
    internal fun show() = floatingButtonPresentable.show()

    /**
     * Hides the floating button from the screen.
     */
    internal fun hide() = floatingButtonPresentable.hide()

    /**
     * Detaches the floating button from the view hierarchy.
     */
    internal fun remove() = floatingButtonPresentable.dismiss()

    /**
     * Returns true if the floating button is currently visible or hidden.
     */
    internal fun isActive(): Boolean =
        floatingButtonPresentable.getState() != Presentable.State.DETACHED

    /**
     * Updates the graphic of the floating button to reflect the current Assurance connectivity.
     * @param connected true if Assurance is connected, false otherwise
     */
    internal fun updateGraphic(connected: Boolean) {
        val context = ServiceProvider.getInstance().appContextService.applicationContext
        context?.let {
            val bitmap = getGraphic(
                it,
                if (connected) R.drawable.ic_assurance_active else R.drawable.ic_assurance_inactive
            )
            floatingButtonPresentable.getPresentation().eventHandler.updateGraphic(bitmap)
        } ?: run {
            Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Failed to update Assurance floating button graphic. " +
                    "Application context is null"
            )
        }
    }

    /**
     * Returns a [Bitmap] for the given resource if it exists, otherwise returns the [backupGraphic]
     * @param context the context to use to get the graphic
     */
    private fun getGraphic(context: Context?, resource: Int): Bitmap {
        if (context == null) {
            Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Failed to get Assurance floating button graphic. " +
                    "Application context is null"
            )
            return backupGraphic
        }

        val drawable: Drawable? = AppCompatResources.getDrawable(context, resource)
        if (drawable == null) {
            Log.debug(
                Assurance.LOG_TAG,
                LOG_TAG,
                "Failed to get Assurance floating button graphic. " +
                    "Drawable is null"
            )
            return backupGraphic
        }

        return drawable.toBitmap()
    }
}
