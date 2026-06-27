package qorrnsmj.smf

import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.audio.AudioManager
import qorrnsmj.smf.audio.Audios
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.core.Timer
import qorrnsmj.smf.game.entity.Entities
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.graphic.render.MasterRenderer
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.state.States
import qorrnsmj.smf.window.SMFKeyCallback
import qorrnsmj.smf.window.Window

object SMF : FixedTimestepGame() {
    init {
        Logger.info("SMF initializing...")

        check(glfwInit()) { Logger.error("Failed to initialize GLFW!") }
        window = Window(1920, 1080, "SMF", true)
        renderer = MasterRenderer()
        renderer.resize(window.width, window.height)
        audioManager = AudioManager()
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
        Logger.info("SMF starting...")

        Audios.load()
        Textures.load()
        EntityModels.load()
        Entities.load()
        Skyboxes.load()

        window.show()
        stateMachine.changeState(States.IN_GAME)

        Logger.info("SMF started!")
        gameloop()
        Logger.info("SMF stopped!")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        start()
    }
}
