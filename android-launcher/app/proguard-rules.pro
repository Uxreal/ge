# Lumen release rules.
#
# Kotlin, Compose, Hilt, Room and DataStore all ship consumer rules; what remains is protobuf lite,
# whose generated messages are reached reflectively by the runtime.
-keep class dev.lumen.launcher.core.data.proto.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# AppWidgetHostView subclass creation paths use reflection on some OEMs.
-keep class android.appwidget.AppWidgetHostView { *; }
