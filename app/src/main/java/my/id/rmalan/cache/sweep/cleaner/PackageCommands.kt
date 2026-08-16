package my.id.rmalan.cache.sweep.cleaner

import my.id.rmalan.cache.sweep.util.PackageValidator
import java.util.concurrent.TimeUnit

object PackageCommands {
    const val PM_PATH = "/system/bin/pm"
    const val DEFAULT_TIMEOUT_SECONDS = 15L

    data class CommandResult(
        val exitCode: Int,
        val output: String,
        val error: String
    )

    fun buildClearCacheArgs(packageName: String, userId: Int): List<String> {
        require(PackageValidator.isValid(packageName)) { "Invalid package name: $packageName" }
        require(userId >= 0) { "Invalid user id: $userId" }

        val args = listOf(
            PM_PATH,
            "clear",
            "--user",
            userId.toString(),
            "--cache-only",
            packageName
        )

        // Safety invariant check
        check(args.contains("--cache-only") && args.contains("clear")) {
            "CRITICAL DEFECT: Refusing unsafe clear command without --cache-only"
        }

        return args
    }

    fun buildTrimCachesArgs(desiredFreeBytes: Long): List<String> {
        require(desiredFreeBytes >= 0) { "Desired free bytes must be non-negative: $desiredFreeBytes" }

        return listOf(
            PM_PATH,
            "trim-caches",
            desiredFreeBytes.toString()
        )
    }

    fun execute(args: List<String>, timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS): CommandResult {
        // Enforce that if 'clear' is in args, '--cache-only' MUST be present
        if (args.contains("clear")) {
            check(args.contains("--cache-only")) {
                "CRITICAL DEFECT: Attempted execution of clear command without --cache-only"
            }
        }

        val process = ProcessBuilder(args).start()
        process.outputStream.close()

        val outputReader = process.inputStream.bufferedReader()
        val errorReader = process.errorStream.bufferedReader()

        val completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            return CommandResult(
                exitCode = -1,
                output = "",
                error = "Command timed out after ${timeoutSeconds}s: ${args.joinToString(" ")}"
            )
        }

        val output = outputReader.use { it.readText() }
        val error = errorReader.use { it.readText() }
        val exitCode = process.exitValue()

        return CommandResult(
            exitCode = exitCode,
            output = output.trim(),
            error = error.trim()
        )
    }
}
