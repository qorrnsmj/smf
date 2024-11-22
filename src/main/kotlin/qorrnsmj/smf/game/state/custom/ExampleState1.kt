package qorrnsmj.smf.game.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.Ship
import qorrnsmj.smf.game.entity.custom.Stall
import qorrnsmj.smf.game.entity.custom.Tree
import qorrnsmj.smf.graphic.render.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.game.state.State

class ExampleState1 : State() {
    private val scene = Scene()
    private val tree = Tree()
    private val stall = Stall()
    private val ship = Ship()

    override fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        tree.pos = Vector3f(0f, 0f, 0f)
        stall.pos = Vector3f(10f, 0f, 0f)
        ship.pos = Vector3f(20f, 0f, 0f)

        tree.scale = Vector3f(0.5f, 0.5f, 0.5f)
        ship.scale = Vector3f(20f, 20f, 20f)

        scene.camera.position = Vector3f(0f, 0f, 0f)
        scene.entities.add(tree)
        scene.entities.add(ship)
        scene.entities.add(stall)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
    }

    override fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
