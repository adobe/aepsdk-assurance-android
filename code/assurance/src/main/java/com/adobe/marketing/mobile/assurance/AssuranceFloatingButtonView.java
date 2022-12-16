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


import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import androidx.core.content.ContextCompat;

@SuppressWarnings({"AppCompatCustomView", "unused"})
class AssuranceFloatingButtonView extends Button implements View.OnTouchListener {
    private float oldY;
    private float maxButtonTouch;
    private static final float BUTTON_MOVEMENT_TOLERANCE = 10;
    private OnPositionChangedListener onPositionChangedListener;

    public enum Graphic {
        CONNECTED,
        DISCONNECTED
    }

    // This tag will be used to identify the floating button in the root view.
    // This means that only one button per activity is supported as of now.
    static final String VIEW_TAG = "AssuranceFloatingButtonTag";

    /** An interface to receive position changed callbacks for the button */
    interface OnPositionChangedListener {
        /**
         * Will be called whenever the button is moved to a new location on screen.
         *
         * @param newX The new x co-ordinate
         * @param newY The new y co-ordinate
         */
        void onPositionChanged(float newX, float newY);
    }

    public AssuranceFloatingButtonView(Context context) {
        super(context);
        setOnTouchListener(this);
        setTag(VIEW_TAG);
    }

    public AssuranceFloatingButtonView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AssuranceFloatingButtonView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setGraphic(final Graphic graphic) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            this.setBackground(
                    graphic == Graphic.CONNECTED
                            ? ContextCompat.getDrawable(
                                    this.getContext(), R.drawable.ic_assurance_active)
                            : ContextCompat.getDrawable(
                                    this.getContext(), R.drawable.ic_assurance_inactive));
        }
    }

    public void setOnPositionChangedListener(
            final OnPositionChangedListener onPositionChangedListener) {
        this.onPositionChangedListener = onPositionChangedListener;
    }

    public void setPosition(final float x, final float y) {
        setX(x);
        setY(y);

        if (this.onPositionChangedListener != null) {
            this.onPositionChangedListener.onPositionChanged(x, y);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent me) {
        if (me.getAction() == MotionEvent.ACTION_UP) {
            if (maxButtonTouch
                    < BUTTON_MOVEMENT_TOLERANCE) { // perform click if up>down action didn't move
                // button.
                performClick();
            }
        } else if (me.getAction() == MotionEvent.ACTION_DOWN) { // prepare for move if drag occurs
            maxButtonTouch = 0.0f;
            oldY = me.getRawY();
        } else if (me.getAction() == MotionEvent.ACTION_MOVE) { // execute move
            final float curY = me.getRawY();
            setPosition(v.getRootView().getWidth() - getWidth(), curY - (getHeight() / 2.0f));
            float displacement = Math.abs(curY - oldY);

            if (displacement > maxButtonTouch) {
                maxButtonTouch = displacement;
            }
        }

        return true;
    }
}
