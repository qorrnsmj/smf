package qorrnsmj.smf.editor

import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.light.PointLight
import qorrnsmj.smf.graphic.skybox.Skyboxes
import qorrnsmj.smf.math.Vector3f

internal object EditorScene {
    fun configure(scene: Scene) {
        scene.world.camera.position = Vector3f(0f, 8f, 22f)
        scene.world.camera.setFront(Vector3f(0f, -0.25f, -1f))
        scene.environment.skybox = Skyboxes.SKY1
        scene.environment.skyColor = Vector3f(0.46f, 0.58f, 0.68f)
        scene.world.lights.add(
            PointLight().apply {
                position = Vector3f(0f, 80f, 0f)
                ambient = Vector3f(0.25f, 0.25f, 0.25f)
                diffuse = Vector3f(1f, 1f, 1f)
                specular = Vector3f(1f, 1f, 1f)
                shininess = 32f
                constant = 1f
                linear = 0f
                quadratic = 0f
            }
        )
    }
}
