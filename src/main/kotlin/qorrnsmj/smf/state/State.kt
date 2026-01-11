package qorrnsmj.smf.state

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.player.Player
import qorrnsmj.smf.game.terrain.HeightProvider
import qorrnsmj.smf.graphic.Scene

abstract class State {
    protected lateinit var scene: Scene
    protected var player: Player = Player()
    protected var delta: Float = 1f / 60f

    open fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        scene = Scene()
    }

    open fun input() {
        player.handleInput(SMF.window, delta, scene.terrain as HeightProvider)
    }

    open fun update(delta: Float) {
        this.delta = delta
    }

    open fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    open fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }

    override fun toString(): String {
        return this.javaClass.simpleName
    }
}
