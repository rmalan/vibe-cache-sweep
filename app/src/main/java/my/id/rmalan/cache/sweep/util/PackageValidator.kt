package my.id.rmalan.cache.sweep.util

object PackageValidator {
    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    const val SELF_PACKAGE = "my.id.rmalan.cache.sweep"

    fun isValidFormat(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName.length > 256) return false
        return packageName == "android" || PACKAGE_REGEX.matches(packageName)
    }

    fun isSelfPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == SELF_PACKAGE || packageName.startsWith("$SELF_PACKAGE.")
    }

    fun isValid(packageName: String?): Boolean {
        return isValidFormat(packageName) && !isSelfPackage(packageName)
    }

    fun isKnownPackage(packageName: String?, knownPackages: Set<String>): Boolean {
        if (packageName.isNullOrBlank()) return false
        return knownPackages.contains(packageName)
    }

    fun validatePackage(packageName: String?, knownPackages: Set<String>? = null): Result<String> {
        if (packageName.isNullOrBlank()) {
            return Result.failure(IllegalArgumentException("Package name cannot be null or blank"))
        }
        if (isSelfPackage(packageName)) {
            return Result.failure(IllegalArgumentException("CacheSweep self-package cannot be cleaned: $packageName"))
        }
        if (!isValidFormat(packageName)) {
            return Result.failure(IllegalArgumentException("Invalid package name format: $packageName"))
        }
        if (knownPackages != null && !isKnownPackage(packageName, knownPackages)) {
            return Result.failure(IllegalArgumentException("Package not found in known scanned packages: $packageName"))
        }
        return Result.success(packageName)
    }
}
