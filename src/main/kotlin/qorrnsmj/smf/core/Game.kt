package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWFramebufferSizeCallback
import org.lwjgl.glfw.GLFWKeyCallback
import qorrnsmj.smf.audio.AudioManager
import qorrnsmj.smf.graphic.render.MasterRenderer
import qorrnsmj.smf.state.StateMachine
import qorrnsmj.smf.window.Window

abstract class Game {
    lateinit var window: Window
    lateinit var renderer: MasterRenderer
    lateinit var stateMachine: StateMachine
    lateinit var errorCallback: GLFWErrorCallback
    lateinit var resizeCallback: GLFWFramebufferSizeCallback
    lateinit var keyCallback: GLFWKeyCallback
    lateinit var audioManager: AudioManager
    protected var running = false

    protected abstract fun start()

    protected abstract fun gameloop()

    protected fun input() {
        stateMachine.input()
    }

    protected fun update() {
        stateMachine.update()
        audioManager.update()
    }

    protected fun render() {
        stateMachine.render()
    }
}
