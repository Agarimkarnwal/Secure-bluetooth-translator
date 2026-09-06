# Add project specific ProGuard rules here.
# ML Kit translate
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep data model classes
-keep class com.setu.app.model.** { *; }
