
# Adobe Experience Platform Assurance SDK

## About this project

AEP Assurance Android SDK allows integrating with [Adobe Experience Platform Assurance](https://developer.adobe.com/client-sdks/documentation/platform-assurance/) to help 
inspect, proof, simulate, and validate how you collect data or serve experiences in your mobile app. 

## Requirements
- Android API 19 or newer
- [MobileCore](https://github.com/adobe/aepsdk-core-android)

## Installing AEP Assurance Android SDK

Installation via [Maven](https://maven.apache.org/) & [Gradle](https://gradle.org/) is the easiest and recommended way to get the AEP SDK into your Android app. In your build.gradle file, include the latest version of following dependency:

```
implementation 'com.adobe.marketing.mobile:assurance:2.x.x'
```

## Development

### Setup
1. Fork this repo for your username and clone it on your development machine.
2. If you haven't already, install Android Studio on your development machine. This project has been verified to work with Andrid Studio Dolphin.
3. Open Android Studio. Click on the "Open" button from the "Welcome to Android Studio" dialog. Navigate to : `aepsdk-assurance-android/code` and select `settings.gradle`

You should now be able to see `assurance` and `assurance-testapp` modules under the `Project --> Android` section.

### Build
Navigate to the project root directory via command line and run the command:
-  `make ci-build` to build the `assurance` library 
-  `make ci-build-app` to build the `assurance-testapp`

### Run unit tests
Navigate to the project root directory via command line and run the command `make ci-unit-test` to run unit tests.

### Run code format
Navigate to the project root directory via command line and
-  run the command `make ci-checkformat` to verify formatting.
- run the command `make ci-format` to fix issues in code format.

## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.

## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.