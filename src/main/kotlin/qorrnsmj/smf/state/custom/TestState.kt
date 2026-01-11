package qorrnsmj.smf.state.custom

import qorrnsmj.smf.game.entity.custom.StallEntity
import qorrnsmj.smf.game.light.PointLight
import qorrnsmj.smf.game.skybox.Skyboxes
import qorrnsmj.smf.game.terrain.Terrains
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.state.State

class TestState : State() {
    private lateinit var pointLight: PointLight
    private lateinit var stall: StallEntity

    override fun start() {
        super.start()

        // init
        stall = StallEntity()
        scene.camera = player.camera
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
        scene.camera.position = Vector3f(0f, 5f, -5f)
        scene.camera.front = Vector3f(0f, 0f, 1f).normalize()
        scene.terrain = Terrains.PLANE
        scene.skybox = Skyboxes.SKY1
        scene.lights.add(pointLight)
        scene.entities.add(stall)
    }
}
