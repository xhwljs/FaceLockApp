# InspireFace SDK ships its own native lib; keep all SDK classes intact.
-keep class com.insightface.sdk.inspireface.** { *; }
-keepclassmembers class com.insightface.sdk.inspireface.** { *; }

# Keep Kotlinx Serialization metadata.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
