FROM eclipse-temurin:17-jdk-jammy AS builder

ENV ANDROID_HOME=/opt/android-sdk
ENV GRADLE_HOME=/opt/gradle
ENV PATH="${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${GRADLE_HOME}/bin"

# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip wget && \
    rm -rf /var/lib/apt/lists/*

# Install Gradle 8.5 (matches gradle-wrapper.properties)
RUN wget -q https://services.gradle.org/distributions/gradle-8.5-bin.zip -O /tmp/gradle.zip && \
    unzip -q /tmp/gradle.zip -d /opt && \
    mv /opt/gradle-8.5 ${GRADLE_HOME} && \
    rm /tmp/gradle.zip

# Install Android command-line tools
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools && \
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdline-tools.zip && \
    unzip -q /tmp/cmdline-tools.zip -d ${ANDROID_HOME}/cmdline-tools && \
    mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest && \
    rm /tmp/cmdline-tools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses > /dev/null 2>&1 && \
    sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

WORKDIR /app
COPY . .

# Generate the Gradle wrapper (creates gradle-wrapper.jar + gradlew)
RUN gradle wrapper --gradle-version 8.5

# Generate a signing keystore (so the APK is installable)
RUN keytool -genkey -v \
    -keystore /app/release.jks \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -alias scantoupload \
    -storepass android -keypass android \
    -dname "CN=Scan to Upload, O=Ruepp, L=Unknown, ST=Unknown, C=US" && \
    printf 'storeFile=../release.jks\nstorePassword=android\nkeyAlias=scantoupload\nkeyPassword=android\n' > /app/keystore.properties

# Build the signed release APK
RUN ./gradlew assembleRelease --no-daemon

# --- Output stage: just the APK ---
FROM scratch AS output
COPY --from=builder /app/app/build/outputs/apk/release/*.apk /
