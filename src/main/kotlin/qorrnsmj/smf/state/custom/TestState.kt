package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.game.skybox.Skyboxes
import qorrnsmj.smf.game.terrain.Terrains
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State

class TestState : State() {
    private lateinit var scene: Scene
    private lateinit var stall: StallEntity
    private lateinit var pointLight: PointLight

    override fun start() {
        // init
        scene = Scene()
        stall = StallEntity()
        pointLight = PointLight().apply {
            position = Vector3f(0f, 10f, 0f)
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
        scene.camera.position = Vector3f(0f, 5f, -5f)
        scene.camera.front = Vector3f(0f, 0f, 1f).normalize()
        scene.skybox = Skyboxes.SKY1

        // add
        scene.terrains.add(Terrains.DEFAULT.apply {
            position.x = -200f
            position.y = -20f
            position.z = -200f
        })
        scene.lights.add(pointLight)
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
