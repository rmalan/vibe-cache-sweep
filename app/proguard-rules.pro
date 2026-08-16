# Proguard rules for CacheSweep

# Shizuku rules
-keep class * extends rikka.shizuku.ShizukuProvider { *; }
-keep class * implements rikka.shizuku.Shizuku$* { *; }
-keep class my.id.rmalan.cache.sweep.shizuku.CacheOpsUserService { *; }
-keep class my.id.rmalan.cache.sweep.shizuku.ICacheOpsService** { *; }

