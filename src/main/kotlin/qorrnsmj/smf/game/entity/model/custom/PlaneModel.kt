package qorrnsmj.smf.game.entity.model.custom

import qorrnsmj.smf.game.entity.model.EntityLoader
import qorrnsmj.smf.game.entity.model.component.Material
import qorrnsmj.smf.game.entity.model.component.Model

class PlaneModel(id: String, texture: String)
    : Model(id, mesh, Material(diffuseTexture = EntityLoader.loadTexture(texture)),
        hasTransparency = true, useFakeLighting = true) {

    companion object {
        private val mesh = EntityLoader.loadMesh(
            floatArrayOf(
                -0.5f, 0f, -0.5f,
                0.5f, 0f, -0.5f,
                0.5f, 0f, 0.5f,
                -0.5f, 0f, 0.5f
            ),
            floatArrayOf(
                0f, 0f,
                1f, 0f,
                1f, 1f,
                0f, 1f
            ),
            floatArrayOf(
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f,
                0f, 1f, 0f
            ),
            floatArrayOf(
                1f, 0f, 0f,
                1f, 0f, 0f,
                1f, 0f, 0f,
                1f, 0f, 0f,
            ),
            intArrayOf(
                2, 1, 0,
                3, 2, 0
            )
        )
    }
}
