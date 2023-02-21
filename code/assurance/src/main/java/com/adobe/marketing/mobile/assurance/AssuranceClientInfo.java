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


import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class AssuranceClientInfo {

    private static final String VALUE_UNKNOWN = "Unknown";
    private static final String MANIFEST_FILE_NAME = "AndroidManifest.xml";
    private static final String EVENT_TYPE_CONNECT = "connect";

    /**
     * Returns the payload for assurance ClientInfo event. ClientInfo event includes
     *
     * <ol>
     *   <li>version - the current version of assurance SDK
     *   <li>deviceInfo - Information about the state of the device, battery status, screensize,
     *       device name and manufacturer etc.
     *   <li>appSettings - Applications Manifest.xml file parsed in JSON format
     *   <li>type = connect, representing that this event is initializing a socket connection with
     *       server
     * </ol>
     *
     * @return Returns {@link Map} representing clientInfo event payload
     */
    Map<String, Object> getData() {
        final Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put(AssuranceConstants.ClientInfoKeys.VERSION, Assurance.extensionVersion());
        eventPayload.put(AssuranceConstants.ClientInfoKeys.DEVICE_INFO, getDeviceInfo());
        eventPayload.put(AssuranceConstants.PayloadDataKeys.TYPE, EVENT_TYPE_CONNECT);
        eventPayload.put(
                AssuranceConstants.ClientInfoKeys.APP_SETTINGS,
                AssuranceIOUtils.parseXMLResourceFileToJson(MANIFEST_FILE_NAME));
        return eventPayload;
    }

    // ========================================================================================
    // Private methods
    // ========================================================================================

    /**
     * Retrives the information about the state of the device
     *
     * <ol>
     *   <li>Canonical platform name - Android , representing canonical platform name for the
     *       device.
     *   <li>Device Name - The end-user-visible name for the end product.
     *   <li>Device type - Could be either Watch, Phone or Tablet.
     *   <li>Device manufacturer - The manufacturer of the product/hardware.
     *   <li>Operating System
     *   <li>Battery level
     *   <li>Screen size
     *   <li>Location service enabled - Returns the current enabled/disabled state of location
     * </ol>
     *
     * @return Returns {@link Map} representing clientInfo event payload
     */
    private HashMap<String, Object> getDeviceInfo() {
        final HashMap<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.PLATFORM_NAME, "Android");
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.DEVICE_NAME, Build.MODEL);
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.DEVICE_TYPE, Build.DEVICE);
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.DEVICE_MANUFACTURER, Build.MANUFACTURER);
        deviceInfo.put(
                AssuranceConstants.DeviceInfoKeys.OPERATING_SYSTEM,
                "Android " + Build.VERSION.RELEASE);
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.CARRIER_NAME, getMobileCarrierName());
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL, getBatteryPercentage());
        deviceInfo.put(AssuranceConstants.DeviceInfoKeys.SCREEN_SIZE, getScreenSize());
        deviceInfo.put(
                AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED, isLocationEnabled());
        deviceInfo.put(
                AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS,
                getCurrentLocationPermission());
        deviceInfo.put(
                AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED,
                isPowerSaveModeEnabled());

        return deviceInfo;
    }

    /**
     * Retrieve a TelephonyManager for handling management the telephony features of the device.
     *
     * <p>Returns nil if unable to retrieve the carrier name.
     *
     * @return Returns {@link String} representing carrier name
     */
    private String getMobileCarrierName() {
        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            return VALUE_UNKNOWN;
        }

        final TelephonyManager telephonyManager =
                ((TelephonyManager) context.getSystemService(Application.TELEPHONY_SERVICE));
        return telephonyManager != null ? telephonyManager.getNetworkOperatorName() : VALUE_UNKNOWN;
    }

    /**
     * The current battery level for the device. This is integer value ranging from 1 to 100. If
     * unable to fetch battery value -1 is returned
     *
     * @return Returns {@code String} representing the deviceType
     */
    private int getBatteryPercentage() {
        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            return -1;
        }

        if (Build.VERSION.SDK_INT >= 21) {

            final BatteryManager bm =
                    (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            return bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

        } else {
            final IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            final Intent batteryStatus = context.registerReceiver(null, iFilter);

            final int level =
                    batteryStatus != null
                            ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                            : -1;
            final int scale =
                    batteryStatus != null
                            ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                            : -1;

            final double batteryPct = level / (double) scale;
            return (int) (batteryPct * 100);
        }
    }
    /**
     * Returns the screen size.
     *
     * @return A {@link String} representing screen size in <width>x<height> format
     */
    private String getScreenSize() {
        final int height = Resources.getSystem().getDisplayMetrics().heightPixels;
        final int width = Resources.getSystem().getDisplayMetrics().widthPixels;
        return String.format(Locale.US, "%dx%d", width, height);
    }

    /**
     * Returns the current enabled/disabled state of location.
     *
     * @return true if location is enabled and false if location is disabled
     */
    private Boolean isLocationEnabled() {
        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            final LocationManager lm =
                    (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return lm.isLocationEnabled();
        } else {
            final int mode =
                    Settings.Secure.getInt(
                            context.getContentResolver(),
                            Settings.Secure.LOCATION_MODE,
                            Settings.Secure.LOCATION_MODE_OFF);
            return (mode != Settings.Secure.LOCATION_MODE_OFF);
        }
    }

    /**
     * Tells the app’s authorization status for using location services. Possible values can be
     *
     * <ol>
     *   <li>Denied : The user denied the use of location services for the app/ Or the user has not
     *       been prompted yet to use location permission
     *   <li>When in use : The user authorized the app to start location services while it is in
     *       use.
     *   <li>Always : The user authorized the app to start location services at any time.
     *   <li>Unknown : If the location authorization status is not retrievable. A rare case.
     * </ol>
     *
     * @return A {@link String} representing the current location permission
     */
    private String getCurrentLocationPermission() {
        if (!isRuntimePermissionRequired()) {
            return "Always";
        }

        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            // Unable to check location permission, App context is not available. Defaulting
            // acquired permission level to unknown
            return VALUE_UNKNOWN;
        }

        final int permissionStatus =
                ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            // if the permission for accessing fine location is granted. one of the following is
            // true
            // 1. permission is granted to access location only `when app in use` (for API 29 and
            // above)
            // 2. permission is granted to access location in background

            // for android version below API 29, background location permission are granted by
            // default
            // for android version above API 29. verify if the access to background location is
            // granted specifically
            if (isBackgroundLocationAccessGrantedByDefault() || isBackgroundPermissionGranted()) {
                return "Always";
            } else {
                return "When in use";
            }

        } else if (permissionStatus == PackageManager.PERMISSION_DENIED) {
            return "Denied";
        } else {
            return "unknown";
        }
    }

    /** Helper method to verify if the device supports runtime permission. */
    private boolean isRuntimePermissionRequired() {
        return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M);
    }

    /**
     * Helper method to know if Background location permission is provided by default to the
     * application.
     */
    private boolean isBackgroundLocationAccessGrantedByDefault() {
        return (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q);
    }

    /**
     * Checks if permission to access fine location while the app is in background has been granted.
     *
     * <ol>
     *   <li>Returns true for the devices running on versions below Android M, Since Runtime
     *       permission not required.
     *   <li>Returns true if the permission for using fine location in background is already
     *       granted.
     *   <li>Returns false if the permission for using fine location in background is not granted or
     *       if the app context is null.
     * </ol>
     *
     * @return Returns {@code boolean} representing the permission to monitor fine location in
     *     background
     */
    private boolean isBackgroundPermissionGranted() {
        // for version below API 23, need not check permissions
        if (!isRuntimePermissionRequired()) {
            return true;
        }

        // get hold of the app context. bail out if null
        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            return false;
        }

        // verify the permission for fine location is granted
        final int permissionState =
                ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionState == PackageManager.PERMISSION_GRANTED) {

            // for android version below API 29, background location permission are granted by
            // default when fine location permission is granted
            // for android version above API 29, explicitly verify if the background location
            // permission is granted
            if (!isBackgroundLocationAccessGrantedByDefault()) {
                final int bgLocationPermissionState =
                        ActivityCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                return bgLocationPermissionState == PackageManager.PERMISSION_GRANTED;
            }

            return true;
        }

        return false;
    }

    /**
     * Returns true if the device is currently in power save mode.
     *
     * <p>When in this mode, applications should reduce their functionality in order to conserve
     * battery as much as possible The API to read PowerSaveMode was introduces in Android API
     * version 21. For Android API versions before 21, this method returns false.
     *
     * @return Returns {@code boolean} if power saving mode is enabled
     */
    private boolean isPowerSaveModeEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false;
        }

        final Context context =
                ServiceProvider.getInstance().getAppContextService().getApplicationContext();

        if (context == null) {
            return false;
        }

        final PowerManager powerManager =
                (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        if (powerManager == null) {
            return false;
        }

        return powerManager.isPowerSaveMode();
    }
}
