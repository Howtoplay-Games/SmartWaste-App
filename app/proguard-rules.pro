# Minification is disabled by default (see app/build.gradle.kts).
# If you enable it, these keep rules cover reflection-based libraries.
-keepattributes *Annotation*, InnerClasses, Signature
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**
-keepclassmembers class sg.smartwaste.app.**$$serializer { *; }
-keepclasseswithmembers class sg.smartwaste.app.** { kotlinx.serialization.KSerializer serializer(...); }
