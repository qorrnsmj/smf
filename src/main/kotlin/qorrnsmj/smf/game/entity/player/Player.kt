package qorrnsmj.smf.game.entity.player

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.terrain.HeightProvider
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.window.Window

class Player(
    private val eyeHeight: Float = 5f,
    private val moveSpeed: Float = 0.5f,
    private val jumpSpeed: Float = 2f,
    private val gravity: Float = 0.1f,
) {
    var camera = Camera()
    private var verticalVelocity = 0f
    private var grounded = false

    fun handleInput(window: Window, delta: Float) {
        camera.processMouseMovement(window)
        moveHorizontal(window, delta)
        handleJump(window)
    }

    fun update(delta: Float, heightProvider: HeightProvider) {
        applyGravity(delta, heightProvider)
    }

    private fun moveHorizontal(window: Window, delta: Float) {
        val front = camera.getFront()
        val forward = Vector3f(front.x, 0f, front.z).normalize()
        val right = forward.cross(Vector3f(0f, 1f, 0f)).normalize()
        val velocity = moveSpeed * delta
        var move = Vector3f()
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) move = move.add(forward.scale(velocity))
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) move = move.add(forward.scale(-velocity))
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) move = move.add(right.scale(-velocity))
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) move = move.add(right.scale(velocity))

        camera.position = camera.position.add(move)
    }

    private fun applyGravity(delta: Float, heightProvider: HeightProvider) {
        verticalVelocity -= gravity * delta
        camera.position.y += verticalVelocity

        val groundY = heightProvider.getHeight(camera.position.x, camera.position.z) + eyeHeight
        if (camera.position.y <= groundY) {
            camera.position.y = groundY
            verticalVelocity = 0f
            grounded = true
        } else {
            grounded = false
        }
    }

    private fun handleJump(window: Window) {
        if (grounded && GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            verticalVelocity = jumpSpeed
            grounded = false
        }
    }
}
