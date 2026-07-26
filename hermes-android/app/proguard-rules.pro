# kotlinx.serialization keeps its generated serializers on the annotated classes.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.hermes.voice.** {
    *** Companion;
}
-keepclasseswithmembers class com.hermes.voice.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp platform shims referenced reflectively.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
