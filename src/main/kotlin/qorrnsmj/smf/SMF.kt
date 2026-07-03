package qorrnsmj.smf

import org.lwjgl.glfw.GLFW.glfwInit
import org.lwjgl.glfw.GLFW.GLFW_CURSOR
import org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.audio.AudioManager
import qorrnsmj.smf.audio.Audios
import qorrnsmj.smf.core.FixedTimestepGame
import qorrnsmj.smf.core.Timer
import qorrnsmj.smf.editor.EditorApp
import qorrnsmj.smf.game.entity.Entities
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.graphic.terrain.Terrains
import qorrnsmj.smf.graphic.texture.Textures
import qorrnsmj.smf.graphic.render.MasterRenderer
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.state.States
import qorrnsmj.smf.window.SMFKeyCallback
import qorrnsmj.smf.window.Window

object SMF : FixedTimestepGame() {
    private var editorApp: EditorApp? = null
    val isEditorMode: Boolean
        get() = editorApp != null

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
            editorApp?.resize(width, height)
        }.set(window.id)
        keyCallback = SMFKeyCallback().set(window.id)

        Logger.info("SMF initialized!")
    }

    override fun start() {
        startGame()
    }

    private fun loadGameResources() {
        Textures.load()
        EntityModels.load()
        Skyboxes.load()
        Terrains.load()
    }

    private fun startGame() {
        Logger.info("SMF starting...")

        Audios.load()
        loadGameResources()
        Entities.load()

        window.show()
        stateMachine.changeState(States.IN_GAME)

        Logger.info("SMF started!")
        gameloop()
        Logger.info("SMF stopped!")
    }

    private fun startEditor() {
        Logger.info("SMF editor starting...")

        loadGameResources()
        editorApp = EditorApp()

        window.show()
        window.setInputMode(GLFW_CURSOR, GLFW_CURSOR_NORMAL)
        editorApp!!.initialize()

        Logger.info("SMF editor started!")
        gameloop()
        editorApp?.dispose()
        editorApp = null
        Logger.info("SMF editor stopped!")
    }

    override fun input() {
        if (editorApp == null) {
            super.input()
        }
    }

    override fun update(delta: Float) {
        val editor = editorApp
        if (editor != null) {
            editor.update(delta / TARGET_UPS)
        } else {
            super.update(delta)
        }
    }

    override fun render(alpha: Float) {
        val editor = editorApp
        if (editor != null) {
            editor.renderScene(alpha)
        } else {
            super.render(alpha)
        }
    }

    override fun postRender(alpha: Float) {
        editorApp?.renderUi()
    }

    @JvmStatic
    fun main(args: Array<String>) {
        if ("--editor" in args) {
            startEditor()
        } else {
            start()
        }
    }
}
