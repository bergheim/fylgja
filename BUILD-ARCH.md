# Build on Arch Linux (No AUR Required)

This guide builds the Fylgja APK using only official Arch packages and Google's official SDK tools.

## Prerequisites

### 1. Install from official repos

```bash
sudo pacman -S jdk17-openjdk gradle
```

- `jdk17-openjdk` — Java 17 (required by Gradle 8.x)
- `gradle` — Latest Gradle from official repos

Both are in the `extra` repository, no AUR needed.

### 2. Download Android SDK cmdline-tools (official Google source)

The Android SDK command-line tools come directly from Google. No AUR packages required.

```bash
# Create SDK directory
mkdir -p ~/.android-sdk/cmdline-tools

# Download latest cmdline-tools for Linux
cd /tmp
curl -LO https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools ~/.android-sdk/cmdline-tools/latest
rm commandlinetools-linux-11076708_latest.zip
```

Update your `PATH`:

```bash
echo 'export ANDROID_HOME=~/.android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc
```

### 3. Accept licenses and install required SDK components

```bash
# Accept all SDK licenses
yes | sdkmanager --licenses

# Install platform-tools, Android 34 platform, and build-tools
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
```

## Build the APK

Generate the Gradle wrapper and build:

```bash
cd Fylgja
gradle wrapper --gradle-version 8.11
./gradlew assembleDebug
```

The APK will be at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Transfer to GrapheneOS

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## What's official vs. downloaded

| Component | Source |
|-----------|--------|
| `jdk17-openjdk` | pacman (extra) |
| `gradle` | pacman (extra) |
| `cmdline-tools` | dl.google.com |
| `platform-tools` | Google SDK Manager |
| `platforms;android-34` | Google SDK Manager |
| `build-tools;34.0.0` | Google SDK Manager |

No AUR packages used. Everything comes from either Arch `extra` or Google's official distribution.

## Troubleshooting

**Java version mismatch:**
```bash
sudo archlinux-java set java-17-openjdk
```

**SDK license errors:**
```bash
yes | sdkmanager --licenses
```
