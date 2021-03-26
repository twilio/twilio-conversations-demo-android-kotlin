# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Workaround in order to build release androidTest. Probably bug in R8 optimiser.
# See: https://stackoverflow.com/questions/58717741/building-androidtest-apk-gets-r8-errors-already-has-a-mapping
-dontoptimize

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Twilio
-keep class !com.twilio.conversations.app.**, com.twilio.conversations.** { *; }

# Used by espresso in UI tests
-keepclassmembers class androidx.recyclerview.widget.RecyclerView { scrollToPosition(int); }

# AndroidX
-keep class androidx.** { *; }

# Timber
-keepclassmembers class timber.log.Timber { *; }
