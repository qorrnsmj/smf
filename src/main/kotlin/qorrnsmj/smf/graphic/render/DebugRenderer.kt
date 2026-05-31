package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL11C
import org.lwjgl.opengl.GL15C
import org.lwjgl.opengl.GL20C
import org.lwjgl.opengl.GL30C
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.render.shader.LineShaderProgram
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.physics.collision.data.AABB
import qorrnsmj.smf.physics.collision.shape.BoxCollider
import qorrnsmj.smf.physics.collision.shape.SphereCollider
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import kotlin.math.cos
import kotlin.math.sin

/**
 * Unified debug renderer for all types of debug visualizations.
 * Handles collision bounds, physics vectors, lighting debug, and other debug overlays.
 * Follows the TextRenderer pattern for consistent architecture.
 */
class DebugRenderer : Resizable {

    private lateinit var shaderProgram: LineShaderProgram
    private var vao: Int = 0
    private var vbo: Int = 0
    private var projectionMatrix: Matrix4f = MVP.getPerspectiveMatrix(16f / 9f)

    private var collisionDebugEnabled: Boolean = false

    private val boxColliderColor = Vector4f(0.2f, 0.6f, 1.0f, 1.0f)
    private val sphereColliderColor = Vector4f(0.2f, 1.0f, 0.6f, 1.0f)
    private val playerBoundsColor = Vector4f(1.0f, 0.35f, 0.35f, 1.0f)

    private val circleSegments = 16

    private val maxLines = 8192
    private val verticesPerLine = 2
    private val floatsPerVertex = 7 // position (3) + color (4)
    private val maxVertices = maxLines * verticesPerLine * floatsPerVertex
    private var playerBounds: AABB? = null

    init {
        initializeRenderer()
    }

    private fun initializeRenderer() {
        shaderProgram = LineShaderProgram()

        // Create VAO and VBO for line rendering
        vao = GL30C.glGenVertexArrays()
        vbo = GL15C.glGenBuffers()

        GL30C.glBindVertexArray(vao)
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)

        GL15C.glBufferData(GL15C.GL_ARRAY_BUFFER, maxVertices * Float.SIZE_BYTES.toLong(), GL15C.GL_DYNAMIC_DRAW)

        // Position attribute (location 0)
        GL20C.glVertexAttribPointer(0, 3, GL11C.GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        GL20C.glEnableVertexAttribArray(0)

        // Color attribute (location 1)
        GL20C.glVertexAttribPointer(1, 4, GL11C.GL_FLOAT, false, 7 * Float.SIZE_BYTES, 3 * Float.SIZE_BYTES.toLong())
        GL20C.glEnableVertexAttribArray(1)

        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)
    }

    // === Collision Debug Functions ===

    fun toggleCollisionDebug() {
        collisionDebugEnabled = !collisionDebugEnabled
    }

    fun setCollisionDebugEnabled(enable: Boolean) {
        collisionDebugEnabled = enable
    }

    fun isCollisionDebugEnabled(): Boolean = collisionDebugEnabled

    @Suppress("unused")
    fun setPlayerBounds(bounds: AABB?) {
        playerBounds = bounds
    }

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

        GL11C.glDisable(GL11C.GL_DEPTH_TEST)
        GL11C.glEnable(GL11C.GL_BLEND)
        GL11C.glBlendFunc(GL11C.GL_SRC_ALPHA, GL11C.GL_ONE_MINUS_SRC_ALPHA)
        GL11C.glLineWidth(2.0f)
    }

    private fun stop() {
        GL11C.glLineWidth(1.0f)
        GL11C.glDisable(GL11C.GL_BLEND)
        GL11C.glEnable(GL11C.GL_DEPTH_TEST)
        GL20C.glUseProgram(0)
    }

    /**
     * Generate vertices for all enabled debug features
     */
    private fun generateDebugVertices(entities: List<Entity>): FloatArray {
        val vertices = mutableListOf<Float>()

        if (collisionDebugEnabled) {
            generateCollisionVertices(entities, vertices)
        }

        return vertices.toFloatArray()
    }

    private fun generateCollisionVertices(entities: List<Entity>, vertices: MutableList<Float>) {
        for (entity in entities) {
            val physicsComponent = entity.physicsComponent
            val collider = physicsComponent.collider ?: continue
            val worldPos = entity.worldTransform.position

            when (collider) {
                is BoxCollider -> {
                    generateAabbWireframe(collider.getBounds(worldPos), boxColliderColor, vertices)
                }
                is SphereCollider -> {
                    generateSphereWireframe(collider.getCenter(worldPos), collider, sphereColliderColor, vertices)
                }
            }
        }

        playerBounds?.let { bounds ->
            generateAabbWireframe(bounds, playerBoundsColor, vertices)
        }
    }

    private fun generateAabbWireframe(bounds: AABB, color: Vector4f, vertices: MutableList<Float>) {
        val corners = arrayOf(
            Vector3f(bounds.min.x, bounds.min.y, bounds.min.z),
            Vector3f(bounds.max.x, bounds.min.y, bounds.min.z),
            Vector3f(bounds.max.x, bounds.max.y, bounds.min.z),
            Vector3f(bounds.min.x, bounds.max.y, bounds.min.z),
            Vector3f(bounds.min.x, bounds.min.y, bounds.max.z),
            Vector3f(bounds.max.x, bounds.min.y, bounds.max.z),
            Vector3f(bounds.max.x, bounds.max.y, bounds.max.z),
            Vector3f(bounds.min.x, bounds.max.y, bounds.max.z)
        )

        val edges = arrayOf(
            intArrayOf(0, 1), intArrayOf(1, 2), intArrayOf(2, 3), intArrayOf(3, 0),
            intArrayOf(4, 5), intArrayOf(5, 6), intArrayOf(6, 7), intArrayOf(7, 4),
            intArrayOf(0, 4), intArrayOf(1, 5), intArrayOf(2, 6), intArrayOf(3, 7)
        )

        for (edge in edges) {
            appendVertex(vertices, corners[edge[0]], color)
            appendVertex(vertices, corners[edge[1]], color)
        }
    }

    private fun generateSphereWireframe(position: Vector3f, sphere: SphereCollider, color: Vector4f, vertices: MutableList<Float>) {
        val radius = sphere.radius
        val angleStep = (2.0 * Math.PI / circleSegments).toFloat()

        generateCircle(color, vertices, angleStep) { angle ->
            Vector3f(
                position.x + radius * cos(angle),
                position.y + radius * sin(angle),
                position.z
            )
        }

        generateCircle(color, vertices, angleStep) { angle ->
            Vector3f(
                position.x + radius * cos(angle),
                position.y,
                position.z + radius * sin(angle)
            )
        }

        generateCircle(color, vertices, angleStep) { angle ->
            Vector3f(
                position.x,
                position.y + radius * cos(angle),
                position.z + radius * sin(angle)
            )
        }
    }

    private fun generateCircle(
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

            appendVertex(vertices, point1, color)
            appendVertex(vertices, point2, color)
        }
    }

    private fun appendVertex(vertices: MutableList<Float>, point: Vector3f, color: Vector4f) {
        vertices.add(point.x)
        vertices.add(point.y)
        vertices.add(point.z)
        vertices.add(color.x)
        vertices.add(color.y)
        vertices.add(color.z)
        vertices.add(color.w)
    }

    private fun renderVertices(vertices: FloatArray) {
        if (vertices.isEmpty()) return

        GL30C.glBindVertexArray(vao)
        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, vbo)

        GL15C.glBufferSubData(GL15C.GL_ARRAY_BUFFER, 0, vertices)

        val vertexCount = vertices.size / floatsPerVertex
        GL11C.glDrawArrays(GL11C.GL_LINES, 0, vertexCount)

        GL15C.glBindBuffer(GL15C.GL_ARRAY_BUFFER, 0)
        GL30C.glBindVertexArray(0)
    }

    override fun resize(width: Int, height: Int) {
        projectionMatrix = MVP.getPerspectiveMatrix(width / height.toFloat())
    }
}
