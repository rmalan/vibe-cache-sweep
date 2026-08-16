package my.id.rmalan.cache.sweep.util

object PackageValidator {
    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    const val SELF_PACKAGE = "my.id.rmalan.cache.sweep"

    fun isValidFormat(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName.length > 256) return false
        return PACKAGE_REGEX.matches(packageName)
    }

    fun isSelfPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == SELF_PACKAGE || packageName.startsWith("$SELF_PACKAGE.")
    }

    fun isValid(packageName: String?): Boolean {
        return isValidFormat(packageName) && !isSelfPackage(packageName)
    }
}
