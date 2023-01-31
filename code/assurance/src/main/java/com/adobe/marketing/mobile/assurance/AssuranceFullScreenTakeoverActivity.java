/*
 * Copyright 2022 Adobe. All rights reserved.
 * This file is licensed to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 * OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.adobe.marketing.mobile.assurance;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.lang.ref.WeakReference;

/** The Android {@link Activity} used to display the fullscreen message. */
@SuppressLint("Registered")
public class AssuranceFullScreenTakeoverActivity extends Activity {
    private static final String LOG_TAG = "AssuranceFullScreenTakeoverActivity";

    protected static WeakReference<AssuranceFullScreenTakeover> weakFullScreenTakeoverReference =
            new WeakReference<>(null);
    // variable to determine if the AssuranceFullscreenActivity is actively being displayed
    protected static boolean isDisplayed;

    protected static void setFullscreenMessage(final AssuranceFullScreenTakeover takeover) {
        weakFullScreenTakeoverReference = new WeakReference<>(takeover);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUI();
        // we don't actually use this layout, but we need it to get on the activity stack
        setContentView(new RelativeLayout(this));

        // The window is never allowed to overlap with the DisplayCutout area.
        // DisplayCutout represents the area of the display that is not functional for displaying
        // content.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER;
        }
    }

    @Override
    public void onResume() {
        isDisplayed = true;

        super.onResume();
        final AssuranceFullScreenTakeover fullScreenTakeover =
                weakFullScreenTakeoverReference.get();

        // make sure we have a valid message before trying to proceed
        if (fullScreenTakeover == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover, could not get fullScreenTakeover object.");
            dismiss();
            return;
        }

        // Always use a weak reference to context outside of its lifecycle. Not doing so will cause
        // the context/activity to leak.
        fullScreenTakeover.messageFullScreenActivity = new WeakReference<>(this);

        // if we can't get root view, can't show the message
        try {
            final ViewGroup rootViewGroup = findViewById(android.R.id.content);

            if (rootViewGroup == null) {
                Log.error(
                        Assurance.LOG_TAG,
                        LOG_TAG,
                        "Failed to show fullscreen takeover, could not get root view group.");
                dismiss();
            } else {
                rootViewGroup.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                fullScreenTakeover.rootViewGroup = rootViewGroup;
                                fullScreenTakeover.showInRootViewGroup();
                            }
                        });
            }
        } catch (NullPointerException ex) {
            Log.error(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Failed to show fullscreen takeover: %s",
                    ex.getLocalizedMessage());
            dismiss();
        }
    }

    @Override
    public void onBackPressed() {
        final AssuranceFullScreenTakeover fullScreenTakeover =
                weakFullScreenTakeoverReference.get();

        if (fullScreenTakeover != null) {
            fullScreenTakeover.remove();
        }
    }

    @Override
    protected void onStop() {
        isDisplayed = false;
        super.onStop();
    }

    private void dismiss() {
        finish();
        overridePendingTransition(0, 0);
    }

    private void hideSystemUI() {
        this.getWindow()
                .getDecorView()
                .setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
}
