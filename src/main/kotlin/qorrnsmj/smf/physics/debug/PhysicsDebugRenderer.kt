package qorrnsmj.smf.physics.debug

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.physics.PhysicsComponent
import qorrnsmj.smf.physics.collision.BoxCollider
import qorrnsmj.smf.physics.collision.SphereCollider
import org.lwjgl.glfw.GLFW.glfwGetCurrentContext
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryUtil.NULL

/**
 * Debug visualization renderer for physics components
 * Renders colliders, velocity vectors, and force indicators
 */
object PhysicsDebugRenderer {
    
    private var enabled = false
    private var showColliders = true
    private var showVelocity = true
    private var showForces = false
    private var warnedLegacyUnsupported = false
    
    /**
     * Initialize debug renderer resources
     */
    fun initialize() {
        // Initialize debug rendering resources if needed
    }
    
    /**
     * Toggle debug visualization on/off
     */
    fun toggleDebug() {
        enabled = !enabled
    }
    
    /**
     * Set specific debug visualization options
     */
    fun setDebugOptions(colliders: Boolean = true, velocity: Boolean = true, forces: Boolean = false) {
        showColliders = colliders
        showVelocity = velocity
        showForces = forces
    }
    
    /**
     * Render debug visualization for all physics entities
     */
    fun renderDebug(entities: List<Entity>) {
        if (!enabled) return
        if (glfwGetCurrentContext() == NULL) return
        if (!isLegacyDebugRenderingAvailable()) {
            if (!warnedLegacyUnsupported) {
                Logger.warn("Physics debug rendering is disabled because fixed-function OpenGL calls are unavailable in this context")
                warnedLegacyUnsupported = true
            }
            return
        }
        
        // Save current OpenGL state
        val prevDepthTest = glIsEnabled(GL_DEPTH_TEST)
        val prevBlend = glIsEnabled(GL_BLEND)
        
        // Setup debug rendering state
        glDisable(GL_DEPTH_TEST)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(2.0f)
        
        entities.forEach { entity ->
            entity.physicsComponent?.let { physics ->
                val pos = entity.position
                
                // Render collider bounds
                if (showColliders && physics.collider != null) {
                    renderCollider(pos, physics.collider!!)
                }
                
                // Render velocity vector
                if (showVelocity && physics.velocity.length() > 0.1f) {
                    renderVelocityVector(pos, physics.velocity)
                }
                
                // Render force indicators
                if (showForces) {
                    renderForceIndicators(pos, physics)
                }
            }
        }
        
        // Restore OpenGL state
        if (prevDepthTest) glEnable(GL_DEPTH_TEST) else glDisable(GL_DEPTH_TEST)
        if (!prevBlend) glDisable(GL_BLEND)
        glLineWidth(1.0f)
    }

    private fun isLegacyDebugRenderingAvailable(): Boolean {
        val caps = try {
            GL.getCapabilities()
        } catch (_: IllegalStateException) {
            return false
        }

        return caps.glBegin != NULL &&
            caps.glEnd != NULL &&
            caps.glVertex3f != NULL &&
            caps.glColor4f != NULL
    }
    
    /**
     * Render collider bounds
     */
    private fun renderCollider(position: Vector3f, collider: qorrnsmj.smf.physics.collision.Collider) {
        when (collider) {
            is SphereCollider -> renderSphereCollider(position, collider.radius)
            is BoxCollider -> renderBoxCollider(position, collider.width, collider.height, collider.depth)
        }
    }
    
    /**
     * Render sphere collider wireframe
     */
    private fun renderSphereCollider(center: Vector3f, radius: Float) {
        glColor4f(0.0f, 1.0f, 0.0f, 0.7f)  // Green
        glBegin(GL_LINES)
        
        val segments = 16
        val angleStep = (2.0 * Math.PI / segments).toFloat()
        
        // Draw horizontal circle
        for (i in 0 until segments) {
            val angle1 = i * angleStep
            val angle2 = (i + 1) * angleStep
            
            glVertex3f(
                center.x + radius * kotlin.math.cos(angle1),
                center.y,
                center.z + radius * kotlin.math.sin(angle1)
            )
            glVertex3f(
                center.x + radius * kotlin.math.cos(angle2),
                center.y,
                center.z + radius * kotlin.math.sin(angle2)
            )
        }
        
        // Draw vertical circle (YZ plane)
        for (i in 0 until segments) {
            val angle1 = i * angleStep
            val angle2 = (i + 1) * angleStep
            
            glVertex3f(
                center.x,
                center.y + radius * kotlin.math.cos(angle1),
                center.z + radius * kotlin.math.sin(angle1)
            )
            glVertex3f(
                center.x,
                center.y + radius * kotlin.math.cos(angle2),
                center.z + radius * kotlin.math.sin(angle2)
            )
        }
        
        glEnd()
    }
    
    /**
     * Render box collider wireframe
     */
    private fun renderBoxCollider(center: Vector3f, width: Float, height: Float, depth: Float) {
        glColor4f(0.0f, 0.0f, 1.0f, 0.7f)  // Blue
        
        val halfW = width / 2f
        val halfH = height / 2f
        val halfD = depth / 2f
        
        glBegin(GL_LINES)
        
        // Bottom face
        glVertex3f(center.x - halfW, center.y - halfH, center.z - halfD)
        glVertex3f(center.x + halfW, center.y - halfH, center.z - halfD)
        
        glVertex3f(center.x + halfW, center.y - halfH, center.z - halfD)
        glVertex3f(center.x + halfW, center.y - halfH, center.z + halfD)
        
        glVertex3f(center.x + halfW, center.y - halfH, center.z + halfD)
        glVertex3f(center.x - halfW, center.y - halfH, center.z + halfD)
        
        glVertex3f(center.x - halfW, center.y - halfH, center.z + halfD)
        glVertex3f(center.x - halfW, center.y - halfH, center.z - halfD)
        
        // Top face
        glVertex3f(center.x - halfW, center.y + halfH, center.z - halfD)
        glVertex3f(center.x + halfW, center.y + halfH, center.z - halfD)
        
        glVertex3f(center.x + halfW, center.y + halfH, center.z - halfD)
        glVertex3f(center.x + halfW, center.y + halfH, center.z + halfD)
        
        glVertex3f(center.x + halfW, center.y + halfH, center.z + halfD)
        glVertex3f(center.x - halfW, center.y + halfH, center.z + halfD)
        
        glVertex3f(center.x - halfW, center.y + halfH, center.z + halfD)
        glVertex3f(center.x - halfW, center.y + halfH, center.z - halfD)
        
        // Vertical edges
        glVertex3f(center.x - halfW, center.y - halfH, center.z - halfD)
        glVertex3f(center.x - halfW, center.y + halfH, center.z - halfD)
        
        glVertex3f(center.x + halfW, center.y - halfH, center.z - halfD)
        glVertex3f(center.x + halfW, center.y + halfH, center.z - halfD)
        
        glVertex3f(center.x + halfW, center.y - halfH, center.z + halfD)
        glVertex3f(center.x + halfW, center.y + halfH, center.z + halfD)
        
        glVertex3f(center.x - halfW, center.y - halfH, center.z + halfD)
        glVertex3f(center.x - halfW, center.y + halfH, center.z + halfD)
        
        glEnd()
    }
    
    /**
     * Render velocity vector
     */
    private fun renderVelocityVector(position: Vector3f, velocity: Vector3f) {
        glColor4f(1.0f, 1.0f, 0.0f, 1.0f)  // Yellow
        
        val scale = 0.5f  // Scale factor for visualization
        val endPos = Vector3f(
            position.x + velocity.x * scale,
            position.y + velocity.y * scale,
            position.z + velocity.z * scale
        )
        
        glBegin(GL_LINES)
        glVertex3f(position.x, position.y, position.z)
        glVertex3f(endPos.x, endPos.y, endPos.z)
        glEnd()
    }
    
    /**
     * Render force indicators
     */
    private fun renderForceIndicators(position: Vector3f, physics: PhysicsComponent) {
        // For now, just render gravity if enabled
        if (physics.useGravity) {
            glColor4f(1.0f, 0.0f, 0.0f, 0.8f)  // Red for gravity
            
            glBegin(GL_LINES)
            glVertex3f(position.x, position.y, position.z)
            glVertex3f(position.x, position.y - 2.0f, position.z)  // Downward arrow
            glEnd()
        }
    }
    
    /**
     * Cleanup debug renderer resources
     */
    fun cleanup() {
        // Cleanup resources if needed
    }
}
