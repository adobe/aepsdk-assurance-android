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

package com.adobe.marketing.mobile.assurance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.telephony.TelephonyManager;
import androidx.core.app.ActivityCompat;
import com.adobe.marketing.mobile.Assurance;
import com.adobe.marketing.mobile.services.AppContextService;
import com.adobe.marketing.mobile.services.ServiceProvider;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class AssuranceClientInfoTest {
    @Mock private AppContextService mockAppContextService;

    @Mock private BatteryManager mockBatteryManager;

    @Mock private LocationManager mockLocationManager;

    @Mock private PowerManager mockPowerManager;

    @Mock private TelephonyManager mockTelephonyManager;

    @Mock private ServiceProvider mockServiceProvider;

    @Mock private Context mockAppContext;

    private AssuranceClientInfo assuranceClientInfo;

    private MockedStatic<AssuranceIOUtils> mockedStaticAssuranceIOUtils;
    private MockedStatic<ServiceProvider> mockedStaticServiceProvider;
    private MockedStatic<ActivityCompat> mockedStaticActivityCompat;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        assuranceClientInfo = new AssuranceClientInfo();
        mockedStaticActivityCompat = Mockito.mockStatic(ActivityCompat.class);
        mockedStaticAssuranceIOUtils = Mockito.mockStatic(AssuranceIOUtils.class);
        mockedStaticServiceProvider = Mockito.mockStatic(ServiceProvider.class);
    }

    @Test
    public void testGetData_happy() throws JSONException {
        final JSONObject appSettings = mockManifestData();
        mockedStaticAssuranceIOUtils
                .when(() -> AssuranceIOUtils.parseXMLResourceFileToJson(any()))
                .thenReturn(appSettings);

        mockAppContextService();
        mockTelephonyManager("MyNetworkCarrier");
        mockBatteryLevel(95);
        mockLocationManager(true, true);
        mockPowerManager(false);

        final Map<String, Object> data = assuranceClientInfo.getData();
        assertEquals(
                Assurance.EXTENSION_VERSION, data.get(AssuranceConstants.ClientInfoKeys.VERSION));
        assertEquals("connect", data.get(AssuranceConstants.PayloadDataKeys.TYPE));
        assertEquals(appSettings, data.get(AssuranceConstants.ClientInfoKeys.APP_SETTINGS));

        final Map<String, Object> obtainedDeviceInfo =
                (Map<String, Object>) data.get(AssuranceConstants.ClientInfoKeys.DEVICE_INFO);
        assertNotNull(obtainedDeviceInfo);
        assertEquals(
                "MyNetworkCarrier",
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.CARRIER_NAME));
        assertEquals(95, obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL));
        assertEquals(
                "Always",
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS));
        assertEquals(
                true,
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED));
        assertEquals(
                false,
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED));
    }

    @Test
    public void testGetData_locationAuthorizationDenied() throws JSONException {
        final JSONObject appSettings = mockManifestData();
        mockedStaticAssuranceIOUtils
                .when(() -> AssuranceIOUtils.parseXMLResourceFileToJson(any()))
                .thenReturn(appSettings);

        mockAppContextService();
        mockTelephonyManager("MyNetworkCarrier");
        mockBatteryLevel(95);
        mockLocationManager(false, false);
        mockPowerManager(false);

        final Map<String, Object> data = assuranceClientInfo.getData();
        assertEquals(
                Assurance.EXTENSION_VERSION, data.get(AssuranceConstants.ClientInfoKeys.VERSION));
        assertEquals("connect", data.get(AssuranceConstants.PayloadDataKeys.TYPE));
        assertEquals(appSettings, data.get(AssuranceConstants.ClientInfoKeys.APP_SETTINGS));

        final Map<String, Object> obtainedDeviceInfo =
                (Map<String, Object>) data.get(AssuranceConstants.ClientInfoKeys.DEVICE_INFO);
        assertNotNull(obtainedDeviceInfo);
        assertEquals(
                "MyNetworkCarrier",
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.CARRIER_NAME));
        assertEquals(95, obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL));
        assertEquals(
                "Denied",
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS));
        assertEquals(
                false,
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED));
        assertEquals(
                false,
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED));
    }

    @Test
    public void testGetData_lowPowerModeEnabled() throws JSONException {
        final JSONObject appSettings = mockManifestData();
        mockedStaticAssuranceIOUtils
                .when(() -> AssuranceIOUtils.parseXMLResourceFileToJson(any()))
                .thenReturn(appSettings);

        mockAppContextService();
        mockTelephonyManager("MyNetworkCarrier");
        mockBatteryLevel(95);
        mockLocationManager(false, false);
        mockPowerManager(true);

        final Map<String, Object> data = assuranceClientInfo.getData();
        assertEquals(
                Assurance.EXTENSION_VERSION, data.get(AssuranceConstants.ClientInfoKeys.VERSION));
        assertEquals("connect", data.get(AssuranceConstants.PayloadDataKeys.TYPE));
        assertEquals(appSettings, data.get(AssuranceConstants.ClientInfoKeys.APP_SETTINGS));

        final Map<String, Object> obtainedDeviceInfo =
                (Map<String, Object>) data.get(AssuranceConstants.ClientInfoKeys.DEVICE_INFO);
        assertNotNull(obtainedDeviceInfo);
        assertEquals(
                "MyNetworkCarrier",
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.CARRIER_NAME));
        assertEquals(95, obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL));
        assertEquals(
                "Denied",
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS));
        assertEquals(
                false,
                obtainedDeviceInfo.get(AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED));
        assertEquals(
                true,
                obtainedDeviceInfo.get(
                        AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED));
    }

    @After
    public void teardown() {
        mockedStaticServiceProvider.close();
        mockedStaticActivityCompat.close();
        mockedStaticAssuranceIOUtils.close();
    }

    private JSONObject mockManifestData() throws JSONException {
        final JSONObject appSettings = new JSONObject();
        final JSONObject manifest = new JSONObject();
        manifest.put("package", "com.assurance.testapp");
        appSettings.put("manifest", manifest);
        appSettings.put("versionCode", "1");
        appSettings.put("versionName", "1.0");
        return appSettings;
    }

    private void mockAppContextService() {
        mockedStaticServiceProvider
                .when(ServiceProvider::getInstance)
                .thenReturn(mockServiceProvider);
        when(mockServiceProvider.getAppContextService()).thenReturn(mockAppContextService);
        when(mockAppContextService.getApplicationContext()).thenReturn(mockAppContext);
    }

    private void mockTelephonyManager(final String carrierName) {
        when(mockAppContext.getSystemService(Application.TELEPHONY_SERVICE))
                .thenReturn(mockTelephonyManager);
        when(mockTelephonyManager.getNetworkOperatorName()).thenReturn(carrierName);
    }

    private void mockBatteryLevel(int level) {
        when(mockAppContext.getSystemService(Context.BATTERY_SERVICE))
                .thenReturn(mockBatteryManager);
        when(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
                .thenReturn(level);
    }

    private void mockLocationManager(
            final boolean locationEnabled, final boolean accessFineLocation) {
        when(mockAppContext.getSystemService(Context.LOCATION_SERVICE))
                .thenReturn(mockLocationManager);
        when(mockLocationManager.isLocationEnabled()).thenReturn(locationEnabled);

        mockedStaticActivityCompat
                .when(
                        () ->
                                ActivityCompat.checkSelfPermission(
                                        mockAppContext, Manifest.permission.ACCESS_FINE_LOCATION))
                .thenReturn(
                        accessFineLocation
                                ? PackageManager.PERMISSION_GRANTED
                                : PackageManager.PERMISSION_DENIED);
    }

    private void mockPowerManager(boolean isPowerSaveModeEnabled) {
        when(mockAppContext.getSystemService(Context.POWER_SERVICE)).thenReturn(mockPowerManager);
        when(mockPowerManager.isPowerSaveMode()).thenReturn(isPowerSaveModeEnabled);
    }
}
