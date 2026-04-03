# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Google Tink / EncryptedSharedPreferences — annotations not included at runtime
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.**
-dontwarn com.google.crypto.tink.**

# Keep data model classes
-keep class com.ruepp.scantoupload.data.model.** { *; }
