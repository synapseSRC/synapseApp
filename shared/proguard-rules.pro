# kotlinx-datetime
-keep class kotlinx.datetime.** { *; }
-keep interface kotlinx.datetime.** { *; }
-keepclassmembers class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# Kotlinx serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.synapse.social.studioasinc.**$$serializer { *; }
-keepclassmembers class com.synapse.social.studioasinc.** {
    *** Companion;
}
-keepclasseswithmembers class com.synapse.social.studioasinc.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Supabase
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
