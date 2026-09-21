package qorrnsmj.smf.game.level.custom

import org.lwjgl.glfw.GLFW.glfwSetWindowTitle
import qorrnsmj.smf.SMF
import java.io.File

class TestLevel : BaseLevel("test") {
    override fun load() {
        currentBranchName()?.let { glfwSetWindowTitle(SMF.window.id, it) }
        super.load()
    }

    private fun currentBranchName(): String? = runCatching {
        val projectDirectory = File(System.getProperty("user.dir"))
        val dotGit = projectDirectory.resolve(".git")
        val gitDirectory = when {
            dotGit.isDirectory -> dotGit
            dotGit.isFile -> {
                val path = dotGit.readText().trim().removePrefix("gitdir:").trim()
                File(path).let { if (it.isAbsolute) it else projectDirectory.resolve(it) }
            }
            else -> return null
        }
        val head = gitDirectory.resolve("HEAD").readText().trim()
        head.removePrefix("ref: refs/heads/").takeIf { head.startsWith("ref: refs/heads/") }
    }.getOrNull()
}
