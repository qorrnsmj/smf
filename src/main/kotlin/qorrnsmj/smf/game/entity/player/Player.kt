package qorrnsmj.smf.game.entity.player

import org.lwjgl.glfw.GLFW
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.EntityModels
import qorrnsmj.smf.game.entity.custom.CollisionConfig
import qorrnsmj.smf.game.entity.custom.CollisionShape
import qorrnsmj.smf.game.entity.custom.LivingEntity
import qorrnsmj.smf.game.entity.custom.Transform
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.collision.shape.CapsuleCollider
import qorrnsmj.smf.physics.component.DynamicPhysics
import qorrnsmj.smf.window.Window

class Player(
    private val eyeHeight: Float = 1.7f,
    private val moveSpeed: Float = 0.04f,
    private val jumpSpeed: Float = 0.08f,
    collisionHalfWidth: Float = 0.22f,
    collisionHeight: Float = eyeHeight,
    collisionHalfDepth: Float = 0.22f,
    groundProbeDistance: Float = 0.01f,
) : LivingEntity(
    transform = Transform(position = Vector3f(0f, 0f, 0f)),
    model = EntityModels.EMPTY,
    physicsComponent = DynamicPhysics(collider = CapsuleCollider(minOf(collisionHalfWidth, collisionHalfDepth), collisionHeight)),
    collisionConfig = CollisionConfig(
        shape = CollisionShape.CAPSULE,
        halfWidth = collisionHalfWidth,
        height = collisionHeight,
        halfDepth = collisionHalfDepth,
        groundProbeDistance = groundProbeDistance,
    )
) {
    var camera = Camera()
    private var jumpKeyWasDown = false

    init {
        syncCameraWithEntity()
    }

    fun setEyePosition(eyePosition: Vector3f) {
        localTransform = localTransform.copy(position = Vector3f(eyePosition.x, eyePosition.y - eyeHeight, eyePosition.z))
        physicsComponent.velocity = Vector3f(0f, 0f, 0f)
        syncCameraWithEntity()
    }

    fun setFeetPosition(feetPosition: Vector3f) {
        localTransform = localTransform.copy(position = feetPosition)
        physicsComponent.velocity = Vector3f(0f, 0f, 0f)
        syncCameraWithEntity()
    }

    fun handleInput(window: Window, delta: Float) {
        camera.processMouseMovement(window)
        moveHorizontal(window, delta)
        handleJump(window)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        syncCameraWithEntity()
    }

    fun update() {
        update(0f)
    }

    private fun moveHorizontal(window: Window, delta: Float) {
        val front = camera.getFront()
        val forward = Vector3f(front.x, 0f, front.z).normalize()
        val right = forward.cross(Vector3f(0f, 1f, 0f)).normalize()
        var moveDirection = Vector3f()
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS) moveDirection = moveDirection.add(forward)
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS) moveDirection = moveDirection.add(forward.scale(-1f))
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS) moveDirection = moveDirection.add(right.scale(-1f))
        if (GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS) moveDirection = moveDirection.add(right)

        val physics = physicsComponent
        val horizontalVelocity = if (moveDirection.lengthSquared() > 0f) {
            moveDirection.normalize().scale(moveSpeed * delta)
        } else {
            Vector3f()
        }
        physics.velocity = Vector3f(horizontalVelocity.x, physics.velocity.y, horizontalVelocity.z)
    }

    private fun handleJump(window: Window) {
        val physics = physicsComponent
        val jumpKeyDown = GLFW.glfwGetKey(window.id, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS
        if (physics.isGrounded && jumpKeyDown && !jumpKeyWasDown) {
            physics.velocity = Vector3f(physics.velocity.x, jumpSpeed, physics.velocity.z)
            physics.isGrounded = false
        }
        jumpKeyWasDown = jumpKeyDown
    }

    private fun syncCameraWithEntity() {
        val feet = worldTransform.position
        camera.position = Vector3f(feet.x, feet.y + eyeHeight, feet.z)
    }
}
