package my.id.rmalan.cache.sweep.shizuku;

interface ICacheOpsService {
    void destroy() = 16777114;
    int getProtocolVersion() = 1;
    int getPrivilegedUid() = 2;
    boolean supportsSelectiveCacheClear() = 3;
    boolean supportsGlobalTrim() = 4;
    int clearPackageCache(String packageName, int userId) = 5;
    int trimCaches(long desiredFreeBytes) = 6;
    String getLastError() = 7;
}

