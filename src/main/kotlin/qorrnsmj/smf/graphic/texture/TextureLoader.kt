package qorrnsmj.smf.graphic.texture

import de.javagl.jgltf.model.TextureModel
import org.lwjgl.opengl.GL33C.GL_RGBA
import org.lwjgl.opengl.GL33C.GL_TEXTURE_2D
import org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_T
import org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE
import org.lwjgl.opengl.GL33C.glTexImage2D
import org.lwjgl.opengl.GL33C.glTexParameteri
import org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_R
import org.lwjgl.opengl.GL33C.glGenerateMipmap
import org.lwjgl.opengl.GL33C.GL_TEXTURE_CUBE_MAP
import org.lwjgl.opengl.GL33C.GL_TEXTURE_CUBE_MAP_POSITIVE_X
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import qorrnsmj.smf.graphic.`object`.TextureBufferObject
import qorrnsmj.smf.util.ResourceUtils
import qorrnsmj.smf.util.Cleanable
import java.nio.ByteBuffer
import kotlin.use

object TextureLoader : Cleanable {
    val textures = mutableListOf<TextureBufferObject>()

    fun loadTexture(textureModel: TextureModel, params: TextureParams): TextureBufferObject {
        val imageData = textureModel.imageModel.imageData
            ?: error("Texture model has no image data")
        val imageBuffer = ResourceUtils.getResourceAsDirectBuffer(imageData)

        return loadTexture(imageBuffer, params)
    }

    fun loadTexture(imagePath: String, params: TextureParams): TextureBufferObject {
        val imageBuffer = ResourceUtils.getResourceAsDirectBuffer(imagePath)
        val texture = loadTexture(imageBuffer, params)

        return texture
    }

    fun loadTexture(imageData: ByteBuffer, params: TextureParams): TextureBufferObject {
        val target = GL_TEXTURE_2D
        val texture = createAndBindTexture()

        uploadImage2D(target, imageData)
        glGenerateMipmap(target)
        applyTextureParams(target, params)

        return texture
    }

    fun loadCubemapTexture(imagePath: String, params: TextureParams): TextureBufferObject {
        val target = GL_TEXTURE_CUBE_MAP
        val texture = createAndBindTexture()
        val faces = arrayOf(
            "${imagePath}_front.png",
            "${imagePath}_back.png",
            "${imagePath}_top.png",
            "${imagePath}_bottom.png",
            "${imagePath}_right.png",
            "${imagePath}_left.png",
        )

        for ((i, file) in faces.withIndex()) {
            val buffer = ResourceUtils.getResourceAsDirectBuffer(file)
            uploadImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, buffer)
        }

        applyTextureParams(target, params)
        return texture
    }

    private fun createAndBindTexture(): TextureBufferObject {
        val tbo = TextureBufferObject()
        tbo.bind()
        textures.add(tbo)

        return tbo
    }

    private fun applyTextureParams(target: Int, params: TextureParams) {
        glTexParameteri(target, GL_TEXTURE_MAG_FILTER, params.magFilter)
        glTexParameteri(target, GL_TEXTURE_MIN_FILTER, params.minFilter)
        glTexParameteri(target, GL_TEXTURE_WRAP_S, params.wrapS)
        glTexParameteri(target, GL_TEXTURE_WRAP_T, params.wrapT)
        glTexParameteri(target, GL_TEXTURE_WRAP_R, params.wrapR)
    }

    private fun uploadImage2D(target: Int, imageBuffer: ByteBuffer) {
        if (!imageBuffer.isDirect) {
            error("Image buffer must be a direct ByteBuffer")
        }

        MemoryStack.stackPush().use { stack ->
            val w = stack.mallocInt(1)
            val h = stack.mallocInt(1)
            val c = stack.mallocInt(1)
            val pixels = STBImage.stbi_load_from_memory(imageBuffer, w, h, c, 4)
                ?: error("Failed to load image: ${STBImage.stbi_failure_reason()}")

            glTexImage2D(
                target, 0, GL_RGBA,
                w.get(), h.get(), 0,
                GL_RGBA, GL_UNSIGNED_BYTE, pixels
            )
            STBImage.stbi_image_free(pixels)
        }
    }

    override fun cleanup() {
        textures.forEach { it.delete() }
    }
}

