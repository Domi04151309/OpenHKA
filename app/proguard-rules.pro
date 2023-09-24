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
-keepattributes SourceFile,LineNumberTable

-dontobfuscate
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*,!code/allocation/variable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.sapuseven.untis.**$$serializer { *; }
-keepclassmembers class com.sapuseven.untis.** {
    *** Companion;
}
-keepclasseswithmembers class com.sapuseven.untis.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

-dontwarn org.joda.convert.**

# ical4j
-dontwarn org.apache.commons.logging.**
-keep class org.apache.commons.logging.** { *; }
-dontnote com.google.vending.**
-keep class com.google.vending.** { *; }
-dontnote com.android.vending.licensing.**
-keep class com.android.vending.licensing.** { *; }
-dontwarn net.fortuna.ical4j.model.**
-keep class net.fortuna.ical4j.model.** { *; }
-dontwarn org.jsr107.**
-keep class org.jsr107.** { *; }
-keep class javax.cache.** { *; }
-dontwarn java.cache.**
-keep class org.jsr107.ri.** { *; }
-dontwarn org.jsr107.ri.**

-dontwarn javax.cache.management.**
-keep class javax.cache.management.** { *; }
-dontwarn javax.enterprise.inject.spi.**
-keep class javax.enterprise.inject.spi.** { *; }

-dontwarn aQute.bnd.**
-dontwarn org.codehaus.**
-dontwarn groovyjarjarasm.**
-dontwarn groovyjarjarantlr.**
-dontwarn org.slf4j.**

## ical4j also contains groovy code which is not used in android
-dontwarn groovy.**
-dontwarn org.codehaus.groovy.**
-dontwarn sun.misc.Perf

-dontnote com.google.vending.**
-dontnote com.android.vending.licensing.**

###################
# Get rid of #can't find referenced method in library class java.lang.Object# warnings for clone() and finalize()
# Warning: net.fortuna.ical4j.model.CalendarFactory: can't find referenced method 'void finalize()' in library class java.lang.Object
# Warning: net.fortuna.ical4j.model.ContentBuilder: can't find referenced method 'java.lang.Object clone()' in library class java.lang.Object
# for details see http://stackoverflow.com/questions/23883028/how-to-fix-proguard-warning-cant-find-referenced-method-for-existing-methods
-dontwarn net.fortuna.ical4j.model.**
