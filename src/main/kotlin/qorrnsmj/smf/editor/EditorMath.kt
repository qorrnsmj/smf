package qorrnsmj.smf.editor

import qorrnsmj.smf.math.Matrix4f

// TODO: Integrate to Matrix class
internal fun Matrix4f.toFloatArray(): FloatArray = floatArrayOf(
    m00, m10, m20, m30,
    m01, m11, m21, m31,
    m02, m12, m22, m32,
    m03, m13, m23, m33,
)

internal fun identityMatrixArray(): FloatArray = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)
