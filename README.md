
# Adobe Experience Platform Assurance SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.adobe.marketing.mobile/assurance.svg?logo=android&logoColor=green&label=assurance)](https://mvnrepository.com/artifact/com.adobe.marketing.mobile/assurance)


## About this project

AEP Assurance Android SDK is a mobile extension for Adobe Experience Platform that allows integrating with [Adobe Experience Platform Assurance](https://developer.adobe.com/client-sdks/documentation/platform-assurance/) to help 
inspect, proof, simulate, and validate how you collect data or serve experiences in your mobile app. This extension requires [`MobileCore`](https://github.com/adobe/aepsdk-core-android). 

## Getting Started

Refer to the [getting started guide](./Documentation/getting-started.md) for setting up and using the Assurance SDK with your application.

## Development

### Setup

1. Fork this repo for your username and clone it on your development machine.
2. If you haven't already, install Android Studio on your development machine. This project has been verified to work with Andrid Studio Dolphin.
3. Open `aepsdk-assurance-android/code/settings.gradle` file in AndroidStudio

You should now be able to see `assurance` and `assurance-testapp` modules under the `Project --> Android` section.

### Build

Navigate to the project root directory via command line and run the command:
-  `make build` to build the `assurance` library
-  `make build-app` to build `assurance-testapp`

### Run unit tests

Navigate to the project root directory via command line and run the command `make unit-test` to run unit tests.

### Run code format

Navigate to the project root directory via command line and
- run the command `make checkformat` to verify formatting.
- run the command `make format` to fix issues in code format.


## Related Projects

| Project                                                                              | Description                                                                                          |
| ------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------------- |
| [Core extensions](https://github.com/adobe/aepsdk-core-android)                      | The Mobile Core represents the foundation of the Adobe Experience Platform mobile SDK.               |


## Documentation

Additional documentation for usage can be found under the [Documentation](Documentation) directory.


## Contributing

Contributions are welcomed! Read the [Contributing Guide](./.github/CONTRIBUTING.md) for more information.


## Licensing

This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.