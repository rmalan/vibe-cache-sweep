# Proguard rules for CacheSweep

# Shizuku rules
-keep class * extends rikka.shizuku.ShizukuProvider { *; }
-keep class * implements rikka.shizuku.Shizuku$* { *; }
