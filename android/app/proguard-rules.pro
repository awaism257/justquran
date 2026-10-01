# Proguard rules for JustQuran

-keepattributes *Annotation*,InnerClasses,Signature

# Compose
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Kotlinx Serialization & Data models
-keep class org.justquran.app.data.** { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <init>(...);
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep class * implements kotlinx.serialization.KSerializer { *; }
-dontwarn kotlinx.serialization.**

# Media3 (ExoPlayer & MediaSession)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }
-keep class androidx.media3.common.** { *; }
-dontwarn androidx.media3.**

# App components
-keep class org.justquran.app.MainActivity { *; }
-keep class org.justquran.app.audio.AudioService { *; }
-keep class org.justquran.app.CrashReporter { *; }

# Coroutines and DataStore
-dontwarn kotlinx.coroutines.**
-dontwarn androidx.datastore.**
