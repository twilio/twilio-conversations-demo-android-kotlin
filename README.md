# Conversations Demo Application Overview

This demo app SDK version: ![](https://img.shields.io/badge/SDK%20version-1.3.1-blue.svg)

Latest available SDK version: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.twilio/conversations-android/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.twilio/conversations-android)

## Getting Started

Welcome to the Conversations Demo application.  This application demonstrates a basic conversations client application with the ability to create and join conversations, add other participants into the conversations and exchange messages.

What you'll minimally need to get started:

- A clone of this repository
- [A way to create a Conversations Service Instance and generate client tokens](https://www.twilio.com/docs/conversations/identity)
- Google Play Services library : [Follow the instructions here](https://developers.google.com/android/guides/setup)

## Building

### Add google-services.json

[Generate google-services.json](https://firebase.google.com/docs/crashlytics/upgrade-sdk?platform=android#add-config-file) file and place it under `app/`.

### Set the value of `ACCESS_TOKEN_SERVICE_URL`

Set the value of `ACCESS_TOKEN_SERVICE_URL` in `gradle.properties` file to point to a valid Access-Token server.
So your Access-Token server should provide valid token for valid credentials by URL:

 ```
$ACCESS_TOKEN_SERVICE_URL?identity=<USER_PROVIDED_USERNAME>&password=<USER_PROVIDED_PASSWORD>
 ```

and return HTTP 401 if case of invalid credentials.

Create the `gradle.properties` file if it doesn't exist with the following contents:

```
ACCESS_TOKEN_SERVICE_URL=http://example.com/get-token/
```

NOTE: no need for quotes around the URL, they will be added automatically.

You can also pass these parameters to gradle during build without need to create a properties file, as follows:

```
./gradlew app:assembleDebug -PACCESS_TOKEN_SERVICE_URL=http://example.com/get-token/
```

### Optionally setup Firebase Crashlytics

If you want to see crashes reported to crashlytics:
1. [Set up Crashlytics in the Firebase console](https://firebase.google.com/docs/crashlytics/get-started?platform=android#setup-console)

2. In order to see native crashes symbolicated upload symbols into the Firebase console:
```
./gradlew app:assembleBUILD_VARIANT
./gradlew app:uploadCrashlyticsSymbolFileBUILD_VARIANT
```
for example to upload symbols for `debug` build type run:
```
./gradlew app:assembleDebug
./gradlew app:uploadCrashlyticsSymbolFileDebug
```

[Read more](https://firebase.google.com/docs/crashlytics/upgrade-sdk?platform=android#optional_step_set_up_ndk_crash_reporting) about Android NDK crash reports.

3. Login into application and navigate to `Menu -> Simulate crash in` in order to check that crashes coming into Firebase console.

### Build

Run `./gradlew app:assembleDebug` to fetch Twilio SDK files and build application.

### Android Studio

You can import this project into Android Studio and then build as you would ordinarily. The token server setup is still important.

### Debug

Build in debug configuration, this will enable verbose logging.

## License

MIT
