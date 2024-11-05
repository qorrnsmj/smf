package qorrnsmj.smf.graphic.shader

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.math.*

// TODO: location引数にとるのやめよう
class ShaderProgram {
    private val id = glCreateProgram()

    /**
     * Attaches a shader to this program.
     *
     * @param shader Shader to attach
     * @return This shader program
     */
    fun attachShader(shader: Shader) {
        glAttachShader(id, shader.id)
    }

    /**
     * Links this program and check it's status afterwards.
     *
     * @return This shader program
     */
    fun link() {
        glLinkProgram(id)

        // check if linking was successful
        val status = glGetProgrami(id, GL_LINK_STATUS)
        check(status == GL_TRUE) { glGetProgramInfoLog(id) }
    }

    fun unlink() {
        glLinkProgram(0)
    }

    /**
     * Uses this shader program.
     */
    fun use() {
        glUseProgram(id)
    }

    /**
     * Unuses this shader program.
     */
    fun unuse() {
        glUseProgram(0)
    }

    /**
     * Deletes this shader program.
     */
    fun delete() {
        unlink()
        unuse()
        glDeleteProgram(id)
    }

    /**
     * Binds the fragment out color variable.
     *
     * @param number Color number you want to bind
     * @param name   Variable name
     */
    fun bindFragmentDataLocation(number: Int, name: CharSequence) {
        glBindFragDataLocation(id, number, name)
    }

    /**
     * Gets the location of an attribute variable with specified name.
     *
     * @param name Attribute name
     *
     * @return Location of the attribute
     */
    fun getAttributeLocation(name: CharSequence): Int {
        return glGetAttribLocation(id, name)
    }

    /**
     * Enables a vertex attribute.
     *
     * @param location Location of the vertex attribute
     */
    fun enableVertexAttribute(location: Int) {
        glEnableVertexAttribArray(location)
    }

    /**
     * Disables a vertex attribute.
     *
     * @param location Location of the vertex attribute
     */
    fun disableVertexAttribute(location: Int) {
        glDisableVertexAttribArray(location)
    }

    /**
     * Sets the vertex attribute pointer.
     *
     * @param location Location of the vertex attribute
     * @param size     Number of values per vertex
     * @param stride   Offset between consecutive generic vertex attributes in
     * bytes
     * @param offset   Offset of the first component of the first generic vertex
     * attribute in bytes
     */
    fun pointVertexAttribute(location: Int, size: Int, stride: Int, offset: Int) {
        glVertexAttribPointer(location, size, GL_FLOAT, false, stride, offset.toLong())
    }

    /**
     * Gets the location of an uniform variable with specified name.
     *
     * @param name Uniform name
     *
     * @return Location of the uniform
     */
    fun getUniformLocation(name: CharSequence): Int {
        return glGetUniformLocation(id, name)
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Int) {
        glUniform1i(location, value)
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Vector2f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(2)
            value.toBuffer(buffer)
            glUniform2fv(location, buffer)
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Vector3f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(3)
            value.toBuffer(buffer)
            glUniform3fv(location, buffer)
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Vector4f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4)
            value.toBuffer(buffer)
            glUniform4fv(location, buffer)
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Matrix2f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(2 * 2)
            value.toBuffer(buffer)
            glUniformMatrix2fv(location, false, buffer)
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(location: Int, value: Matrix3f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(3 * 3)
            value.toBuffer(buffer)
            glUniformMatrix3fv(location, false, buffer)
        }
    }

    /**
     * Sets the uniform variable for specified location.
     *
     * @param location Uniform location
     * @param value    Value to set
     */
    fun setUniform(id: String, value: Matrix4f) {
        MemoryStack.stackPush().use { stack ->
            val buffer = stack.mallocFloat(4 * 4)
            value.toBuffer(buffer)
            glUniformMatrix4fv(getUniformLocation(id), false, buffer)
        }
    }
}
