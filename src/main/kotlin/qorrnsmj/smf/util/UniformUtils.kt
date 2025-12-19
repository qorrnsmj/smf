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
}
