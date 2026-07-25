# Keep kotlinx.serialization generated serializers for our persisted models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class dev.lumen.launcher.** {
    *** Companion;
}
-keepclasseswithmembers class dev.lumen.launcher.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class dev.lumen.launcher.**$$serializer { *; }

# Launcher entry points referenced from the manifest only.
-keep class dev.lumen.launcher.LauncherApplication { *; }
-keep class dev.lumen.launcher.MainActivity { *; }
-keep class dev.lumen.launcher.data.notifications.BadgeNotificationListener { *; }
