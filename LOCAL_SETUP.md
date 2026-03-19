# Local Setup

## Android SDK
- Set `ANDROID_HOME` to your Android SDK root, or create `local.properties` with `sdk.dir=/absolute/path/to/Android/sdk`

## Firebase
- Copy `app/google-services.example.json` to `app/google-services.json`
- Replace the placeholder values with your real Firebase Android app config

## Release Signing
- Debug builds do not require release signing
- If you need a release build, copy `keystore.properties.example` to `keystore.properties`
- Point `storeFile` at a local keystore that is not committed to git

## Firestore Email Update Script
- Copy `update-firestore-emails/serviceAccountKey.example.json` to `update-firestore-emails/serviceAccountKey.json`
- Fill in your Firebase service-account credentials locally only

## Verification Commands
- `./gradlew app:compileDebugKotlin`
- `./gradlew testDebugUnitTest`
- `./gradlew lintDebug`
