package qorrnsmj.smf.state.custom

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.entity.custom.solarsystem.EarthEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.JupiterEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.SaturnRingEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.MarsEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.MercuryEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.NeptuneEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.PlanetEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.SaturnEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.SunEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.UranusEntity
import qorrnsmj.smf.game.entity.custom.solarsystem.VenusEntity
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.graphic.Scene
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State

class SolarSystemState : State() {
    private lateinit var scene: Scene
    private var elapsedTime = 0f
    private val sunLight = PointLight().apply {
        position = Vector3f(0f, 0f, 0f)
        ambient = Vector3f(0.2f, 0.2f, 0.2f)
        diffuse = Vector3f(1.2f, 1.2f, 1.2f)
        specular = Vector3f(0f, 0f, 0f)
        shininess = 0f
        constant = 1.0f
        linear = 0f
        quadratic = 0f
    }
    private val planets = mutableListOf<PlanetEntity>(
        SunEntity().apply { scale = Vector3f(5.0f, 5.0f, 5.0f) },
        MercuryEntity().apply { scale = Vector3f(0.38f, 0.38f, 0.38f) },
        VenusEntity().apply { scale = Vector3f(0.95f, 0.95f, 0.95f) },
        EarthEntity().apply { scale = Vector3f(1.0f, 1.0f, 1.0f) },
        MarsEntity().apply { scale = Vector3f(0.53f, 0.53f, 0.53f) },
        JupiterEntity().apply { scale = Vector3f(11f, 11f, 11f) },
        SaturnEntity().apply {
            val scale = Vector3f(9.5f, 9.5f, 9.5f)
            this.scale = scale
            this.children.add(SaturnRingEntity().apply {
                this.scale = Vector3f(scale.x * 3.3f, scale.y * 3.3f, scale.z * 3.3f)
            })
        },
        UranusEntity().apply { scale = Vector3f(4.0f, 4.0f, 4.0f) },
        NeptuneEntity().apply { scale = Vector3f(3.9f, 3.9f, 3.9f) }
    )

    override fun start() {
        scene = Scene()
        elapsedTime = 0f

        scene.camera.position = Vector3f(40f, 40f, 40f)
        scene.lights.add(sunLight)
        planets.forEach {
            it.position = Vector3f(it.sunDistance, 0f, 0f)
            scene.entities.add(it)
        }

        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED)
    }

    override fun input() {
        scene.camera.processKeyboardInput(SMF.window)
        scene.camera.processMouseMovement(SMF.window)
    }

    override fun update(delta: Float) {
        elapsedTime += delta / 50f
        val sunPos = Vector3f(0f, 0f, 0f)

        planets.forEachIndexed { i, it ->
            it.spin(delta)
            it.orbit(sunPos, elapsedTime + i * i * i)
            it.rotation.x = it.axialTilt

            if (it is SaturnEntity) {
                val ring = it.children[0]
                ring.position = it.position
                ring.rotation = it.rotation
            }
        }
    }

    override fun render(alpha: Float) {
        SMF.renderer.render(scene)
    }

    override fun stop() {
        SMF.window.setInputMode(GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL)
    }
}
