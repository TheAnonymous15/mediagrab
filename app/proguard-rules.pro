# ==================== MediaGrab ProGuard Rules ====================
# Optimized rules to enable minification while preserving critical classes

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ==================== APP CLASSES ====================
# Keep all app classes (most important for stability)
-keep class com.example.dwn.** { *; }
-keepclassmembers class com.example.dwn.** { *; }

# ==================== YT-DLP / CHAQUOPY (CRITICAL) ====================
# These use Python + reflection extensively - DO NOT OBFUSCATE
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }
-keepclassmembers class com.yausername.** { *; }

# Chaquopy Python runtime - MUST keep everything
-keep class com.chaquo.python.** { *; }
-keepclassmembers class com.chaquo.python.** { *; }
-dontwarn com.chaquo.python.**

# ==================== ROOM DATABASE ====================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ==================== KOTLIN ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlin.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ==================== ANDROIDX / JETPACK ====================
-keep class androidx.** { *; }
-keepclassmembers class androidx.** { *; }
-dontwarn androidx.**

# Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Media3 / ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ==================== NATIVE LIBRARIES ====================
-keepclasseswithmembernames class * { native <methods>; }

# ==================== SERIALIZATION ====================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations

# Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== SUPPRESS WARNINGS ====================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
-dontwarn javax.annotation.**
-dontwarn java.lang.invoke.**
-dontwarn org.jetbrains.annotations.**
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.**
-dontwarn org.slf4j.**
-dontwarn sun.misc.**
