@echo off
rem Build APK on Windows. Requires Android Studio (JDK 17+ and Android SDK).
cd /d "%~dp0"
call gradlew.bat assembleDebug assembleRelease
if errorlevel 1 exit /b 1
if not exist output mkdir output
copy /Y app\build\outputs\apk\debug\app-debug.apk output\TOYLEK-STUDIO-debug.apk
if exist app\build\outputs\apk\release\app-release.apk copy /Y app\build\outputs\apk\release\app-release.apk output\TOYLEK-STUDIO.apk
echo Done: APK files are in the output folder
