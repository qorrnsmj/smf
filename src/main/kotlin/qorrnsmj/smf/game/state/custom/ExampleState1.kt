package qorrnsmj.smf.game.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.SMF.renderer
import qorrnsmj.smf.game.entity.custom.Ship
import qorrnsmj.smf.game.entity.custom.Stall
import qorrnsmj.smf.graphic.render.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.game.state.State

class ExampleState1 : State() {
    private val scene = Scene()
    private val stall = Stall()
    private val ship = Ship()

    override fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        stall.pos = Vector3f(0f, 0f, 0f)
        ship.pos = Vector3f(5f, 0f, 0f)
        ship.scale = Vector3f(20f, 20f, 20f)

        scene.camera.position = Vector3f(0f, 10f, 20f)
        scene.entities.add(ship)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
        //stall.rot.y += 1f
        //ship.rot.y += 1f
    }

    override fun render(alpha: Float) {
        renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
