package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.math.Matrix4f

const val MAX_LOCAL_LIGHT_SHADOWS = 30
const val MAX_POINT_LIGHT_SHADOWS = 8

data class ShadowRenderState(
    val enabled: Boolean,
    val lightSpaceMatrix: Matrix4f,
    val depthTextureId: Int,
    val strength: Float = 0f,
    val local: LocalShadowRenderState = LocalShadowRenderState(),
    val point: PointShadowRenderState = PointShadowRenderState(),
)

data class LocalShadowRenderState(
    val enabled: Boolean = false,
    val depthTextureId: Int = 0,
    val count: Int = 0,
    val matrices: List<Matrix4f> = emptyList(),
    val strengths: FloatArray = FloatArray(0),
    val lightShadowIndices: IntArray = IntArray(MAX_LOCAL_LIGHT_SHADOWS) { -1 },
)

data class PointShadowRenderState(
    val enabled: Boolean = false,
    val count: Int = 0,
    val textureIds: IntArray = IntArray(0),
    val strengths: FloatArray = FloatArray(0),
    val lightShadowIndices: IntArray = IntArray(MAX_LOCAL_LIGHT_SHADOWS) { -1 },
    val farPlanes: FloatArray = FloatArray(0),
)
