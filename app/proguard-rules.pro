-repackageclasses
-ignorewarnings
-dontwarn
-dontnote
# Keep all Activities
-keep public class * extends android.app.Activity

# Specifically keep your N3 activity (if you know its original name)
-keep class com.synapse.social.studioasinc.N3 { *; }

# Keep Supabase classes
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }

# Keep kotlinx-datetime classes
-keep class kotlinx.datetime.** { *; }
-dontwarn kotlinx.datetime.**

# Keep gRPC classes for network communication
-keep class io.grpc.** { *; }
-keep class io.grpc.android.** { *; }

# libsignal-android (org.signal:libsignal-android)
-keep class org.signal.libsignal.** { *; }
-keepclassmembers class org.signal.libsignal.** { *; }
-dontwarn org.signal.libsignal.**

# WebRTC (io.github.webrtc-sdk:android) — the JNI layer calls back into org.webrtc classes by name,
# so R8 must not rename or strip them. Guards a future minifyEnabled = true flip.
-keep class org.webrtc.** { *; }
-keep class com.synapse.social.studioasinc.data.call.webrtc.** { *; }
-dontwarn org.webrtc.**

# jni_zero ships with WebRTC M144 and is reachable only from native code: JNI_OnLoad does
# FindClass("org/jni_zero/JniInit") at load time, so R8 sees the whole package as dead. When it was
# stripped the CHECK failed into a SIGTRAP, killing the process on the first call attempt.
-keep class org.jni_zero.** { *; }

# ── Release minification guards ───────────────────────────────────────────────
# Gson rebuilds generic types (List<PendingAction>) from erased signatures, so they must survive.
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod

# Chat background names are read off the field names of R$raw reflectively.
-keep class **.R$raw { *; }

# ActionQueue writes PendingAction as Gson JSON into EncryptedSharedPreferences. Field names and
# enum constants are the storage keys — obfuscating them orphans actions already queued on device.
-keepclassmembers class com.synapse.social.studioasinc.shared.domain.model.PendingAction { *; }
-keepclassmembers enum com.synapse.social.studioasinc.shared.domain.model.PendingAction$ActionType { *; }

# EncryptedSharedPreferences reaches Tink's primitive registry, which instantiates by class name.
-keep class com.google.crypto.tink.** { *; }

# The Gemini SDK maps JSON onto AutoValue models through Gson reflection.
-keep class com.google.genai.** { *; }
