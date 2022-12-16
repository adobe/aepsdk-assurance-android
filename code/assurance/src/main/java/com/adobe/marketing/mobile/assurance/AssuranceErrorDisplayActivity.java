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


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.Log;

public class AssuranceErrorDisplayActivity extends AppCompatActivity {
    private static final String LOG_TAG = "AssuranceErrorDisplayActivity";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        hideSystemUI();

        setContentView(R.layout.activity_assurance_error_display);

        final TextView tvError = findViewById(R.id.tv_error);
        final TextView tvErrorDesc = findViewById(R.id.tv_error_desc);
        final Button btnDismiss = findViewById(R.id.btn_dismiss);

        if (btnDismiss != null) {
            btnDismiss.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            finish();
                        }
                    });
        } else {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to retrieve the instance of the dismiss button.");
        }

        final Intent intent = getIntent();

        if (intent == null) {
            Log.warning(
                    Assurance.LOG_TAG,
                    LOG_TAG,
                    "Unable to show AssuranceErrorDisplayActivity, Intent is null");
            return;
        }

        final String errorName =
                intent.getStringExtra(AssuranceConstants.IntentExtraKey.ERROR_NAME);
        final String errorDescription =
                intent.getStringExtra(AssuranceConstants.IntentExtraKey.ERROR_DESCRIPTION);

        if (errorName != null) {
            tvError.setText(errorName);
        }

        if (errorDescription != null) {
            tvErrorDesc.setText(errorDescription);
        }
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
