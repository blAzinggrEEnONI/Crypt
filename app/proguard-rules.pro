# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ============== Security & Encryption ==============
# Keep encryption classes - critical for security
-keep class com.example.crypt.data.security.** { *; }
-keep class com.example.crypt.domain.service.CryptLogger { *; }
-keep class com.example.crypt.domain.service.ErrorHandler { *; }
-keep class com.example.crypt.domain.service.InputValidator { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# ============== Keystore & Android Security ==============
# Keep Android Keystore related classes
-keep class android.security.keystore.** { *; }
-keep class java.security.KeyStore { *; }

# ============== Database ==============
# Keep Room database classes
-keep class androidx.room.Room { *; }
-keep class com.example.crypt.data.database.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# ============== Hilt Dependency Injection ==============
# Keep Hilt-generated code
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class com.example.crypt.di.** { *; }

# ============== Jetpack Compose ==============
# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep interface androidx.compose.runtime.** { *; }

# ============== Lifecycle & ViewModel ==============
# Keep ViewModel classes
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class androidx.lifecycle.** { *; }

# ============== Navigation ==============
# Keep navigation components
-keep class androidx.navigation.** { *; }

# ============== Biometric ==============
# Keep biometric authentication
-keep class androidx.biometric.** { *; }
-keep class android.hardware.biometrics.** { *; }

# ============== Coroutines ==============
# Keep coroutine classes
-keep class kotlinx.coroutines.** { *; }
-keep interface kotlinx.coroutines.** { *; }

# ============== Domain Models ==============
# Keep domain model classes
-keep class com.example.crypt.domain.model.** { *; }

# ============== Services ==============
# Keep all domain services
-keep class com.example.crypt.domain.service.** { *; }

# ============== Remove logging in release ==============
# Remove Log.d and Log.v calls
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# ============== Preserve debug info selectively ==============
# Keep line numbers for critical security code
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SecurityCode

# ============== Optimization ==============
# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============== Reflection ==============
# Keep classes used via reflection
-keep class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============== Exceptions ==============
# Keep exception classes
-keep public class * extends java.lang.Exception { *; }
-keep public class * extends java.lang.Throwable { *; }

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}