# Getting Started with Assurance SDK

## Before starting

Assurance extension has a dependency on [AEP Core SDK](https://github.com/adobe/aepsdk-core-ios#readme) which must be installed to use the extension.


## Add Assurance extension to your app

1. Installation via [Maven](https://maven.apache.org/) & [Gradle](https://gradle.org/) is the easiest and recommended way to get the AEP SDK into your Android app. Add a dependency on Assurance and Core to your mobile application. To ensure consistent builds, it is best to explicitly  specify the dependency version and update them manually.

    ```
    implementation 'com.adobe.marketing.mobile:core:2.+'
    implementation 'com.adobe.marketing.mobile:assurance:2.+'
    ```

2. Import MobileCore and Assurance extensions:
   
   ### Java

   ```
   import com.adobe.marketing.mobile.MobileCore;
   import com.adobe.marketing.mobile.Assurance;
   ```
   
   ### Kotlin

   ```
   import com.adobe.marketing.mobile.MobileCore
   import com.adobe.marketing.mobile.Assurance
   ```

    **Note**: Assurance SDK displays some UI components using the source app context. If you see an error similar to the following :

    ```
    AAPT: error: attribute layout_constraintStart_toStartOf (aka <your_app_name>:layout_constraintStart_toStartOf) not found
    ```
    while building your app with Assurance SDK, include a dependency on `implementation 'androidx.constraintlayout:constraintlayout:1.1.3'` or newer in your app.

3. Import the Assurance library into your project and register it with `MobileCore`
   
   ### Java

   ```
   public class MainApp extends Application {
        private static final String APP_ID = "YOUR_APP_ID";

        @Override
        public void onCreate() {
            super.onCreate();

            MobileCore.setApplication(this);
            MobileCore.setLogLevel(LoggingMode.VERBOSE);
            MobileCore.configureWithAppID(APP_ID);

            List<Class<? extends Extension>> extensions = Arrays.asList(
                    Assurance.EXTENSION,...);
            MobileCore.registerExtensions(extensions, o -> {
                Log.d(LOG_TAG, "AEP Mobile SDK is initialized");
            });
        }
    }
    ```

    ### Kotlin

    ```
    class MyApp : Application() {

        override fun onCreate() {
            super.onCreate()
            MobileCore.setApplication(this)
            MobileCore.setLogLevel(LoggingMode.VERBOSE)
            MobileCore.configureWithAppID("YOUR_APP_ID")

            val extensions = listOf(Assurance.EXTENSION, ...)
            MobileCore.registerExtensions(extensions) {
                Log.d(LOG_TAG, "AEP Mobile SDK is initialized")
            }
        }
    }
    ```


## Creating and connecting to an Assurance session

You can connect to an Assurance session via the `startSession()` api or, via a deep link configured in your app. Deep link is the recommended way of connecting to an Assurance session when using the Android SDK.

Assurance extension is already setup to handle incoming intents to your app. It is sufficient to [add an intent filter for incoming links](https://developer.android.com/training/app-links/deep-linking) in your app to complete the deep link configuration. The combination of `android:host` and `android:scheme` (in the form of `<host>://<scheme>`) of this intent filter will serve as the `Base URL` while creating a session.

### Create a session

1. Visit the [Adobe Experience Platform Assurance UI](https://experience.adobe.com/assurance) and log in using your credentials for the Experience Cloud.
2. Select **Create Session**.
3. In the **Create New Session** dialog, you will be prompted a `Session Name` and a `Base URL`. 
4. Enter a name to identify the session. Provide a deeplink that was configured in your app as the `Base URL`. After providing these details, select **Next**.

### Connect to a session

After you've created a session, you can begin connecting to it by following these steps:

1. Ensure that you see a QR code and a PIN in the **Session Details** dialog.
2. Use your device camera or an app to scan the QR code and to open your app.
3. When your app launches, you should see the a PIN entry screen overlaid.
4. Type in the PIN from the previous step and press **Connect**.
5. Your app should now be connected to Assurance and an Adobe Experience Platform icon will be displayed on your app.