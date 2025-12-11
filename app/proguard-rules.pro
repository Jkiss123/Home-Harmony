####################################################################################################
# ProGuard/R8 Configuration for Home Harmony App
# Purpose: Code obfuscation, shrinking, and optimization for security
####################################################################################################

# ================================================================================================
# GENERAL SETTINGS
# ================================================================================================

# Keep line numbers for better crash reports (debugging in production)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations for reflection
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep exception information
-keepattributes Exceptions

# Preserve metadata for runtime-visible annotations
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations

# ================================================================================================
# KOTLIN
# ================================================================================================

# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class kotlin.coroutines.SafeContinuation {
    volatile <fields>;
}

# Keep Kotlin data classes (used for models)
-keep class com.example.furniturecloudy.data.** { *; }

# Parcelize support
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements android.os.Parcelable {
    static ** CREATOR;
}

# ================================================================================================
# FIREBASE
# ================================================================================================

# Firebase Authentication
-keep class com.google.firebase.auth.** { *; }
-keepclassmembers class com.google.firebase.auth.** { *; }

# Firebase Firestore - Critical: Keep model classes for serialization
-keep class com.google.firebase.firestore.** { *; }
-keep class com.google.firestore.** { *; }
-keepclassmembers class com.example.furniturecloudy.data.** {
    <init>();
    <fields>;
    public <methods>;
}

# Firebase Storage
-keep class com.google.firebase.storage.** { *; }

# Firebase Common (internal)
-keep class com.google.firebase.** { *; }
-keepclassmembers class com.google.firebase.** {
    <init>();
}

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Google Sign-In
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.common.** { *; }

# ================================================================================================
# HILT / DAGGER (Dependency Injection)
# ================================================================================================

-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class dagger.hilt.** { *; }

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltModules$** { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }

# Keep classes annotated with Hilt annotations
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @javax.inject.Inject class * { *; }

# ================================================================================================
# ROOM DATABASE
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }

# Keep Room generated classes
-keep class **_Impl { *; }

# Keep database entities
-keep class com.example.furniturecloudy.database.entity.** { *; }
-keep class com.example.furniturecloudy.database.dao.** { *; }

# ================================================================================================
# GSON (JSON Serialization)
# ================================================================================================

# Keep generic types
-keepattributes Signature

# Keep Gson annotations
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all model classes used with Gson
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ================================================================================================
# GLIDE (Image Loading)
# ================================================================================================

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}

# ================================================================================================
# NAVIGATION COMPONENT
# ================================================================================================

-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.fragment.app.Fragment {}
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

# Keep SafeArgs generated classes
-keep class **.*Directions { *; }
-keep class **.*Args { *; }

# ================================================================================================
# VIEW BINDING & DATA BINDING
# ================================================================================================

-keep class * implements androidx.viewbinding.ViewBinding {
    public static *** bind(android.view.View);
    public static *** inflate(android.view.LayoutInflater);
}

# Data Binding
-dontwarn android.databinding.**
-keep class android.databinding.** { *; }
-keep class androidx.databinding.** { *; }

# Keep generated binding classes
-keep class com.example.furniturecloudy.databinding.** { *; }

# ================================================================================================
# BIOMETRIC AUTHENTICATION
# ================================================================================================

-keep class androidx.biometric.** { *; }
-keep class android.hardware.biometrics.** { *; }

# ================================================================================================
# ANDROID KEYSTORE (Encryption)
# ================================================================================================

-keep class android.security.keystore.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }

# Keep encryption helper classes
-keep class com.example.furniturecloudy.util.AddressEncryptionHelper { *; }
-keepclassmembers class com.example.furniturecloudy.util.AddressEncryptionHelper {
    public <methods>;
}

# ================================================================================================
# SESSION MANAGEMENT & SECURITY
# ================================================================================================

# Keep security-related classes from obfuscation for auditing
-keep class com.example.furniturecloudy.util.SessionManager { *; }
-keep class com.example.furniturecloudy.util.AppAuthManager { *; }
-keep class com.example.furniturecloudy.util.BiometricHelper { *; }
-keep class com.example.furniturecloudy.util.PinCodeManager { *; }

# ================================================================================================
# MOMO PAYMENT SDK
# ================================================================================================

-keep class vn.momo.momo_partner.** { *; }
-keep interface vn.momo.momo_partner.** { *; }
-keepclassmembers class vn.momo.momo_partner.** {
    <init>(...);
    public <methods>;
}

# Keep payment helper classes
-keep class com.example.furniturecloudy.utils.payment.** { *; }

# ================================================================================================
# VIEWMODELS & LIFECYCLE
# ================================================================================================

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# Keep ViewModel constructors for Hilt injection
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep all ViewModels in project
-keep class com.example.furniturecloudy.model.viewmodel.** { *; }

# ================================================================================================
# ADAPTERS (RecyclerView)
# ================================================================================================

# Keep adapter classes (they use reflection for ViewHolder)
-keep class com.example.furniturecloudy.model.adapter.** { *; }

# ================================================================================================
# THIRD-PARTY LIBRARIES
# ================================================================================================

# CircleImageView
-keep class de.hdodenhof.circleimageview.** { *; }

# StepView
-keep class com.github.shuhart.stepview.** { *; }

# Loading Button
-keep class br.com.simplepass.loadingbutton.** { *; }

# ================================================================================================
# SERIALIZABLE & ENUMS
# ================================================================================================

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================================================================================
# RETAIN SERVICE METHOD PARAMETERS (for Firebase callbacks)
# ================================================================================================

-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

# ================================================================================================
# REMOVE LOGGING IN RELEASE
# ================================================================================================

# Remove Log calls (security: prevent log leaks)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ================================================================================================
# OPTIMIZATION
# ================================================================================================

# Aggressive optimization (optional - may increase build time)
-optimizationpasses 5
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

# Allow aggressive obfuscation
-repackageclasses ''
-allowaccessmodification

# ================================================================================================
# WARNINGS TO SUPPRESS
# ================================================================================================

-dontwarn javax.annotation.**
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.jvm.internal.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ================================================================================================
# END OF PROGUARD RULES
# ================================================================================================