package qorrnsmj.smf.game.entity.player

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.LivingEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.window.Window

class Player(
    private val eyeHeight: Float = 5f,
    private val moveSpeed: Float = 0.5f,
    private val jumpSpeed: Float = 2f,
    collisionHalfWidth: Float = 0.5f,
    collisionHeight: Float = eyeHeight,
    collisionHalfDepth: Float = 0.5f,
    groundProbeDistance: Float = 0.05f,
) : LivingEntity(
    transform = Transform(position = Vector3f(0f, 0f, 0f)),
    model = EntityModels.EMPTY,
    physicsComponent = DynamicPhysics(collider = null),
    collisionHalfWidth = collisionHalfWidth,
    collisionHeight = collisionHeight,
    collisionHalfDepth = collisionHalfDepth,
    groundProbeDistance = groundProbeDistance
) {
    var camera = Camera()

    init {
        syncCameraWithEntity()
    }

    fun setEyePosition(eyePosition: Vector3f) {
        localTransform = localTransform.copy(position = Vector3f(eyePosition.x, eyePosition.y - eyeHeight, eyePosition.z))
        physicsComponent.velocity = Vector3f(0f, 0f, 0f)
        syncCameraWithEntity()
    }

    fun handleInput(window: Window, delta: Float) {
        camera.processMouseMovement(window)
        moveHorizontal(window, delta)
        handleJump(window)
    }

    fun update() {
        syncCameraWithEntity()
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

        localTransform = localTransform.copy(position = localTransform.position.add(move))
        syncCameraWithEntity()
    }

    private fun handleJump(window: Window) {
        val physics = physicsComponent
        if (physics.isGrounded && GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS) {
            physics.velocity = Vector3f(physics.velocity.x, jumpSpeed, physics.velocity.z)
            physics.isGrounded = false
        }
    }

    private fun syncCameraWithEntity() {
        val feet = worldTransform.position
        camera.position = Vector3f(feet.x, feet.y + eyeHeight, feet.z)
    }
}
