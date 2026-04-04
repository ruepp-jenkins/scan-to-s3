# Scan to Upload — Android App Build Guide

## Prerequisites

- **Android Studio** Hedgehog (2023.1.1) or newer
- **JDK 17** (bundled with Android Studio)
- **Android SDK 36** (install via Android Studio → SDK Manager)
- A physical Android device or emulator running Android 8.0+ (API 26+)

## Local Development

1. Open Android Studio
2. Select **File → Open** and navigate to the `android/` directory
3. Wait for Gradle sync to complete
4. Select a device/emulator from the toolbar
5. Click **Run** (green play button) or press `Shift+F10`

### Connecting to a local backend

By default, Android emulators can reach your host machine at `10.0.2.2`. If your backend runs on port 3000:

- In the app's login screen, enter `http://10.0.2.2:3000` as the Server URL

The `network_security_config.xml` already allows cleartext traffic to `10.0.2.2` and `localhost` for development.

For a physical device on the same network, use your machine's local IP address with HTTPS or configure the network security config.

## Release Signing

### 1. Generate a keystore

```bash
keytool -genkey -v -keystore scan-to-upload.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias scantoupload
```

Keep this keystore safe — you need the same key to publish updates.

### 2. Configure signing in Gradle

Create `android/keystore.properties` (do NOT commit this file):

```properties
storeFile=../scan-to-upload.jks
storePassword=your_keystore_password
keyAlias=scantoupload
keyPassword=your_key_password
```

Add to `app/build.gradle.kts` inside `android {}`:

```kotlin
signingConfigs {
    create("release") {
        val props = java.util.Properties().apply {
            load(rootProject.file("keystore.properties").inputStream())
        }
        storeFile = file(props["storeFile"] as String)
        storePassword = props["storePassword"] as String
        keyAlias = props["keyAlias"] as String
        keyPassword = props["keyPassword"] as String
    }
}

buildTypes {
    release {
        signingConfig = signingConfigs.getByName("release")
        // ... existing config
    }
}
```

## Building with Docker (headless server / CI)

No Android Studio or SDK installation required — everything is containerized.

### Build the APK

```bash
cd android
docker build --output=. .
```

This builds the unsigned release APK and copies it to the current directory. The multi-stage Dockerfile:
1. Installs JDK 17, Gradle 9.3.1, Android SDK 36, build-tools 36.0.0
2. Generates the Gradle wrapper
3. Runs `assembleRelease`
4. Outputs just the `.apk` file

### Build the APK (keep in image)

```bash
cd android
docker build -t scantoupload-builder --target builder .
docker cp $(docker create scantoupload-builder):/app/app/build/outputs/apk/release/ ./output/
```

### CI/CD Integration (Jenkins)

To add to your existing Jenkins pipeline, add a stage:

```groovy
stage('Build Android APK') {
    steps {
        dir('android') {
            sh 'docker build --output=. .'
            archiveArtifacts artifacts: '*.apk', fingerprint: true
        }
    }
}
```

### Resource requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| RAM      | 4 GB    | 8 GB        |
| Disk     | 10 GB   | 15 GB       |
| CPU      | 2 cores | 4 cores     |

First build takes longer (~5-10 min) due to downloading SDK + dependencies. Subsequent builds are faster if you cache the Docker layers.

### Caching Gradle dependencies

To speed up repeated builds, use a Docker volume for the Gradle cache:

```bash
docker build --build-arg GRADLE_CACHE=/gradle-cache -t scantoupload-builder .
```

Or use BuildKit cache mounts by adding this before the build step in the Dockerfile:

```dockerfile
RUN --mount=type=cache,target=/root/.gradle ./gradlew assembleRelease --no-daemon
```

## Building a Release AAB

```bash
cd android
./gradlew bundleRelease
```

The AAB will be at `app/build/outputs/bundle/release/app-release.aab`.

## Building a Release APK (for direct installation)

```bash
cd android
./gradlew assembleRelease
```

The APK will be at `app/build/outputs/apk/release/app-release.apk`.

## Google Play Console Setup

1. Create a [Google Play Developer account](https://play.google.com/console/) ($25 one-time fee)
2. Create a new app in the Console
3. Fill in the store listing:
   - App name: **Scan to Upload**
   - Short description: PDF upload utility
   - Full description: Upload scanned PDF documents directly to your server
   - Screenshots: Take from emulator (phone + 7" tablet minimum)
   - Feature graphic: 1024x500 banner
4. Set up a privacy policy URL (required)
5. Complete the content rating questionnaire
6. Set pricing & distribution (likely private/internal)

## Submitting for Review

1. Go to **Release → Production** in Play Console
2. Click **Create new release**
3. Upload the `.aab` file
4. Add release notes
5. Click **Review release** then **Start rollout**

First review typically takes 1-3 days. Subsequent updates are usually faster.

## Updating the App

1. Bump `versionCode` (integer, must increase) and `versionName` in `app/build.gradle.kts`
2. Build a new AAB: `./gradlew bundleRelease`
3. Upload to Play Console under a new release
4. Add release notes describing changes
