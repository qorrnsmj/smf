package qorrnsmj.smf.game.progress

import org.tinylog.kotlin.Logger
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class GameProgressStore(
    private val savePath: Path = defaultSavePath(),
) {
    fun loadOrNull(): GameProgress? {
        if (!savePath.exists()) {
            Logger.info("No game progress save found at {}", savePath)
            return null
        }

        val progress = GameProgressJson.decode(savePath.readText())
        Logger.info(
            "Game progress loaded: path={}, stage={}, position={}, facing={}, flags={}, inventory={}, defeatedMobIds={}",
            savePath,
            progress.currentStageName,
            progress.playerPosition,
            progress.playerFacing,
            progress.flags.size,
            progress.inventory.size,
            progress.defeatedMobIds.size,
        )
        return progress
    }

    fun save(progress: GameProgress) {
        Files.createDirectories(savePath.parent)
        savePath.writeText(GameProgressJson.encode(progress))
        Logger.info(
            "Game progress saved: path={}, stage={}, position={}, facing={}, flags={}, inventory={}, defeatedMobIds={}",
            savePath,
            progress.currentStageName,
            progress.playerPosition,
            progress.playerFacing,
            progress.flags.size,
            progress.inventory.size,
            progress.defeatedMobIds.size,
        )
    }

    fun getSavePath(): Path = savePath

    companion object {
        fun defaultSavePath(): Path {
            val appData = System.getenv("APPDATA")
            val basePath = if (appData.isNullOrBlank()) {
                Path.of(System.getProperty("user.home"), "AppData", "Roaming")
            } else {
                Path.of(appData)
            }
            return basePath.resolve("SMF").resolve("save.json")
        }
    }
}
