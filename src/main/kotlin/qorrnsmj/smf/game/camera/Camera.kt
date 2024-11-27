package qorrnsmj.smf.game.camera

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.graphic.MVP
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.math.Vector3f
import java.lang.Math.*
import kotlin.math.cos
import kotlin.math.sin

// TODO: もっと変数とか綺麗にする
class Camera(
    var position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var front: Vector3f = Vector3f(0.0f, 0.0f, -1.0f),
    var up: Vector3f = Vector3f(0.0f, 1.0f, 0.0f)
) {
    private var yaw: Float = -90.0f // Y軸の回転角度
    private var pitch: Float = 0.0f // X軸の回転角度
    private val speed: Float = 0.3f // 移動速度
    private val sensitivity: Float = 0.1f // マウスの感度
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var firstMouse: Boolean = true

    fun processKeyboardInput(window: Window) {
        val moveSpeed = speed

        // WASDで移動
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            position += front * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            position -= front * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            position -= right() * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            position += right() * moveSpeed
        }

        // SpaceとShiftで上下移動
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            position += up * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) {
            position -= up * moveSpeed
        }

        // Qでカメラをリセット
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_Q) == GLFW.GLFW_PRESS) {
            position = Vector3f(0.0f, 0.0f, 5.0f)
            front = Vector3f(0.0f, 0.0f, -1.0f)
            up = Vector3f(0.0f, 1.0f, 0.0f)
            yaw = -90.0f
            pitch = 0.0f
        }
    }

    fun processMouseMovement(window: Window) {
        // マウスの現在位置を取得
        val mouseX = DoubleArray(1)
        val mouseY = DoubleArray(1)
        GLFW.glfwGetCursorPos(window.id, mouseX, mouseY)

        // マウスの初期位置を取得
        if (firstMouse) {
            lastX = mouseX[0]
            lastY = mouseY[0]
            firstMouse = false
        }

        // マウスの移動量を計算
        val xOffset = (mouseX[0] - lastX) * sensitivity
        val yOffset = (lastY - mouseY[0]) * sensitivity // 上下は逆になるから引き算
        lastX = mouseX[0]
        lastY = mouseY[0]

        // カメラの向きを更新
        yaw += xOffset.toFloat()
        pitch += yOffset.toFloat()

        // ピッチを制限して、上下に180度回転しないようにする
        if (pitch > 89.0f) pitch = 89.0f
        if (pitch < -89.0f) pitch = -89.0f

        // 新しいfrontベクトルを計算して正規化
        val x = cos(toRadians(yaw.toDouble())) * cos(toRadians(pitch.toDouble()))
        val y = sin(toRadians(pitch.toDouble()))
        val z = sin(toRadians(yaw.toDouble())) * cos(toRadians(pitch.toDouble()))
        front = Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).normalize()
    }

    fun getViewMatrix(): Matrix4f {
        return MVP.getViewMatrix(position, position + front, up)
    }

    private fun right(): Vector3f {
        return front.cross(up).normalize()
    }

    // TODO: Vectorクラスに実装する
    operator fun Vector3f.plus(other: Vector3f): Vector3f {
        return Vector3f(this.x + other.x, this.y + other.y, this.z + other.z)
    }

    operator fun Vector3f.minus(other: Vector3f): Vector3f {
        return Vector3f(this.x - other.x, this.y - other.y, this.z - other.z)
    }

    operator fun Vector3f.times(scalar: Float): Vector3f {
        return Vector3f(this.x * scalar, this.y * scalar, this.z * scalar)
    }
}
