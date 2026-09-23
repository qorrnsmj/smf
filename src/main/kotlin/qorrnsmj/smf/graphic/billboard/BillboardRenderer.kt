package qorrnsmj.smf.graphic.billboard

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryUtil
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.game.entity.billboard.BillboardEntity
import qorrnsmj.smf.game.entity.custom.Entity
import qorrnsmj.smf.graphic.SceneRenderer
import qorrnsmj.smf.graphic.scene.Scene
import qorrnsmj.smf.graphic.scene.settings.ViewportShadingSettings
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.MVP
import qorrnsmj.smf.util.Resizable
import qorrnsmj.smf.util.UniformUtils.setUniform

class BillboardRenderer : SceneRenderer, Resizable {
    private val program = BillboardShaderProgram()
    private val locationView = glGetUniformLocation(program.id, "view")
    private val locationProjection = glGetUniformLocation(program.id, "projection")
    private val locationCenter = glGetUniformLocation(program.id, "center")
    private val locationSize = glGetUniformLocation(program.id, "size")
    private val locationRight = glGetUniformLocation(program.id, "billboardRight")
    private val locationUp = glGetUniformLocation(program.id, "billboardUp")
    private val locationTexture = glGetUniformLocation(program.id, "billboardTexture")
    private val locationTint = glGetUniformLocation(program.id, "tint")
    private val locationAlphaSource = glGetUniformLocation(program.id, "alphaSource")
    private val locationEdgeFade = glGetUniformLocation(program.id, "edgeFade")
    private val vao = createQuad()

    override fun render(scene: Scene) {
        if (scene.renderSettings.viewportShadingMode != ViewportShadingSettings.RENDERED) return

        val billboards = mutableListOf<BillboardEntity>()
        scene.world.entities.forEach { collectBillboards(it, billboards) }
        if (billboards.isEmpty()) return

        val camera = scene.world.camera
        billboards.sortByDescending { distanceSquared(it.worldTransform.position, camera.position) }

        program.use()
        setUniform(locationView, camera.getViewMatrix())
        glBindVertexArray(vao)
        glEnableVertexAttribArray(0)
        glEnableVertexAttribArray(1)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(false)

        for (entity in billboards) {
            renderBillboard(entity, camera)
        }

        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glActiveTexture(GL_TEXTURE0)
        glBindTexture(GL_TEXTURE_2D, 0)
        glDisableVertexAttribArray(0)
        glDisableVertexAttribArray(1)
        glBindVertexArray(0)
    }

    private fun renderBillboard(entity: BillboardEntity, camera: Camera) {
        val billboard = entity.billboard
        val center = entity.worldTransform.position
        val (right, up) = getBasis(billboard.facing, center, camera)

        if (billboard.doubleSided) {
            glDisable(GL_CULL_FACE)
        } else {
            glEnable(GL_CULL_FACE)
            glCullFace(GL_BACK)
        }

        setUniform(locationCenter, center)
        setUniform(locationSize, billboard.size)
        setUniform(locationRight, right)
        setUniform(locationUp, up)
        setUniform(locationTexture, billboard.texture.id, 0)
        setUniform(locationTint, billboard.tint)
        setUniform(locationAlphaSource, billboard.alphaSource.ordinal)
        setUniform(locationEdgeFade, billboard.edgeFade.coerceIn(0f, 1f))
        glDrawArrays(GL_TRIANGLES, 0, 6)
    }

    private fun getBasis(
        facing: BillboardFacing,
        center: Vector3f,
        camera: Camera,
    ): Pair<Vector3f, Vector3f> {
        if (facing == BillboardFacing.HORIZONTAL) {
            return Vector3f(1f, 0f, 0f) to Vector3f(0f, 0f, 1f)
        }

        if (facing == BillboardFacing.CAMERA_Y_AXIS) {
            val toCamera = camera.position.subtract(center)
            val horizontal = Vector3f(toCamera.x, 0f, toCamera.z)
            if (horizontal.lengthSquared() > 0.000001f) {
                val right = Vector3f(0f, 1f, 0f).cross(horizontal.normalize()).normalize()
                return right to Vector3f(0f, 1f, 0f)
            }
        }

        val front = camera.getFront()
        val right = front.cross(camera.up).normalize()
        val up = right.cross(front).normalize()
        return right to up
    }

    private fun collectBillboards(entity: Entity, result: MutableList<BillboardEntity>) {
        if (entity is BillboardEntity) result.add(entity)
        entity.children.forEach { collectBillboards(it, result) }
    }

    private fun distanceSquared(first: Vector3f, second: Vector3f): Float {
        val delta = first.subtract(second)
        return delta.lengthSquared()
    }

    override fun resize(width: Int, height: Int) {
        program.use()
        setUniform(locationProjection, MVP.getPerspectiveMatrix(width / height.toFloat()))
    }

    private fun createQuad(): Int {
        val vertices = floatArrayOf(
            -0.5f, -0.5f, 0f, 0f,
             0.5f, -0.5f, 1f, 0f,
             0.5f,  0.5f, 1f, 1f,
            -0.5f, -0.5f, 0f, 0f,
             0.5f,  0.5f, 1f, 1f,
            -0.5f,  0.5f, 0f, 1f,
        )
        val vao = glGenVertexArrays()
        val vbo = glGenBuffers()
        glBindVertexArray(vao)
        glBindBuffer(GL_ARRAY_BUFFER, vbo)

        val buffer = MemoryUtil.memAllocFloat(vertices.size)
        buffer.put(vertices).flip()
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW)
        MemoryUtil.memFree(buffer)

        val stride = 4 * Float.SIZE_BYTES
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.SIZE_BYTES)
        glBindVertexArray(0)
        return vao
    }
}
