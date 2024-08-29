/*
  Copyright 2024 Adobe. All rights reserved.
  This file is licensed to you under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License. You may obtain a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software distributed under
  the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
  OF ANY KIND, either express or implied. See the License for the specific language
  governing permissions and limitations under the License.
*/

package com.adobe.marketing.mobile.assurance.internal

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.BatteryManager
import android.os.PowerManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import com.adobe.marketing.mobile.Assurance
import com.adobe.marketing.mobile.services.AppContextService
import com.adobe.marketing.mobile.services.ServiceProvider
import com.adobe.marketing.mobile.util.JSONUtils
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AssuranceClientInfoTest {
    @Mock
    private lateinit var mockAppContextService: AppContextService

    @Mock
    private lateinit var mockBatteryManager: BatteryManager

    @Mock
    private lateinit var mockLocationManager: LocationManager

    @Mock
    private lateinit var mockPowerManager: PowerManager

    @Mock
    private lateinit var mockTelephonyManager: TelephonyManager

    @Mock
    private lateinit var mockServiceProvider: ServiceProvider

    @Mock
    private lateinit var mockAppContext: Context

    @Mock
    private lateinit var mockApp: Application

    private lateinit var mockedStaticAssuranceIOUtils: MockedStatic<AssuranceIOUtils>
    private lateinit var mockedStaticServiceProvider: MockedStatic<ServiceProvider>
    private lateinit var mockedStaticActivityCompat: MockedStatic<ActivityCompat>

    companion object {
        private const val TEST_APP_PACKAGE_NAME = "com.assurance.testapp"
        private const val TEST_APP_NAME = "TestApp"
        private const val TEST_BATTERY_LEVEL = 95
        private const val TEST_NETWORK_CARRIER = "MyNetworkCarrier"
        private const val TEST_LOCATION_AUTHORIZATION_STATUS_ALWAYS = "Always"
        private const val TEST_LOCATION_AUTHORIZATION_STATUS_DENIED = "Denied"
        private const val SOME_INCORRECT_PACKAGE_NAME = "com.corrupt.testapp"
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        mockedStaticActivityCompat = Mockito.mockStatic(ActivityCompat::class.java)
        mockedStaticAssuranceIOUtils = Mockito.mockStatic(AssuranceIOUtils::class.java)
        mockedStaticServiceProvider = Mockito.mockStatic(
            ServiceProvider::class.java
        )
    }

    @Test
    fun `Test getData with all valid values`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockManifestData(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        mockTelephonyManager(TEST_NETWORK_CARRIER)
        mockBatteryLevel(TEST_BATTERY_LEVEL)
        mockLocationManager(true, true)
        mockPowerManager(false)

        val assuranceClientInfo = AssuranceClientInfo()
        val data = assuranceClientInfo.data

        // Check "version" key
        Assert.assertEquals(
            Assurance.EXTENSION_VERSION,
            data[AssuranceConstants.ClientInfoKeys.VERSION]
        )

        // Check "type" key
        Assert.assertEquals("connect", data[AssuranceConstants.PayloadDataKeys.TYPE])

        // Check "appSettings" object
        val expectedAppSettings = createManifestJson(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        Assert.assertEquals(
            JSONUtils.toMap(expectedAppSettings),
            JSONUtils.toMap(data[AssuranceConstants.ClientInfoKeys.APP_SETTINGS] as JSONObject)
        )

        // Check "deviceInfo" object
        val obtainedDeviceInfo =
            data[AssuranceConstants.ClientInfoKeys.DEVICE_INFO] as Map<String, Any>?
        Assert.assertNotNull(obtainedDeviceInfo)
        Assert.assertEquals(
            TEST_NETWORK_CARRIER,
            obtainedDeviceInfo!![AssuranceConstants.DeviceInfoKeys.CARRIER_NAME]
        )
        Assert.assertEquals(95, obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL])
        Assert.assertEquals(
            TEST_LOCATION_AUTHORIZATION_STATUS_ALWAYS,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS]
        )
        Assert.assertEquals(
            true,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED]
        )
        Assert.assertEquals(
            false,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED]
        )
    }

    @Test
    fun `Test #getData when the parsed manifest is not the desired app manifest`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockManifestData(SOME_INCORRECT_PACKAGE_NAME, TEST_APP_NAME)
        mockTelephonyManager(TEST_NETWORK_CARRIER)
        mockBatteryLevel(TEST_BATTERY_LEVEL)
        mockLocationManager(true, true)
        mockPowerManager(false)
        mockAppDetails(
            TEST_APP_PACKAGE_NAME,
            TEST_APP_NAME,
            1,
            "1.0"
        )

        val assuranceClientInfo = AssuranceClientInfo()
        val data = assuranceClientInfo.data

        // Check version key
        Assert.assertEquals(
            Assurance.EXTENSION_VERSION, data[AssuranceConstants.ClientInfoKeys.VERSION]
        )

        // Check type key
        Assert.assertEquals("connect", data[AssuranceConstants.PayloadDataKeys.TYPE])

        // Check app settings object
        val expectedAppSettings = createManifestJson(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        assertEquals(
            JSONUtils.toMap(expectedAppSettings),
            JSONUtils.toMap(data[AssuranceConstants.ClientInfoKeys.APP_SETTINGS] as JSONObject)
        )

        // Check device info object
        val obtainedDeviceInfo =
            data[AssuranceConstants.ClientInfoKeys.DEVICE_INFO] as Map<String, Any>?
        Assert.assertNotNull(obtainedDeviceInfo)
        Assert.assertEquals(
            TEST_NETWORK_CARRIER,
            obtainedDeviceInfo!![AssuranceConstants.DeviceInfoKeys.CARRIER_NAME]
        )
        Assert.assertEquals(
            TEST_BATTERY_LEVEL,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL]
        )
        Assert.assertEquals(
            TEST_LOCATION_AUTHORIZATION_STATUS_ALWAYS,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS]
        )
        Assert.assertEquals(
            true,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED]
        )
        Assert.assertEquals(
            false,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED]
        )
    }

    @Test
    fun `Test #getData when Location permissions are denied`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockManifestData(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        mockTelephonyManager(TEST_NETWORK_CARRIER)
        mockBatteryLevel(TEST_BATTERY_LEVEL)
        mockLocationManager(false, false)
        mockPowerManager(false)

        val assuranceClientInfo = AssuranceClientInfo()
        val data = assuranceClientInfo.data

        // Check "version" key
        Assert.assertEquals(
            Assurance.EXTENSION_VERSION, data[AssuranceConstants.ClientInfoKeys.VERSION]
        )

        // Check "type" key
        Assert.assertEquals("connect", data[AssuranceConstants.PayloadDataKeys.TYPE])

        // Check "appSettings" object
        val expectedAppSettings = createManifestJson(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        Assert.assertEquals(
            JSONUtils.toMap(expectedAppSettings),
            JSONUtils.toMap(data[AssuranceConstants.ClientInfoKeys.APP_SETTINGS] as JSONObject)
        )

        // Check "deviceInfo" object
        val obtainedDeviceInfo =
            data[AssuranceConstants.ClientInfoKeys.DEVICE_INFO] as Map<String, Any>?
        Assert.assertNotNull(obtainedDeviceInfo)
        Assert.assertEquals(
            TEST_NETWORK_CARRIER,
            obtainedDeviceInfo!![AssuranceConstants.DeviceInfoKeys.CARRIER_NAME]
        )
        Assert.assertEquals(
            TEST_BATTERY_LEVEL,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL]
        )
        Assert.assertEquals(
            TEST_LOCATION_AUTHORIZATION_STATUS_DENIED,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS]
        )
        Assert.assertEquals(
            false,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED]
        )
        Assert.assertEquals(
            false,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED]
        )
    }

    @Test
    fun `Test #getData when Low Power mode is enabled`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockManifestData(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        mockTelephonyManager(TEST_NETWORK_CARRIER)
        mockBatteryLevel(TEST_BATTERY_LEVEL)
        mockLocationManager(false, false)
        mockPowerManager(true)

        val assuranceClientInfo = AssuranceClientInfo()
        val data = assuranceClientInfo.data

        // Check "version" key
        Assert.assertEquals(
            Assurance.EXTENSION_VERSION, data[AssuranceConstants.ClientInfoKeys.VERSION]
        )

        // Check "type" key
        Assert.assertEquals("connect", data[AssuranceConstants.PayloadDataKeys.TYPE])

        // Check "appSettings" object
        val expectedAppSettings = createManifestJson(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        Assert.assertEquals(
            JSONUtils.toMap(expectedAppSettings),
            JSONUtils.toMap(data[AssuranceConstants.ClientInfoKeys.APP_SETTINGS] as JSONObject)
        )

        // Check "deviceInfo" object
        val obtainedDeviceInfo =
            data[AssuranceConstants.ClientInfoKeys.DEVICE_INFO] as Map<String, Any>?
        Assert.assertNotNull(obtainedDeviceInfo)
        Assert.assertEquals(
            TEST_NETWORK_CARRIER,
            obtainedDeviceInfo!![AssuranceConstants.DeviceInfoKeys.CARRIER_NAME]
        )
        Assert.assertEquals(
            TEST_BATTERY_LEVEL,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.BATTERY_LEVEL]
        )
        Assert.assertEquals(
            TEST_LOCATION_AUTHORIZATION_STATUS_DENIED,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_AUTHORIZATION_STATUS]
        )
        Assert.assertEquals(
            false,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOCATION_SERVICE_ENABLED]
        )
        Assert.assertEquals(
            true,
            obtainedDeviceInfo[AssuranceConstants.DeviceInfoKeys.LOW_POWER_BATTERY_ENABLED]
        )
    }

    @Test
    fun `Test #getFallbackManifestData when app is null`() {
        mockedStaticServiceProvider
            .`when`<Any?> { ServiceProvider.getInstance() }
            .thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.appContextService).thenReturn(mockAppContextService)
        `when`(mockAppContextService.applicationContext).thenReturn(mockAppContext)
        `when`(mockAppContextService.application).thenReturn(null)

        val assuranceClientInfo = AssuranceClientInfo()
        val fallbackManifestData = assuranceClientInfo.getFallbackManifestData()
        assertEquals(0, fallbackManifestData.length())
    }

    @Test
    fun `Test #getFallbackManifestData when package info throws exception`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(
            TEST_APP_PACKAGE_NAME,
            TEST_APP_NAME,
            1,
            "1.0"
        )

        // Simulate valid app info
        val mockAppInfo = ApplicationInfo()
        mockAppInfo.name = TEST_APP_NAME
        `when`(mockApp.getApplicationInfo()).thenReturn(mockAppInfo)

        // Simulate package manager throwing exception
        val mockPackageManager = Mockito.mock(PackageManager::class.java)
        `when`(mockAppContext.packageManager).thenReturn(mockPackageManager)
        `when`(
            mockPackageManager.getPackageInfo(
                ArgumentMatchers.eq(TEST_APP_PACKAGE_NAME),
                ArgumentMatchers.anyInt()
            )
        ).thenThrow(PackageManager.NameNotFoundException())

        val assuranceClientInfo = AssuranceClientInfo()
        val fallbackManifestData = assuranceClientInfo.getFallbackManifestData()

        val expectedFallbackManifestData = JSONObject(
            """
            {
                "manifest": {
                    "package": "$TEST_APP_PACKAGE_NAME",
                    "application": {
                        "name": "$TEST_APP_NAME"
                    }
                }
            }
            """.trimIndent()
        )

        assertEquals(JSONUtils.toMap(expectedFallbackManifestData), JSONUtils.toMap(fallbackManifestData))
    }

    @Test
    fun `Test #getFallbackManifestData when app info is null`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)

        // Simulate valid package info
        val mockPackageManager = Mockito.mock(PackageManager::class.java)
        `when`(mockAppContext.packageManager).thenReturn(mockPackageManager)
        val mockPackageInfo = Mockito.mock(
            PackageInfo::class.java
        )
        mockPackageInfo.versionCode = 1
        mockPackageInfo.versionName = "1.0"
        `when`(
            mockPackageManager.getPackageInfo(
                ArgumentMatchers.eq(TEST_APP_PACKAGE_NAME),
                ArgumentMatchers.anyInt()
            )
        ).thenReturn(mockPackageInfo)

        // Simulate null app info
        `when`(mockApp.applicationInfo).thenReturn(null)

        val assuranceClientInfo = AssuranceClientInfo()
        val fallbackManifestData = assuranceClientInfo.getFallbackManifestData()

        val expectedFallbackManifestData = JSONObject(
            """
            {
                "manifest": {
                    "package": "$TEST_APP_PACKAGE_NAME",
                    "versionCode": "1",
                    "versionName": "1.0"
                }
            }
            """.trimIndent()
        )

        assertEquals(JSONUtils.toMap(expectedFallbackManifestData), JSONUtils.toMap(fallbackManifestData))
    }

    @Test
    fun `Test #getFallbackManifestData when all data is available`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(
            TEST_APP_PACKAGE_NAME,
            TEST_APP_NAME,
            1,
            "1.0"
        )
        val assuranceClientInfo = AssuranceClientInfo()
        val fallbackManifestData = assuranceClientInfo.getFallbackManifestData()
        val expectedFallbackManifestData = createManifestJson(TEST_APP_PACKAGE_NAME, TEST_APP_NAME)
        assertEquals(JSONUtils.toMap(expectedFallbackManifestData), JSONUtils.toMap(fallbackManifestData))
    }

    @Test
    fun `Test #validateManifestData when manifest data is null`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, 1, "1.0")

        val assuranceClientInfo = AssuranceClientInfo()
        val result = assuranceClientInfo.validateManifestData(null)

        assertEquals(false, result)
    }

    @Test
    fun `Test #validateManifestData when manifest data is empty`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, 1, "1.0")

        val assuranceClientInfo = AssuranceClientInfo()
        val result = assuranceClientInfo.validateManifestData(JSONObject())

        assertEquals(false, result)
    }

    @Test
    fun `Test #validateManifestData when manifest data is missing package name`() {
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, 1, "1.0")

        val manifestData = JSONObject(
            """
            {
                "manifest": {
                    "application": {
                        "name": "$TEST_APP_NAME"
                    }
                }
            }
            """.trimIndent()
        )
        val assuranceClientInfo = AssuranceClientInfo()
        val result = assuranceClientInfo.validateManifestData(manifestData)
        assertEquals(false, result)
    }

    @Test
    fun `Test #validateManifestData when packake when manifest package does not match app package`() {
        val manifestData = JSONObject(
            """
            {
                "manifest": {
                    "package": "$SOME_INCORRECT_PACKAGE_NAME",
                    "application": {
                        "name": "TestApp"
                    }
                }
            }
            """.trimIndent()
        )
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, 1, "1.0")

        val assuranceClientInfo = AssuranceClientInfo()
        val result = assuranceClientInfo.validateManifestData(manifestData)
        assertEquals(false, result)
    }

    @Test
    fun `Test #validateManifestData when manifest data is valid`() {
        val manifestData = JSONObject(
            """
            {
                "manifest": {
                    "package": "$TEST_APP_PACKAGE_NAME",
                    "application": {
                        "name": "$TEST_APP_NAME"
                    }
                }
            }
            """.trimIndent()
        )
        mockAppContextService(TEST_APP_PACKAGE_NAME)
        mockAppDetails(TEST_APP_PACKAGE_NAME, TEST_APP_NAME, 1, "1.0")

        val assuranceClientInfo = AssuranceClientInfo()
        val result = assuranceClientInfo.validateManifestData(manifestData)
        assertEquals(true, result)
    }

    @After
    fun teardown() {
        mockedStaticServiceProvider.close()
        mockedStaticActivityCompat.close()
        mockedStaticAssuranceIOUtils.close()
    }

    private fun mockManifestData(packageName: String, appName: String): JSONObject {
        val appSettings = createManifestJson(packageName, appName)
        mockedStaticAssuranceIOUtils.`when`<Any> { AssuranceIOUtils.parseXMLResourceFileToJson(any()) }
            ?.thenReturn(appSettings)
        return appSettings
    }

    /**
     * Creates a JSON object representing the manifest file.
     * @param packageName the package name of the app
     * @param appName the name of the app
     */
    private fun createManifestJson(packageName: String, appName: String) = JSONObject(
        """
            {
                "manifest": {
                    "package": "$packageName",
                    "versionCode": "1",
                    "versionName": "1.0",
                    "application": {
                        "name": "$appName"
                    }
                }
            }
        """.trimIndent()
    )

    private fun mockAppContextService(appPackage: String) {
        mockedStaticServiceProvider
            .`when`<Any?> { ServiceProvider.getInstance() }
            .thenReturn(mockServiceProvider)
        `when`(mockServiceProvider.appContextService).thenReturn(mockAppContextService)
        `when`(mockAppContextService.applicationContext).thenReturn(mockAppContext)

        `when`(mockAppContextService.application).thenReturn(mockApp)
        `when`(mockApp.applicationContext).thenReturn(mockAppContext)
        `when`(mockApp.packageName).thenReturn(appPackage)
    }

    private fun mockAppDetails(
        packageName: String,
        appName: String,
        versionCode: Int,
        versionName: String
    ) {
        val mockPackageManager = Mockito.mock(PackageManager::class.java)
        `when`(mockAppContext.packageManager).thenReturn(mockPackageManager)
        val mockPackageInfo = Mockito.mock(
            PackageInfo::class.java
        )
        mockPackageInfo.versionCode = versionCode
        mockPackageInfo.versionName = versionName
        `when`(
            mockPackageManager.getPackageInfo(
                ArgumentMatchers.eq(packageName),
                ArgumentMatchers.anyInt()
            )
        ).thenReturn(mockPackageInfo)

        val mockAppInfo = ApplicationInfo()
        mockAppInfo.name = appName
        `when`(mockApp.getApplicationInfo()).thenReturn(mockAppInfo)
    }

    private fun mockTelephonyManager(carrierName: String) {
        `when`(mockAppContext.getSystemService(Application.TELEPHONY_SERVICE))
            .thenReturn(mockTelephonyManager)
        `when`(mockTelephonyManager.networkOperatorName).thenReturn(carrierName)
    }

    private fun mockBatteryLevel(level: Int) {
        `when`(mockAppContext.getSystemService(Context.BATTERY_SERVICE))
            .thenReturn(mockBatteryManager)
        `when`(mockBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            .thenReturn(level)
    }

    private fun mockLocationManager(
        locationEnabled: Boolean,
        accessFineLocation: Boolean
    ) {
        `when`(mockAppContext.getSystemService(Context.LOCATION_SERVICE))
            .thenReturn(mockLocationManager)
        `when`(mockLocationManager.isLocationEnabled).thenReturn(locationEnabled)

        mockedStaticActivityCompat
            .`when`<Any> {
                ActivityCompat.checkSelfPermission(
                    mockAppContext, Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
            ?.thenReturn(
                if (accessFineLocation
                ) PackageManager.PERMISSION_GRANTED
                else PackageManager.PERMISSION_DENIED
            )
    }

    private fun mockPowerManager(isPowerSaveModeEnabled: Boolean) {
        `when`(mockAppContext.getSystemService(Context.POWER_SERVICE))
            .thenReturn(mockPowerManager)
        `when`(mockPowerManager.isPowerSaveMode).thenReturn(isPowerSaveModeEnabled)
    }
}
