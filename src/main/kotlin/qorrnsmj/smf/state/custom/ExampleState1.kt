package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.Ship
import qorrnsmj.smf.game.entity.custom.Stall
import qorrnsmj.smf.game.entity.custom.Tree
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.graphic.render.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State
import kotlin.math.sin

class ExampleState1 : State() {
    private var elapsedTime = 0f
    private val scene = Scene()
    private val pointLight = PointLight(
        position = Vector3f(0f, 20f, 0f),
        ambient = Vector3f(0.5f, 0.5f, 0.5f),
        diffuse = Vector3f(1f, 1f, 1f),
        specular = Vector3f(1f, 1f, 1f),
        shininess = 32f,
        constant = 1f,
        linear = 0f,
        quadratic = 0f
    )
    private val tree = Tree()
    private val stall = Stall()
    private val ship = Ship()

    override fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        tree.position = Vector3f(0f, 0f, 0f)
        tree.scale = Vector3f(0.5f, 0.5f, 0.5f)
        stall.position = Vector3f(10f, 0f, 0f)
        ship.position = Vector3f(20f, 0f, 0f)
        ship.scale = Vector3f(20f, 20f, 20f)

        scene.camera.position = Vector3f(0f, 0f, 0f)

        scene.lights.add(pointLight)
        scene.entities.add(tree)
        scene.entities.add(ship)
        scene.entities.add(stall)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
        elapsedTime += delta

        pointLight.position = Vector3f(0f, sin(elapsedTime / 5) * 20, 0f)
        //pointLight.position = scene.camera.position
    }

    override fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
