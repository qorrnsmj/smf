package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.NormCubeEntity
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.entity.custom.TestPlaneEntity
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State

class ExampleState1 : State() {
    private var elapsedTime = 0f
    private lateinit var scene: Scene
    private lateinit var stall: StallEntity
    private lateinit var normCube: NormCubeEntity
    private lateinit var plane: TestPlaneEntity
    private lateinit var pointLight1: PointLight
    private lateinit var pointLight2: PointLight

    override fun start() {
        // init
        elapsedTime = 0f
        scene = Scene()
        stall = StallEntity()
        normCube = NormCubeEntity()
        plane = TestPlaneEntity()
        pointLight1 = PointLight().apply {
            position = Vector3f(0f, 20f, 0f)
            ambient = Vector3f(0.1f, 0.1f, 0.1f)
            diffuse = Vector3f(1f, 1f, 1f)
            specular = Vector3f(1f, 1f, 1f)
            shininess = 32f
            constant = 1f
            linear = 0f
            quadratic = 0f
        }

        // setup
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
        scene.camera.position = Vector3f(5f, 5f, 0f)

        // transform
        stall.position = Vector3f(0f, 0f, 0f)
        normCube.position = Vector3f(10f, 3f, 0f)
        plane.position = Vector3f(20f, 3f, 0f)

        // add
        scene.terrains.add(Terrain("test"))
        scene.lights.add(pointLight1)
        scene.entities.add(stall)
        scene.entities.add(normCube)
        scene.entities.add(plane)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
        elapsedTime += delta

        stall.move()
        stall.fruits.position.y += 0.01f
        plane.rotation.y += 1f

        //pointLight2.position = scene.camera.position
    }

    override fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
