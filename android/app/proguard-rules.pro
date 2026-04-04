# ---- OkHttp + Okio ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# ---- Google Tink / EncryptedSharedPreferences ----
# The security-crypto alpha library does not ship complete consumer ProGuard rules.
# Tink uses reflection internally to instantiate key managers and primitives.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-keep class com.google.crypto.tink.** { *; }
-keep class * extends com.google.crypto.tink.shaded.protobuf.GeneratedMessageLite { *; }

# AndroidX Security (EncryptedSharedPreferences, MasterKey)
-keep class androidx.security.crypto.** { *; }

# ---- App data model classes ----
-keep class com.ruepp.scantoupload.data.model.** { *; }
