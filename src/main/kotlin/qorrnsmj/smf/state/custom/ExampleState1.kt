package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.NormCubeEntity
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.graphic.render.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State

class ExampleState1 : State() {
    private var elapsedTime = 0f
    private val scene = Scene()
    private val stall = StallEntity()
    private val normCube = NormCubeEntity()
    private val pointLight1 = PointLight(
        position = Vector3f(0f, 20f, 0f),
        ambient = Vector3f(1f, 1f, 1f),
        diffuse = Vector3f(1f, 1f, 1f),
        specular = Vector3f(1f, 1f, 1f),
        shininess = 32f,
        constant = 1f,
        linear = 0f,
        quadratic = 0f
    )
    private val pointLight2 = PointLight(
        position = Vector3f(0f, 20f, 0f),
        ambient = Vector3f(0.5f, 0.5f, 0.5f),
        diffuse = Vector3f(1f, 1f, 1f),
        specular = Vector3f(1f, 1f, 1f),
        shininess = 32f,
        constant = 1f,
        linear = 0f,
        quadratic = 0f
    )

    override fun start() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)

        stall.position = Vector3f(10f, 0f, 0f)
        normCube.position = Vector3f(0f, 0f, 0f)

        // FIXME: 効いてなくない？
        scene.camera.position = Vector3f(5f, 5f, 0f)
        scene.camera.front = Vector3f(0f, 10f, 10f)

        scene.lights.add(pointLight1)
        //scene.lights.add(pointLight2)
        scene.entities.add(stall)
        scene.entities.add(normCube)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
        elapsedTime += delta

        stall.move()
        stall.fruits.position.y += 0.01f

        //pointLight2.position = scene.camera.position
        //pointLight1.position = Vector3f(0f, sin(elapsedTime / 5) * 20, 0f)
    }

    override fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
