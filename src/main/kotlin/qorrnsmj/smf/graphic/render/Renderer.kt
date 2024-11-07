package qorrnsmj.smf.graphic.render

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import qorrnsmj.smf.graphic.shader.Shader
import qorrnsmj.smf.graphic.shader.ShaderProgram
import qorrnsmj.smf.graphic.shader.VertexArrayObject
import qorrnsmj.smf.graphic.shader.VertexBufferObject
import qorrnsmj.smf.math.Matrix4f
import qorrnsmj.smf.math.Vector3f
import java.nio.FloatBuffer

// vao, shaderprogramは単一で
// https://www.reddit.com/r/opengl/comments/deqzl1/what_should_be_sharing_a_vaovbo/
// https://stackoverflow.com/questions/66001948/should-shader-programs-be-shared-in-opengl
object Renderer {
    private lateinit var vao: VertexArrayObject
    private lateinit var vbo: VertexBufferObject
    private lateinit var program: ShaderProgram
    private lateinit var vertices: FloatBuffer
    private var verticesCount = 0
    private var drawing = false

    fun init() {
        Logger.debug("[Renderer] init")

        glEnable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glEnable(GL_DEPTH_TEST)
        glCullFace(GL_BACK)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        vao = VertexArrayObject().bind()
        vbo = VertexBufferObject().bind(GL_ARRAY_BUFFER)
        program = ShaderProgram().apply {
            attachShader(Shader(GL_VERTEX_SHADER, "default.vert"))
            attachShader(Shader(GL_FRAGMENT_SHADER, "default.frag"))
            link()
            use()
        }
        vertices = MemoryUtil.memAllocFloat(1024)

        glVertexAttribPointer(0, 3, GL_FLOAT, false, 7 * Float.SIZE_BYTES, 0)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(1, 4, GL_FLOAT, false, 7 * Float.SIZE_BYTES, (3 * Float.SIZE_BYTES).toLong())
        glEnableVertexAttribArray(1)

        setProjection(Projection.getPerspectiveMatrix(1600f / 1600f))
        setView(View.getMatrix(
            eye = Vector3f(0f, 0f, 10f),
            center = Vector3f(0f, 0f, 0f),
            up = Vector3f(0f, 1f, 0f)
        ))
        setModel(Matrix4f())
    }

    /* Render */

    fun clear() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    fun begin() {
        check(!drawing) { "[Renderer] Renderer is already drawing!" }
        drawing = true
        verticesCount = 0
    }

    fun end() {
        check(drawing) { "[Renderer] Renderer isn't drawing!" }
        drawing = false
        flush()
    }

    fun dispose() {
        vao.delete()
        vbo.delete()
        program.delete()
    }

    fun draw(vertices: FloatArray) {
        // Uniform変数が更新されたら呼び出せばいい
        //setUniforms()

        // 空き容量があるか確認。無かったらflush
        if (this.vertices.remaining() < vertices.size) {
            Logger.debug("[Renderer] No remaining!")
            flush()
        }

        // Renderer.verticesに追加
        for (i in vertices.indices) {
            this.vertices.put(vertices[i])
        }
        verticesCount += vertices.size / 7
    }

    private fun flush() {
        if (verticesCount > 0) {
            // positionを0にする
            vertices.flip()

            vbo.bind(GL_ARRAY_BUFFER)
            vbo.uploadData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW) // OK

            // 描画
            glDrawArrays(GL_TRIANGLES, 0, verticesCount) // OK

            // 頂点データをクリア
            vertices.clear()
            verticesCount = 0
        }
    }

    /* Uniforms */

    fun setProjection(projection: Matrix4f) {
        program.setUniform("projection", projection)
    }

    fun setView(view: Matrix4f) {
        program.setUniform("view", view)
    }

    fun setModel(model: Matrix4f) {
        program.setUniform("model", model)
    }
}
