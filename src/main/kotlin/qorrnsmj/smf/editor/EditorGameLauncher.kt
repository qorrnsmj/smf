package qorrnsmj.smf.editor

import java.io.File
import java.nio.file.Files
import java.nio.file.Path

internal class EditorGameLauncher {
    private var process: Process? = null
    private var log: Path? = null

    val isRunning: Boolean get() = process?.isAlive == true

    fun launch(level: Path) {
        check(!isRunning) { "A game launched by this editor is already running" }
        val map = level.toAbsolutePath().normalize()
        require(Files.isRegularFile(map)) { "Map not found: $map" }
        val javaBin = Path.of(System.getProperty("java.home"), "bin")
        val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        val executable = javaBin.resolve(if (windows) "javaw.exe" else "java")
        require(Files.isRegularFile(executable)) { "Java executable not found: $executable" }
        val logs = EditorProjectStore.appRoot.resolve("logs")
        Files.createDirectories(logs)
        val output = Files.createTempFile(logs, "game-", ".log")
        try {
            process = ProcessBuilder(
                executable.toString(), "-cp", launchClasspath(),
                "qorrnsmj.smf.SMF", "--map", map.toString(),
            ).directory(Path.of(System.getProperty("user.dir")).toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start()
            log = output
        } catch (error: Exception) {
            throw IllegalStateException("Game could not be started: ${error.message}\nLog: $output", error)
        }
    }

    fun collectFailure(): String? {
        val child = process ?: return null
        if (child.isAlive) return null
        process = null
        val code = child.exitValue()
        return if (code == 0) null else "Game exited with code $code.\nLog: $log"
    }

    private fun launchClasspath(): String {
        val entries = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .toMutableList()
        val sourceResources = Path.of(System.getProperty("user.dir"), "src", "main", "resources")
            .toAbsolutePath()
            .normalize()
        if (Files.isDirectory(sourceResources)) {
            entries.add(0, sourceResources.toString())
        }
        return entries.distinct().joinToString(File.pathSeparator)
    }
}
