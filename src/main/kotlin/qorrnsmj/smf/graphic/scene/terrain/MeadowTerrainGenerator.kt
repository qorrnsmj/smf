package qorrnsmj.smf.graphic.scene.terrain

import qorrnsmj.smf.graphic.resource.buffer.TextureBufferObject
import qorrnsmj.smf.graphic.scene.terrain.component.SingleTexture
import qorrnsmj.smf.math.Vector3f
import kotlin.math.floor

object MeadowTerrainGenerator {
    const val SIZE = 160f
    const val RESOLUTION = 129

    fun create(seed: Int, texture: TextureBufferObject): Terrain = TerrainLoader.loadHeightGridModel(
        sizeX = SIZE,
        sizeY = SIZE,
        heightGrid = generateHeightGrid(RESOLUTION, seed),
        position = Vector3f(-SIZE / 2f, 0f, -SIZE / 2f),
        textureMode = SingleTexture(texture),
    )

    fun generateHeightGrid(resolution: Int = RESOLUTION, seed: Int): Array<FloatArray> {
        require(resolution >= 2)
        val heights = Array(resolution) { FloatArray(resolution) }
        val step = SIZE / (resolution - 1)
        for (x in 0 until resolution) {
            for (z in 0 until resolution) {
                val worldX = x * step
                val worldZ = z * step
                val broad = valueNoise(worldX * 0.012f, worldZ * 0.012f, seed)
                val detail = fractalNoise(worldX * 0.026f, worldZ * 0.026f, seed + 71, 3)
                heights[x][z] = broad * 3.2f + detail * 0.9f
            }
        }

        val centerHeight = heights[resolution / 2][resolution / 2]
        heights.forEach { row ->
            for (z in row.indices) row[z] -= centerHeight
        }
        return heights
    }

    private fun fractalNoise(x: Float, z: Float, seed: Int, octaves: Int): Float {
        var frequency = 1f
        var amplitude = 1f
        var total = 0f
        var totalAmplitude = 0f
        repeat(octaves) { octave ->
            total += valueNoise(x * frequency, z * frequency, seed + octave * 101) * amplitude
            totalAmplitude += amplitude
            frequency *= 2f
            amplitude *= 0.42f
        }
        return total / totalAmplitude
    }

    private fun valueNoise(x: Float, z: Float, seed: Int): Float {
        val x0 = floor(x).toInt()
        val z0 = floor(z).toInt()
        val tx = fade(x - x0)
        val tz = fade(z - z0)
        val a = lerp(hash(x0, z0, seed), hash(x0 + 1, z0, seed), tx)
        val b = lerp(hash(x0, z0 + 1, seed), hash(x0 + 1, z0 + 1, seed), tx)
        return lerp(a, b, tz)
    }

    private fun hash(x: Int, z: Int, seed: Int): Float {
        var value = x.toLong() * 0x632BE59BD9B4E019L + z.toLong() * 0x9E3779B97F4A7C15UL.toLong() + seed
        value = (value xor (value ushr 30)) * 0xBF58476D1CE4E5B9UL.toLong()
        value = (value xor (value ushr 27)) * 0x94D049BB133111EBUL.toLong()
        value = value xor (value ushr 31)
        return ((value ushr 40).toInt() / 0xFFFFFF.toFloat()) * 2f - 1f
    }

    private fun fade(value: Float): Float = value * value * value * (value * (value * 6f - 15f) + 10f)

    private fun lerp(a: Float, b: Float, amount: Float): Float = a + (b - a) * amount
}
