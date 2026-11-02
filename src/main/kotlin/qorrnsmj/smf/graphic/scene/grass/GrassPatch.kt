package qorrnsmj.smf.graphic.scene.grass
import qorrnsmj.smf.graphic.scene.terrain.Terrain

import qorrnsmj.smf.graphic.scene.terrain.HeightProvider
import qorrnsmj.smf.math.Vector2f
import qorrnsmj.smf.math.Vector3f
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.random.Random

data class GrassInstance(
    val position: Vector3f,
    val scale: Float,
    val rotation: Float,
    val phase: Float,
    val tint: Float,
)

enum class FoliageType {
    GRASS,
    FLOWERS,
}

class GrassPatch(
    instances: List<GrassInstance>,
    val foliageType: FoliageType = FoliageType.GRASS,
) {
    val instances: List<GrassInstance> = instances.toList()
    var windDirection = Vector2f(1f, 0.35f)
    var windStrength = DEFAULT_WIND_STRENGTH
    var windSpeed = 1.6f
    var drawDistance = 60f
    var timeSeconds = 0f
        private set

    fun update(deltaSeconds: Float) {
        require(deltaSeconds.isFinite() && deltaSeconds >= 0f)
        timeSeconds += deltaSeconds
    }

    companion object {
        const val DEFAULT_WIND_STRENGTH = 0.4f

        fun scatter(
            terrain: HeightProvider,
            origin: Vector2f,
            size: Vector2f,
            spacing: Float = 0.45f,
            seed: Int = 1,
            foliageType: FoliageType = FoliageType.GRASS,
            scaleRange: ClosedFloatingPointRange<Float> = 0.45f..0.8f,
        ): GrassPatch {
            require(origin.x.isFinite() && origin.y.isFinite())
            require(size.x.isFinite() && size.x > 0f && size.y.isFinite() && size.y > 0f)
            require(spacing.isFinite() && spacing > 0f)
            require(scaleRange.start.isFinite() && scaleRange.endInclusive.isFinite())
            require(scaleRange.start > 0f && scaleRange.endInclusive >= scaleRange.start)
            val columns = ceil(size.x / spacing).toInt()
            val rows = ceil(size.y / spacing).toInt()
            require(columns.toLong() * rows <= 100_000) { "Use smaller grass patches or wider spacing" }
            val random = Random(seed)
            val instances = ArrayList<GrassInstance>(columns * rows)
            for (z in 0 until rows) {
                for (x in 0 until columns) {
                    val worldX = origin.x + (x + 0.1f + random.nextFloat() * 0.8f) * size.x / columns
                    val worldZ = origin.y + (z + 0.1f + random.nextFloat() * 0.8f) * size.y / rows
                    instances += GrassInstance(
                        position = Vector3f(worldX, terrain.getHeight(worldX, worldZ) - 0.02f, worldZ),
                        scale = scaleRange.start + random.nextFloat() *
                            (scaleRange.endInclusive - scaleRange.start),
                        rotation = random.nextFloat() * (2f * PI.toFloat()),
                        phase = random.nextFloat() * (2f * PI.toFloat()),
                        tint = 0.85f + random.nextFloat() * 0.25f,
                    )
                }
            }
            return GrassPatch(instances, foliageType)
        }
    }
}
