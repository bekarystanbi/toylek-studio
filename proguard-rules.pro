# CameraX и Media3 поставляют собственные consumer-правила.
-keep class com.toylek.studio.kiosk.** { *; }
-dontwarn org.checkerframework.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn com.google.auto.value.**
-dontwarn afu.org.checkerframework.**
-keep class androidx.media3.** { *; }
