# Firebase
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play Services
-keep class com.google.android.gms.** { *; }

# Jetpack Compose
-keep class androidx.compose.** { *; }

# EncryptedSharedPreferences (для токенов авторизации)
-keep class androidx.security.crypto.** { *; }

# Credentials API
-keep class androidx.credentials.** { *; }

# Room
-keep class androidx.room.** { *; }

# Lifecycle, ViewModel
-keep class androidx.lifecycle.** { *; }

# General
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Gson (если используешь)
-keep class com.google.gson.** { *; }


# Kotlin metadata (иногда важно)
-keepclassmembers class kotlin.Metadata { *; }
# WorkManager (важно для PeriodicWorkRequest, WorkerParameters, Worker)
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
# Для GSON: сохраняем все поля в моделях
-keep class com.feofanova.mathup.data.remote.model.** { *; }
-keep class com.feofanova.mathup.data.characters.entities.** { *; }
-keep class com.feofanova.mathup.data.local.entities.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken
