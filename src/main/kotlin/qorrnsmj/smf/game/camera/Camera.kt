package qorrnsmj.smf.game.camera

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.window.Window
import qorrnsmj.smf.math.Vector3f
import java.lang.Math.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.asin
import kotlin.math.atan2

// TODO:
//  - もっと変数とか綺麗にする
//  - ムービー用にターゲットに対して視点固定とかできるようにする
class Camera(
    var position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f),
    var up: Vector3f = Vector3f(0.0f, 1.0f, 0.0f)
) {
    private var yaw: Float = -90.0f // Y軸の回転角度
    private var pitch: Float = 0.0f // X軸の回転角度
    private val speed: Float = 0.3f // 移動速度
    private val sensitivity: Float = 0.1f // マウスの感度
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var mouseInitialized: Boolean = false

    fun getFront(): Vector3f {
        val x = cos(toRadians(yaw.toDouble())) * cos(toRadians(pitch.toDouble()))
        val y = sin(toRadians(pitch.toDouble()))
        val z = sin(toRadians(yaw.toDouble())) * cos(toRadians(pitch.toDouble()))
        return Vector3f(x.toFloat(), y.toFloat(), z.toFloat()).normalize()
    }

    fun setFront(f: Vector3f) {
        val normalized = f.normalize()
        pitch = toDegrees(asin(normalized.y.toDouble())).toFloat()
        yaw = toDegrees(atan2(normalized.z.toDouble(), normalized.x.toDouble())).toFloat()

        mouseInitialized = false
    }

    // TODO: 別のクラスにあるべきじゃない？
    fun processKeyboardInput(window: Window) {
        val moveSpeed = speed
        val horizontalFront = getFront().let { Vector3f(it.x, 0.0f, it.z).normalize() }
        val horizontalRight = horizontalFront.cross(Vector3f(0.0f, 1.0f, 0.0f)).normalize()

        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) {
            position += horizontalFront * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) {
            position -= horizontalFront * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) {
            position -= horizontalRight * moveSpeed
        }
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) {
            position += horizontalRight * moveSpeed
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
            up = Vector3f(0.0f, 1.0f, 0.0f)
            yaw = -90.0f
            pitch = 0.0f
        }
    }

    fun processMouseMovement(window: Window) {
        val mouseX = DoubleArray(1)
        val mouseY = DoubleArray(1)
        GLFW.glfwGetCursorPos(window.id, mouseX, mouseY)

        if (!mouseInitialized) {
            lastX = mouseX[0]
            lastY = mouseY[0]
            mouseInitialized = true
            return
        }

        val xOffset = (mouseX[0] - lastX) * sensitivity
        val yOffset = (lastY - mouseY[0]) * sensitivity

        lastX = mouseX[0]
        lastY = mouseY[0]

        yaw += xOffset.toFloat()
        pitch += yOffset.toFloat()

        pitch = pitch.coerceIn(-89.0f, 89.0f)
    }

    fun getViewMatrix(): Matrix4f {
        return MVP.getViewMatrix(position, position + getFront(), up)
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
