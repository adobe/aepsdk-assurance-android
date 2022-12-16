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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"AppCompatCustomView", "unused"})
class AssuranceFloatingButton implements AssuranceSessionLifecycleListener {
    private static final String LOG_TAG = "AssuranceFloatingButton";
    private static final float BUTTON_SIZE = 80.0f;

    private float lastKnownXPos, lastKnownYPos;
    private boolean buttonDisplayEnabled;
    private AssuranceFloatingButtonView.Graphic currentGraphic;

    private Map<String, AssuranceFloatingButtonView> managedButtonViews = new ConcurrentHashMap<>();
    private final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle;
    private final View.OnClickListener onClickListener;

    AssuranceFloatingButton(
            final AssuranceSessionOrchestrator.ApplicationHandle applicationHandle,
            final View.OnClickListener listener) {
        this.applicationHandle = applicationHandle;
        buttonDisplayEnabled = false;
        this.onClickListener = listener;

        currentGraphic = AssuranceFloatingButtonView.Graphic.DISCONNECTED;
    }

    void display() {
        buttonDisplayEnabled = true;
        manageButtonDisplayForActivity(applicationHandle.getCurrentActivity());
    }

    void setCurrentGraphic(final AssuranceFloatingButtonView.Graphic graphic) {
        if (currentGraphic != graphic) {
            currentGraphic = graphic;
            manageButtonDisplayForActivity(applicationHandle.getCurrentActivity());
        }
    }

    public AssuranceFloatingButtonView.Graphic getCurrentGraphic() {
        return currentGraphic;
    }

    /**
     * Display an instance of the {@code FloatingButtonView} on the {@code currentActivity}
     * supplied.
     *
     * <p>An instance of {@link AssuranceFloatingButtonView} needs to be already instantiated and
     * managed by this manager before this method can be used to display the button on the activity.
     * This method also checks to see if there is already a button displaying on the activity, and
     * if so, will adjust the position as per the co-ordinates supplied. if the button does not
     * exist, then it will be created and displayed.
     *
     * @param x The x co-ordinate where the button will be displayed
     * @param y The y co-ordinate where the button will be displayed
     */
    private void display(final float x, final float y, final Activity activity) {
        // Make sure we don't overlay a assurance ui view with the floating button... hilarity will
        // ensue.
        if (activity instanceof AssuranceFullScreenTakeoverActivity) {
            Log.trace(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Skipping FloatingButton Overlay due to Assurance view presentation.");
            return;
        }

        // We will use the absolute width and height later if the root view has not been measured by
        // then.
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        final String activityClassName = activity.getLocalClassName();
                        final DisplayMetrics displayMetrics = new DisplayMetrics();
                        activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        final int absHeightPx = displayMetrics.heightPixels;
                        final int absWidthPx = displayMetrics.widthPixels;
                        final ViewGroup rootViewGroup =
                                (ViewGroup) activity.getWindow().getDecorView().getRootView();

                        final int width =
                                rootViewGroup.getMeasuredWidth() == 0
                                        ? absWidthPx
                                        : rootViewGroup.getMeasuredWidth();
                        final int height =
                                rootViewGroup.getMeasuredHeight() == 0
                                        ? absHeightPx
                                        : rootViewGroup.getMeasuredHeight();

                        AssuranceFloatingButtonView floatingButtonView =
                                rootViewGroup.findViewWithTag(AssuranceFloatingButtonView.VIEW_TAG);

                        if (floatingButtonView != null) {
                            // The button already exists as a child of the root
                            // Adjust x and y to account for orientation change.
                            lastKnownXPos = adjustXBounds(floatingButtonView, width, x);
                            lastKnownYPos = adjustYBounds(floatingButtonView, height, y);
                            floatingButtonView.setGraphic(currentGraphic);
                            floatingButtonView.setVisibility(buttonDisplayEnabled ? VISIBLE : GONE);
                            floatingButtonView.setPosition(lastKnownXPos, lastKnownYPos);
                            return;
                        } else {
                            floatingButtonView = managedButtonViews.get(activityClassName);
                        }

                        if (floatingButtonView == null) {
                            Log.error(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Unable to create floating button for activity `%s`",
                                    activityClassName);
                            return;
                        }

                        floatingButtonView.setGraphic(currentGraphic);
                        floatingButtonView.setVisibility(buttonDisplayEnabled ? VISIBLE : GONE);
                        floatingButtonView.setOnPositionChangedListener(
                                new AssuranceFloatingButtonView.OnPositionChangedListener() {
                                    @Override
                                    public void onPositionChanged(float newX, float newY) {
                                        lastKnownXPos = newX;
                                        lastKnownYPos = newY;
                                    }
                                });

                        final ViewTreeObserver viewTreeObserver =
                                floatingButtonView.getViewTreeObserver();
                        final AssuranceFloatingButtonView buttonViewForLayoutListener =
                                floatingButtonView;
                        ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener =
                                new ViewTreeObserver.OnGlobalLayoutListener() {
                                    @Override
                                    public void onGlobalLayout() {
                                        removeOnGlobalLayoutListenerCompat(
                                                buttonViewForLayoutListener, this);

                                        if (x >= 0 && y >= 0) {
                                            // Adjust x and y to account for orientation change.
                                            lastKnownXPos =
                                                    adjustXBounds(
                                                            buttonViewForLayoutListener, width, x);
                                            lastKnownYPos =
                                                    adjustYBounds(
                                                            buttonViewForLayoutListener, height, y);
                                        } else {
                                            lastKnownXPos =
                                                    width - buttonViewForLayoutListener.getWidth();
                                            lastKnownYPos = 0.0f;
                                        }

                                        buttonViewForLayoutListener.setPosition(
                                                lastKnownXPos, lastKnownYPos);
                                    }
                                };

                        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLayoutListener);

                        // handle any runtime exceptions that might occur when adding the view.
                        try {
                            rootViewGroup.addView(floatingButtonView);
                        } catch (final Exception ex) {
                            Log.trace(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "Failed to add floating button view: Error - %s",
                                    ex.getLocalizedMessage());
                        }

                        ViewGroup.LayoutParams layoutParams = floatingButtonView.getLayoutParams();

                        if (layoutParams != null) {
                            layoutParams.width =
                                    layoutParams.height =
                                            Math.round(displayMetrics.density * BUTTON_SIZE);
                            floatingButtonView.setLayoutParams(layoutParams);
                            floatingButtonView.setPosition(lastKnownXPos, lastKnownYPos);
                        }
                    }
                });
    }

    void remove() {
        Log.trace(Assurance.LOG_TAG, LOG_TAG, "Removing the floating button.");
        final Activity activity = applicationHandle.getCurrentActivity();

        if (activity != null) {
            removeFloatingButtonFromActivity(activity);
        }

        buttonDisplayEnabled = false;
    }

    private void removeFloatingButtonFromActivity(final Activity activity) {
        Log.trace(Assurance.LOG_TAG, LOG_TAG, "Removing the floating button for " + activity);

        if (activity == null) {
            Log.error(
                    Assurance.LOG_TAG, LOG_TAG, "Cannot remove floating button, activity is null.");
            return;
        }

        final String activityClassName = activity.getLocalClassName();
        activity.runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {
                        final ViewGroup rootViewGroup =
                                (ViewGroup) activity.getWindow().getDecorView().getRootView();
                        AssuranceFloatingButtonView floatingButton =
                                rootViewGroup.findViewWithTag(AssuranceFloatingButtonView.VIEW_TAG);

                        if (floatingButton != null) {
                            // Always reset listeners assigned to the Views. Not doing so will cause
                            // a leak, especially
                            // in this case where the listeners hold a reference to the context
                            // indirectly.
                            floatingButton.setOnPositionChangedListener(null);
                            floatingButton.setOnClickListener(null);
                            floatingButton.setVisibility(GONE);
                        } else {
                            Log.debug(
                                    Assurance.LOG_TAG,
                                    LOG_TAG,
                                    "No floating button found for removal on activity `%s`",
                                    activityClassName);
                        }
                    }
                });
        managedButtonViews.remove(activityClassName);
    }

    /**
     * Adjust the x co-ordinate so that it remains within the screen width.
     *
     * @param floatingButtonView The button for which the position needs to be adjusted
     * @param screenWidth The screen width in pixels
     * @param oldX The x co-ordinate which needs to be adjusted
     * @return The adjusted x co-ordinate
     */
    private float adjustXBounds(
            final AssuranceFloatingButtonView floatingButtonView,
            final float screenWidth,
            final float oldX) {
        //        if (floatingButtonView != null && oldX > (screenWidth -
        // floatingButtonView.getWidth())) {
        return (screenWidth - floatingButtonView.getWidth());
        //        }
        //
        //        return oldX;
    }

    /**
     * Adjust the y co-ordinate so that it remains within the screen height.
     *
     * @param floatingButtonView The button for which the position needs to be adjusted
     * @param screenHeight The screen height in pixels
     * @param oldY The y co-ordinate which needs to be adjusted
     * @return The adjusted y co-ordinate
     */
    private float adjustYBounds(
            final AssuranceFloatingButtonView floatingButtonView,
            final float screenHeight,
            final float oldY) {
        if (floatingButtonView != null && oldY > (screenHeight - floatingButtonView.getHeight())) {
            return (screenHeight - floatingButtonView.getHeight());
        }

        return oldY;
    }

    /**
     * Removes the {@code OnGlobalLayoutListener} registered earlier.
     *
     * <p>The {@code onGlobalLayoutListener} instance should be something that was registered
     * earlier. For example, {@link #display(float, float, Activity)} method registers a listener to
     * position the button on the view tree, and then once done, uses this method to de-register the
     * listener.
     *
     * @param floatingButtonView The {@link AssuranceFloatingButtonView} instance which is used to
     *     retrieve the {@link ViewTreeObserver}
     * @param onGlobalLayoutListener The {@link ViewTreeObserver.OnGlobalLayoutListener} instance
     */
    private void removeOnGlobalLayoutListenerCompat(
            AssuranceFloatingButtonView floatingButtonView,
            ViewTreeObserver.OnGlobalLayoutListener onGlobalLayoutListener) {
        ViewTreeObserver viewTreeObserver = floatingButtonView.getViewTreeObserver();

        if (Build.VERSION.SDK_INT >= 16) {
            viewTreeObserver.removeOnGlobalLayoutListener(onGlobalLayoutListener);
        } else {
            viewTreeObserver.removeGlobalOnLayoutListener(onGlobalLayoutListener);
        }
    }

    private void manageButtonDisplayForActivity(final Activity activity) {
        if (activity == null) {
            Log.debug(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    LOG_TAG,
                    "[manageButtonDisplayForActivity] activity is null");
            return;
        }

        final String activityClassName = activity.getLocalClassName();

        // if a floating button should no longer be displayed,
        // and this activity has a button showing, then remove it
        if (!buttonDisplayEnabled) {
            if (managedButtonViews.containsKey(activityClassName)) {
                // This activity has a managedButton that needs to be now removed
                removeFloatingButtonFromActivity(activity);
            }
        } else {
            // Show the button (create new if does not exist for this activity)
            if (managedButtonViews.get(activityClassName) == null
                    && !AssuranceFullScreenTakeoverActivity.class
                            .getSimpleName()
                            .equalsIgnoreCase(activity.getClass().getSimpleName())) {
                // We do not have an existing button showing, create one
                Log.trace(Assurance.LOG_TAG, LOG_TAG, "Creating floating button for " + activity);
                final AssuranceFloatingButtonView newButton =
                        new AssuranceFloatingButtonView(activity);
                managedButtonViews.put(activityClassName, newButton);
                newButton.setOnClickListener(onClickListener);
            }

            display(lastKnownXPos, lastKnownYPos, activity);
        }
    }

    public void onActivityResumed(Activity activity) {
        manageButtonDisplayForActivity(activity);
    }

    public void onActivityDestroyed(Activity activity) {
        managedButtonViews.remove(activity.getLocalClassName());
    }
}
