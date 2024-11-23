package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.lwjgl.glfw.GLFWKeyCallback
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.render.MasterRenderer
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.state.States.*

abstract class Game {
    lateinit var window: Window
    lateinit var renderer: MasterRenderer
    lateinit var stateMachine: StateMachine
    lateinit var errorCallback: GLFWErrorCallback
    lateinit var resizeCallback: GLFWFramebufferSizeCallback
    lateinit var keyCallback: GLFWKeyCallback
    protected var running = false

    protected abstract fun start()

    protected abstract fun gameLoop()

    protected fun input() {
        stateMachine.input()
    }

    protected fun update() {
        stateMachine.update()
    }

    protected fun render() {
        stateMachine.render()
    }

    protected fun cleanup() {
        running = false
        window.hide()

        errorCallback.free()
        resizeCallback.free()
        keyCallback.free()

        stateMachine.changeState(EMPTY)
        renderer.cleanup()
        window.cleanup()

        glfwTerminate()
        Logger.info("Game cleaned up!")
    }
}
