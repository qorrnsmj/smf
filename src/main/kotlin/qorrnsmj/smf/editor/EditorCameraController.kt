package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFW.GLFW_KEY_A
import org.lwjgl.glfw.GLFW.GLFW_KEY_D
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT
import org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL
import org.lwjgl.glfw.GLFW.GLFW_KEY_S
import org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE
import org.lwjgl.glfw.GLFW.GLFW_KEY_W
import org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT
import org.lwjgl.glfw.GLFW.GLFW_PRESS
import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import org.lwjgl.glfw.GLFW.glfwGetKey
import org.lwjgl.glfw.GLFW.glfwGetMouseButton
import qorrnsmj.smf.SMF
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.math.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.asin
import kotlin.math.atan2

internal class EditorCameraController(private val camera: Camera) {
    private var yaw = -90f
    private var pitch = -20f
    private var lastMouseX = 0.0
    private var lastMouseY = 0.0
    private var mouseInitialized = false

    fun update(delta: Float, uiWantsMouse: Boolean) {
        val window = SMF.window.id
        val rightDown = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS

        if (rightDown && !uiWantsMouse) {
            updateMouseLook()
        } else {
            mouseInitialized = false
        }

        if (rightDown && !uiWantsMouse) {
            updateMovement(delta)
        }
    }

    fun syncFromCamera() {
        val front = camera.getFront().normalize()
        pitch = Math.toDegrees(asin(front.y.toDouble())).toFloat()
        yaw = Math.toDegrees(atan2(front.z.toDouble(), front.x.toDouble())).toFloat()
        mouseInitialized = false
    }

    private fun updateMouseLook() {
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(SMF.window.id, x, y)

        if (!mouseInitialized) {
            lastMouseX = x[0]
            lastMouseY = y[0]
            mouseInitialized = true
            return
        }

        val sensitivity = 0.08f
        yaw += ((x[0] - lastMouseX) * sensitivity).toFloat()
        pitch += ((lastMouseY - y[0]) * sensitivity).toFloat()
        pitch = pitch.coerceIn(-89f, 89f)
        lastMouseX = x[0]
        lastMouseY = y[0]
        camera.setFront(front())
    }

    private fun updateMovement(delta: Float) {
        val window = SMF.window.id
        val accelerate = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS ||
            glfwGetKey(window, GLFW_KEY_RIGHT_CONTROL) == GLFW_PRESS
        val speed = if (accelerate) 48f else 12f
        val amount = speed * delta
        val horizontalFront = camera.horizontalFront()
        val right = horizontalFront.cross(Vector3f(0f, 1f, 0f)).normalize()

        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) camera.position = camera.position.add(horizontalFront.scale(amount))
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) camera.position = camera.position.subtract(horizontalFront.scale(amount))
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) camera.position = camera.position.subtract(right.scale(amount))
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) camera.position = camera.position.add(right.scale(amount))
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) camera.position = camera.position.add(Vector3f(0f, amount, 0f))
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) camera.position = camera.position.subtract(Vector3f(0f, amount, 0f))
    }

    private fun front(): Vector3f {
        val yawRad = Math.toRadians(yaw.toDouble())
        val pitchRad = Math.toRadians(pitch.toDouble())
        return Vector3f(
            (cos(yawRad) * cos(pitchRad)).toFloat(),
            sin(pitchRad).toFloat(),
            (sin(yawRad) * cos(pitchRad)).toFloat(),
        ).normalize()
    }
}
