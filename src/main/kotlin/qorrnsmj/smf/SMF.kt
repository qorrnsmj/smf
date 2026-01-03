package qorrnsmj.smf

import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.core.Timer
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.skybox.Skyboxes
import qorrnsmj.smf.game.terrain.Terrains
import qorrnsmj.smf.game.texture.Textures
import qorrnsmj.smf.graphic.render.MasterRenderer
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.window.SMFKeyCallback
import qorrnsmj.smf.window.Window

object SMF : FixedTimestepGame() {
    init {
        Logger.info("SMF initializing...")

        check(glfwInit()) { Logger.error("Failed to initialize GLFW!") }
        window = Window(1600, 1600, "SMF", true)
        renderer = MasterRenderer()
        renderer.resize(window.width, window.height)

        Textures.load()
        EntityModels.load()
        Terrains.load()
        Skyboxes.load()
        stateMachine = StateMachine()
        timer = Timer()

        errorCallback = GLFWErrorCallback.createPrint().set()
        resizeCallback = GLFWFramebufferSizeCallback.create { _, width, height ->
            window.resize(width, height)
            renderer.resize(width, height)
        }.set(window.id)
        keyCallback = SMFKeyCallback().set(window.id)

        Logger.info("SMF initialized!")
    }

    override fun start() {
        Logger.info("SMF started!")

        window.show()
        gameLoop()

        Logger.info("SMF stopping...")
        cleanup()
        Logger.info("SMF stopped!")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start()
    }
}
