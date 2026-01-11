package qorrnsmj.smf.game.terrain

import qorrnsmj.smf.game.terrain.component.TerrainModel
import qorrnsmj.smf.math.Vector3f

open class Terrain(
    val model: TerrainModel,
    val position: Vector3f = Vector3f(0f, 0f, 0f),
) : HeightProvider {
    override fun getHeight(worldX: Float, worldZ: Float): Float {
        val mesh = model.mesh
        val localX = worldX - position.x
        val localZ = worldZ - position.z
        if (localX < 0f || localZ < 0f || localX > mesh.size.x || localZ > mesh.size.y) return position.y

        val cellsX = mesh.gridResolution - 1
        val cellsZ = mesh.gridResolution - 1 // TODO
        val cellSizeX = mesh.size.x / cellsX
        val cellSizeZ = mesh.size.y / cellsZ
        val xIndex = (localX / cellSizeX).toInt().coerceIn(0, cellsX - 1)
        val zIndex = (localZ / cellSizeZ).toInt().coerceIn(0, cellsZ - 1)
        val xCoord = (localX % cellSizeX) / cellSizeX
        val zCoord = (localZ % cellSizeZ) / cellSizeZ

        fun heightAt(x: Int, z: Int) = mesh.heights[x][z]

        val h00 = heightAt(xIndex, zIndex)
        val h10 = heightAt(xIndex + 1, zIndex)
        val h01 = heightAt(xIndex, zIndex + 1)
        val h11 = heightAt(xIndex + 1, zIndex + 1)

        val h0 = h00 * (1 - xCoord) + h10 * xCoord
        val h1 = h01 * (1 - xCoord) + h11 * xCoord
        val height = h0 * (1 - zCoord) + h1 * zCoord

        return position.y + height
    }
}
