package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.lwjgl.opengl.GL
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.core.window.Window
import qorrnsmj.smf.graphic.render.Renderer
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.state.States.*

// TODO: オーバーロード減らせない？
abstract class Game {
    protected var running = false
    val window: Window
    val timer: Timer
    val renderer: Renderer
    val stateMachine: StateMachine
    private val errorCallback: GLFWErrorCallback
    private val resizeCallback: GLFWFramebufferSizeCallback

    init {
        Logger.info("Game initializing")

        // Set error callback
        errorCallback = GLFWErrorCallback.createPrint().set()

        // Initialize GLFW
        check(glfwInit()) { "Failed to initialize GLFW!" }

        // Create window, timer and renderer
        window = Window(800, 800, "SMF", true)
        timer = Timer()
        renderer = Renderer.apply { init() }

        // Set state machine
        stateMachine = StateMachine()

        // Set resize callback
        resizeCallback = GLFWFramebufferSizeCallback.create { _, width, height ->
            glfwSetWindowSize(window.id, width, height)
            stateMachine.resize(width, height)
        }.set(window.id)

        running = true
        window.show()
        Logger.info("Game initialized")
    }

    protected abstract fun start()

    protected abstract fun gameLoop()

    protected fun input() {
        stateMachine.input()
    }

    protected fun update() {
        stateMachine.update()
    }

    protected fun update(delta: Float) {
        stateMachine.update(delta)
    }

    protected fun render() {
        stateMachine.render()
    }

    protected fun render(alpha: Float) {
        stateMachine.render(alpha)
    }

    protected fun sync(fps: Int) {
        val lastLoopTime = timer.lastLoopTime
        var now = timer.getTime()
        val targetTime = 1f / fps

        while (now - lastLoopTime < targetTime) {
            Thread.yield()

            // This is optional if you want your game to stop consuming too much
            // CPU but you will loose some accuracy because Thread.sleep(1)
            // could sleep longer than 1 millisecond
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            now = timer.getTime()
        }
    }

    protected fun dispose() {
        /* Dispose renderer */
        //renderer.dispose()

        /* Set empty state to trigger the exit method in the current state */
        stateMachine.changeState(EMPTY)

        /* Release window and its callbacks */
        window.destroy()

        /* Terminate GLFW */
        glfwTerminate()

        /* Release callbacks */
        errorCallback.free()
        resizeCallback.free()
    }

    companion object {
        const val TARGET_FPS = 60
        const val TARGET_UPS = 30

        /**
         * Determines if the OpenGL context supports version 3.2.
         *
         * @return true, if OpenGL context supports version 3.2, else false
         */
        fun isDefaultContext(): Boolean {
            return GL.getCapabilities().OpenGL32
        }
    }
}
