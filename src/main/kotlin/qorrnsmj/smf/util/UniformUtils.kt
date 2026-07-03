package qorrnsmj.smf.util

import org.lwjgl.opengl.GL33C.*
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.math.*

object UniformUtils {
    fun setUniform(location: Int, value: Int) {
        glUniform1i(location, value)
    }

    fun setUniform(location: Int, value: Float) {
        glUniform1f(location, value)
    }

    fun setUniform(location: Int, value: IntArray) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocInt(value.size)
            buffer.put(value).flip()
            glUniform1iv(location, buffer)
        }
    }

    fun setUniform(location: Int, value: FloatArray) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocFloat(value.size)
            buffer.put(value).flip()
            glUniform1fv(location, buffer)
        }
    }

    fun setUniform(location: Int, value: Vector2f) {
        glUniform2f(location, value.x, value.y)
    }

    fun setUniform(location: Int, value: Vector3f) {
        glUniform3f(location, value.x, value.y, value.z)
    }

    fun setUniform(location: Int, value: Vector4f) {
        glUniform4f(location, value.x, value.y, value.z, value.w)
    }

    fun setUniform(location: Int, value: Matrix2f) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocFloat(2 * 2)
            value.toBuffer(buffer)
            glUniformMatrix2fv(location, false, buffer)
        }
    }

    fun setUniform(location: Int, value: Matrix3f) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocFloat(3 * 3)
            value.toBuffer(buffer)
            glUniformMatrix3fv(location, false, buffer)
        }
    }

    fun setUniform(location: Int, value: Matrix4f) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocFloat(4 * 4)
            value.toBuffer(buffer)
            glUniformMatrix4fv(location, false, buffer)
        }
    }

    fun setUniform(location: Int, values: List<Matrix4f>) {
        MemoryStack.stackPush().use {
            val buffer = it.mallocFloat(values.size * 16)
            values.forEach { matrix ->
                buffer.put(matrix.m00).put(matrix.m10).put(matrix.m20).put(matrix.m30)
                buffer.put(matrix.m01).put(matrix.m11).put(matrix.m21).put(matrix.m31)
                buffer.put(matrix.m02).put(matrix.m12).put(matrix.m22).put(matrix.m32)
                buffer.put(matrix.m03).put(matrix.m13).put(matrix.m23).put(matrix.m33)
            }
            buffer.flip()
            glUniformMatrix4fv(location, false, buffer)
        }
    }

    fun setUniform(location: Int, textureId: Int, unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_2D, textureId)
        glUniform1i(location, unit)
    }
}
