# NetPulse R8 rules. Most libraries (Room, Hilt, WorkManager, Glance, Firebase, OkHttp)
# ship consumer rules; only app-specific keeps live here.

# Keep line numbers for readable crash reports, hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# kotlinx.serialization: keep generated serializers for @Serializable classes
# (navigation routes and the settings backup model).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class com.example.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class com.example.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance widget receiver and worker classes are referenced from the manifest / by name.
-keep class com.example.widget.** extends androidx.glance.appwidget.GlanceAppWidgetReceiver
-keep class * extends androidx.work.ListenableWorker { <init>(...); }

# OkHttp optional platform integrations.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
