package my.id.rmalan.cache.sweep.shizuku;

interface ICacheOpsService {
    int getProtocolVersion();
    int getPrivilegedUid();
    boolean supportsSelectiveCacheClear();
    boolean supportsGlobalTrim();
    int clearPackageCache(String packageName, int userId);
    int trimCaches(long desiredFreeBytes);
    String getLastError();
}
