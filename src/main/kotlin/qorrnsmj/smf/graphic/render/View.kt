package qorrnsmj.smf.graphic.render

import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f

// Cameraにする？それともCameraは別で作る？
object View {
    fun getMatrix(eye: Vector3f, center: Vector3f, up: Vector3f): Matrix4f {
        // カメラの向いてる方向のベクトルと、上方向のベクトルを正規化したもの
        val forward = center.subtract(eye).normalize()
        var upside = up.normalize()

        // forwardとupの外積を求める。これはカメラの右方向へのベクトルを表す
        val side = forward.cross(upside).normalize()

        // sideとforwardの外積を求める。これはカメラの上方向へのベクトルを表す (upベクトルの再計算)
        // -> 入力として与えられたupベクトルは必ずしもカメラの視線（forwardベクトル）に対して完全に直交しているとは限らない
        // そのため、forwardベクトルと直交するようにupベクトルを再計算する必要がある
        upside = side.cross(forward)

        // カメラ座標変換用の回転行列、つまりview行列 (ワールド座標軸 -> カメラ座標軸)
        // | Xx Xy Xz 0 | (x軸)
        // | Yx Yy Yz 0 | (y軸)
        // | Zx Zy Zz 0 | (z軸)
        // | 0  0  0  1 |
        val viewMatrix = Matrix4f(
            Vector4f(side.x, upside.x, -forward.x, 0f),
            Vector4f(side.y, upside.y, -forward.y, 0f),
            Vector4f(side.z, upside.z, -forward.z, 0f),
            Vector4f(0f, 0f, 0f, 1f)
        )

        // カメラの位置を原点に移動させる。(ビュー行列と平行移動行列を合成)
        return viewMatrix.multiply(Matrix4f(
            Vector4f(1f, 0f, 0f, 0f),
            Vector4f(0f, 1f, 0f, 0f),
            Vector4f(0f, 0f, 1f, 0f),
            Vector4f(-eye.x, -eye.y, -eye.z, 1f))
        )
    }
}
