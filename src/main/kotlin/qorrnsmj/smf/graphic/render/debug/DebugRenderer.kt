package qorrnsmj.smf.graphic.render.debug

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.game.entity.Entity
import qorrnsmj.smf.graphic.render.shader.LineShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.physics.collision.BoxCollider
import qorrnsmj.smf.physics.collision.SphereCollider
import qorrnsmj.smf.util.Cleanable
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unified debug renderer for all types of debug visualizations.
 * Handles collision bounds, physics vectors, lighting debug, and other debug overlays.
 * Follows the TextRenderer pattern for consistent architecture.
 */
class DebugRenderer : Cleanable, Resizable {
    
    private lateinit var shaderProgram: LineShaderProgram
    private var vao: Int = 0
    private var vbo: Int = 0
    private var projectionMatrix: Matrix4f = MVP.getPerspectiveMatrix(16f / 9f)
    
    // Debug feature toggle states
    private var collisionDebugEnabled: Boolean = false
    // Future debug features can be added here:
    // private var velocityVectorsEnabled: Boolean = false
    // private var lightingDebugEnabled: Boolean = false
    // private var physicsInfoEnabled: Boolean = false
    
    // Colors for different debug types
    private val boxColliderColor = Vector4f(0.2f, 0.6f, 1.0f, 1.0f)     // Blue for boxes
    private val sphereColliderColor = Vector4f(0.2f, 1.0f, 0.6f, 1.0f)  // Green for spheres
    // Future colors can be added here:
    // private val velocityVectorColor = Vector4f(1.0f, 1.0f, 0.2f, 1.0f)  // Yellow for velocity
    // private val forceVectorColor = Vector4f(1.0f, 0.2f, 0.2f, 1.0f)     // Red for forces
    
    // Circle approximation for sphere wireframes
    private val circleSegments = 16
    
    // Maximum vertices for batch rendering (conservative estimate)
    private val maxLines = 2000
    private val verticesPerLine = 2
    private val floatsPerVertex = 7 // position (3) + color (4)
    private val maxVertices = maxLines * verticesPerLine * floatsPerVertex
    
    init {
        initializeRenderer()
    }
    
    private fun initializeRenderer() {
        shaderProgram = LineShaderProgram()
        
        // Create VAO and VBO for line rendering
        vao = glGenVertexArrays()
        vbo = glGenBuffers()
        
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        
        // Allocate buffer for dynamic line rendering
        glBufferData(GL_ARRAY_BUFFER, maxVertices * Float.SIZE_BYTES.toLong(), GL_DYNAMIC_DRAW)
        
        // Position attribute (location 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        
        // Color attribute (location 1)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 3 * Float.SIZE_BYTES.toLong())
        glEnableVertexAttribArray(1)
        
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }
    
    // === Collision Debug Functions ===
    
    fun toggleCollisionDebug() {
        collisionDebugEnabled = !collisionDebugEnabled
    }
    
    fun setCollisionDebugEnabled(enable: Boolean) {
        collisionDebugEnabled = enable
    }
    
    fun isCollisionDebugEnabled(): Boolean = collisionDebugEnabled
    
    // === Future Debug Functions (placeholders) ===
    
    // Example: Velocity Vector Debug
    // fun toggleVelocityVectors() {
    //     velocityVectorsEnabled = !velocityVectorsEnabled
    // }
    //
    // fun isVelocityVectorsEnabled(): Boolean = velocityVectorsEnabled
    
    // Example: Lighting Debug
    // fun toggleLightingDebug() {
    //     lightingDebugEnabled = !lightingDebugEnabled
    // }
    //
    // fun isLightingDebugEnabled(): Boolean = lightingDebugEnabled
    
    // Example: Physics Info Debug
    // fun togglePhysicsInfo() {
    //     physicsInfoEnabled = !physicsInfoEnabled
    // }
    //
    // fun isPhysicsInfoEnabled(): Boolean = physicsInfoEnabled
    
    // Example: Entity Hierarchy Debug
    // fun toggleHierarchyDebug() {
    //     hierarchyDebugEnabled = !hierarchyDebugEnabled
    // }
    
    // TODO: Add more debug features here. Key bindings can be:
    // F1: Collision Debug (implemented)
    // F2: Velocity Vectors 
    // F3: Lighting Debug
    // F4: Physics Info
    // F5: Entity Hierarchy
    
    fun isAnyDebugEnabled(): Boolean = collisionDebugEnabled
    // When adding more features, update this method:
    // fun isAnyDebugEnabled(): Boolean = collisionDebugEnabled || velocityVectorsEnabled || lightingDebugEnabled
    
    /**
     * Main render function for all debug visualizations
     */
    fun render(entities: List<Entity>, viewMatrix: Matrix4f) {
        if (!isAnyDebugEnabled()) return
        
        start()
        
        // Set up MVP matrix
        val mvpMatrix = projectionMatrix.multiply(viewMatrix)
        MemoryStack.stackPush().use { stack ->
            val mvpBuffer = stack.mallocFloat(16)
            mvpMatrix.toBuffer(mvpBuffer)
            shaderProgram.loadMvpMatrix(mvpBuffer)
        }
        
        // Generate vertices for all enabled debug features
        val vertices = generateDebugVertices(entities)
        if (vertices.isNotEmpty()) {
            renderVertices(vertices)
        }
        
        stop()
    }
    
    private fun start() {
        shaderProgram.use()
        
        // Enable line rendering settings
        glDisable(GL_DEPTH_TEST) // Render over geometry for debugging
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glLineWidth(2.0f) // Make lines more visible
    }
    
    private fun stop() {
        glLineWidth(1.0f)
        glDisable(GL_BLEND)
        glEnable(GL_DEPTH_TEST)
        glUseProgram(0)
    }
    
    /**
     * Generate vertices for all enabled debug features
     */
    private fun generateDebugVertices(entities: List<Entity>): FloatArray {
        val vertices = mutableListOf<Float>()
        
        // Collision debug wireframes
        if (collisionDebugEnabled) {
            generateCollisionVertices(entities, vertices)
        }
        
        // Future debug features can be added here:
        // if (velocityVectorsEnabled) {
        //     generateVelocityVectors(entities, vertices)
        // }
        
        // if (lightingDebugEnabled) {
        //     generateLightingDebug(entities, vertices)
        // }
        
        return vertices.toFloatArray()
    }
    
    /**
     * Generate collision debug wireframes
     */
    private fun generateCollisionVertices(entities: List<Entity>, vertices: MutableList<Float>) {
        for (entity in entities) {
            val physicsComponent = entity.physicsComponent ?: continue
            val collider = physicsComponent.collider ?: continue
            val worldPos = entity.getWorldPosition()
            
            when (collider) {
                is BoxCollider -> {
                    generateBoxWireframe(worldPos, collider, boxColliderColor, vertices)
                }
                is SphereCollider -> {
                    generateSphereWireframe(worldPos, collider, sphereColliderColor, vertices)
                }
            }
        }
    }
    
    private fun generateBoxWireframe(
        position: Vector3f, 
        box: BoxCollider, 
        color: Vector4f, 
        vertices: MutableList<Float>
    ) {
        val halfWidth = box.width / 2f
        val halfHeight = box.height / 2f
        val halfDepth = box.depth / 2f
        
        // Define 8 corners of the box
        val corners = arrayOf(
            Vector3f(position.x - halfWidth, position.y - halfHeight, position.z - halfDepth), // 0: min corner
            Vector3f(position.x + halfWidth, position.y - halfHeight, position.z - halfDepth), // 1
            Vector3f(position.x + halfWidth, position.y + halfHeight, position.z - halfDepth), // 2
            Vector3f(position.x - halfWidth, position.y + halfHeight, position.z - halfDepth), // 3
            Vector3f(position.x - halfWidth, position.y - halfHeight, position.z + halfDepth), // 4
            Vector3f(position.x + halfWidth, position.y - halfHeight, position.z + halfDepth), // 5
            Vector3f(position.x + halfWidth, position.y + halfHeight, position.z + halfDepth), // 6: max corner
            Vector3f(position.x - halfWidth, position.y + halfHeight, position.z + halfDepth)  // 7
        )
        
        // Define 12 edges of the box (each edge is a line)
        val edges = arrayOf(
            // Bottom face
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
            // Top face  
            intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
            // Vertical edges
            intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
        )
        
        // Add vertices for each edge
        for (edge in edges) {
            val start = corners[edge[0]]
            val end = corners[edge[1]]
            
            // Start vertex
            vertices.addAll(arrayOf(start.x, start.y, start.z, color.x, color.y, color.z, color.w))
            // End vertex
            vertices.addAll(arrayOf(end.x, end.y, end.z, color.x, color.y, color.z, color.w))
        }
    }
    
    private fun generateSphereWireframe(
        position: Vector3f, 
        sphere: SphereCollider, 
        color: Vector4f, 
        vertices: MutableList<Float>
    ) {
        val radius = sphere.radius
        val angleStep = (2.0 * Math.PI / circleSegments).toFloat()
        
        // Generate 3 orthogonal circles (XY, XZ, YZ planes) to represent the sphere
        generateCircle(position, radius, color, vertices, angleStep) { angle ->
            // XY plane circle
            Vector3f(
                position.x + radius * cos(angle),
                position.y + radius * sin(angle), 
                position.z
            )
        }
        
        generateCircle(position, radius, color, vertices, angleStep) { angle ->
            // XZ plane circle
            Vector3f(
                position.x + radius * cos(angle),
                position.y,
                position.z + radius * sin(angle)
            )
        }
        
        generateCircle(position, radius, color, vertices, angleStep) { angle ->
            // YZ plane circle
            Vector3f(
                position.x,
                position.y + radius * cos(angle),
                position.z + radius * sin(angle)
            )
        }
    }
    
    private fun generateCircle(
        center: Vector3f,
        radius: Float,
        color: Vector4f,
        vertices: MutableList<Float>,
        angleStep: Float,
        pointGenerator: (Float) -> Vector3f
    ) {
        for (i in 0 until circleSegments) {
            val angle1 = i * angleStep
            val angle2 = ((i + 1) % circleSegments) * angleStep
            
            val point1 = pointGenerator(angle1)
            val point2 = pointGenerator(angle2)
            
            // Add line segment
            vertices.addAll(arrayOf(point1.x, point1.y, point1.z, color.x, color.y, color.z, color.w))
            vertices.addAll(arrayOf(point2.x, point2.y, point2.z, color.x, color.y, color.z, color.w))
        }
    }
    
    private fun renderVertices(vertices: FloatArray) {
        if (vertices.isEmpty()) return
        
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)
        
        // Upload vertex data
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices)
        
        // Draw lines
        val vertexCount = vertices.size / floatsPerVertex
        glDrawArrays(GL_LINES, 0, vertexCount)
        
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindVertexArray(0)
    }
    
    override fun resize(width: Int, height: Int) {
        projectionMatrix = MVP.getPerspectiveMatrix(width / height.toFloat())
    }
    
    override fun cleanup() {
        shaderProgram.delete()
        glDeleteBuffers(vbo)
        glDeleteVertexArrays(vao)
    }
}
