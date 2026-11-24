#!/usr/bin/env bash
# Сборка APK на компьютере с установленным Android SDK (Android Studio).
# Нужен JDK 17+ и переменная ANDROID_HOME (или файл local.properties с sdk.dir=...).
set -e
cd "$(dirname "$0")"
chmod +x gradlew
./gradlew assembleDebug assembleRelease
mkdir -p output
cp app/build/outputs/apk/debug/app-debug.apk output/TOYLEK-STUDIO-debug.apk
[ -f app/build/outputs/apk/release/app-release.apk ] && cp app/build/outputs/apk/release/app-release.apk output/TOYLEK-STUDIO.apk
echo "Готово: папка output"
