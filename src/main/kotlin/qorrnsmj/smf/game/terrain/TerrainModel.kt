package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.model.component.Mesh

/**
 * Terrain専用のモデルクラス
 */
data class TerrainModel(
    val name: String,
    val mesh: Mesh,
    val material: TerrainMaterial
)

