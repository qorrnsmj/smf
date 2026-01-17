//package qorrnsmj.test.t11.core.render
//
//import org.lwjgl.opengl.GL33C.*
//import org.lwjgl.system.MemoryStack
//import org.tinylog.kotlin.Logger
//import qorrnsmj.smf.math.*
//import qorrnsmj.test.t11.core.model.Model
//import qorrnsmj.test.t11.core.render.UniformType.*
//import qorrnsmj.test.t11.game.entity.Entity
//
//object MasterRenderer {
//    private val entityRenderer = EntityRenderer()
//    private val modelEntitiesMap = HashMap<Model, MutableList<Entity>>()
//
//    init {
//        Logger.info("MasterRenderer initializing...")
//
//        glEnable(GL_BLEND)
//        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
//
//        glEnable(GL_CULL_FACE)
//        glCullFace(GL_BACK)
//
//        glEnable(GL_DEPTH_TEST)
//        glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
//
//        Logger.info("MasterRenderer initialized!!")
//    }
//
//    /* Render */
//
//    // TODO: Sceneクラス的なの作るのはどう？
//    fun render(entities: List<Entity>) {
//        // process
//        for (entity in entities) processEntity(entity)
//
//        // render
//        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
//        entityRenderer.render(modelEntitiesMap)
//
//        // clear
//        modelEntitiesMap.clear()
//    }
//
//    private fun processEntity(entity: Entity) {
//        val batch = modelEntitiesMap.getOrPut(entity.getModel()) { mutableListOf() }
//        batch.add(entity)
//    }
//
//    /* Uniform */
//
//    fun <T> setUniform(location: Int, value: T) {
//        when (value) {
//            is Int -> glUniform1i(location, value)
//            is Float -> glUniform1f(location, value)
//            is Vector2f -> glUniform2f(location, value.x, value.y)
//            is Vector3f -> glUniform3f(location, value.x, value.y, value.z)
//            is Vector4f -> glUniform4f(location, value.x, value.y, value.z, value.w)
//            is Matrix2f -> {
//                MemoryStack.stackPush().use { stack ->
//                    val buffer = stack.mallocFloat(2 * 2)
//                    value.toBuffer(buffer)
//                    glUniformMatrix2fv(location, false, buffer)
//                }
//            }
//            is Matrix3f -> {
//                MemoryStack.stackPush().use { stack ->
//                    val buffer = stack.mallocFloat(3 * 3)
//                    value.toBuffer(buffer)
//                    glUniformMatrix3fv(location, false, buffer)
//                }
//            }
//            is Matrix4f -> {
//                MemoryStack.stackPush().use { stack ->
//                    val buffer = stack.mallocFloat(4 * 4)
//                    value.toBuffer(buffer)
//                    glUniformMatrix4fv(location, false, buffer)
//                }
//            }
//            else -> throw IllegalArgumentException("Unsupported type")
//        }
//    }
//
//    fun updateModelMatrix(matrix: Matrix4f) {
//        val location = entityRenderer.program.uniformLocationMap[MODEL]!!
//        glUseProgram(entityRenderer.program.id)
//        setUniform(location, matrix)
//        glUseProgram(0)
//    }
//
//    fun updateViewMatrix(matrix: Matrix4f) {
//        val location = entityRenderer.program.uniformLocationMap[VIEW]!!
//        glUseProgram(entityRenderer.program.id)
//        setUniform(location, matrix)
//        glUseProgram(0)
//    }
//
//    fun updateProjectionMatrix(matrix: Matrix4f) {
//        val location = entityRenderer.program.uniformLocationMap[PROJECTION]!!
//        glUseProgram(entityRenderer.program.id)
//        setUniform(location, matrix)
//        glUseProgram(0)
//    }
//
//    enum class RendererType {
//        ENTITY
//    }
//}
