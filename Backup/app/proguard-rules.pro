# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep class com.jnetai.stopwatch.** { *; }

# Keep error tracking classes
-keep class com.jnetai.stopwatch.utils.ErrorLogger { *; }
-keep class com.jnetai.stopwatch.utils.SettingsManager { *; }
