package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.math.Matrix4f

data class ShadowRenderState(
    val enabled: Boolean,
    val lightSpaceMatrix: Matrix4f,
    val depthTextureId: Int,
)
