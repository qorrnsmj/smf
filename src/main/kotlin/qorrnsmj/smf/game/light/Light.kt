package qorrnsmj.smf.game.light

import qorrnsmj.smf.math.Vector3f

abstract class Light(
    var position: Vector3f,
    var ambient: Vector3f,
    var diffuse: Vector3f,
    var specular: Vector3f,
    var shininess: Float,
    var constant: Float,
    var linear: Float,
    var quadratic: Float
)
