package qorrnsmj.smf.audio

import org.lwjgl.openal.AL10.*
import org.lwjgl.stb.STBVorbis
import org.lwjgl.stb.STBVorbisInfo
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import java.io.FileInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Path

/**
 * Utility class for loading audio files into AudioBuffers
 */
object AudioLoader {

    /**
     * Load an OGG Vorbis file from resources
     * @param resourcePath Path to the resource (e.g., "assets/audio/bgm/music.ogg")
     * @return AudioBuffer containing the loaded audio data
     */
    fun loadOGG(resourcePath: String): AudioBuffer {
        val inputStream = AudioLoader::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Audio resource not found: $resourcePath")
        
        return loadOGG(inputStream, resourcePath)
    }

    /**
     * Load an OGG Vorbis file from a file path
     * @param filePath Path to the audio file
     * @return AudioBuffer containing the loaded audio data
     */
    fun loadOGGFromFile(filePath: Path): AudioBuffer {
        val inputStream = FileInputStream(filePath.toFile())
        return loadOGG(inputStream, filePath.toString())
    }

    /**
     * Load an OGG Vorbis file from an InputStream
     * @param inputStream The input stream to read from
     * @param identifier Identifier for error messages (file path, etc.)
     * @return AudioBuffer containing the loaded audio data
     */
    private fun loadOGG(inputStream: InputStream, identifier: String): AudioBuffer {
        Logger.debug("Loading OGG audio: $identifier")

        try {
            // Read the entire file into memory
            val fileData = inputStream.readBytes()
            val fileBuffer = MemoryUtil.memAlloc(fileData.size)
            fileBuffer.put(fileData).flip()

            // Use memory stack for temporary allocations
            MemoryStack.stackPush().use { stack ->
                val channelsBuffer = stack.mallocInt(1)
                val sampleRateBuffer = stack.mallocInt(1)

                // Decode the OGG file
                val samples = STBVorbis.stb_vorbis_decode_memory(
                    fileBuffer,
                    channelsBuffer,
                    sampleRateBuffer
                ) ?: throw RuntimeException("Failed to decode OGG file: $identifier")

                val channels = channelsBuffer.get(0)
                val sampleRate = sampleRateBuffer.get(0)

                Logger.debug("OGG decoded - Channels: $channels, Sample Rate: $sampleRate, Samples: ${samples.remaining()}")

                // Determine OpenAL format
                val format = when (channels) {
                    1 -> AL_FORMAT_MONO16
                    2 -> AL_FORMAT_STEREO16
                    else -> throw RuntimeException("Unsupported channel count: $channels")
                }

                // Create and upload to buffer
                val audioBuffer = AudioBuffer()
                audioBuffer.uploadData(samples, format, sampleRate)

                // Cleanup
                MemoryUtil.memFree(samples)
                MemoryUtil.memFree(fileBuffer)

                Logger.info("Successfully loaded OGG audio: $identifier (${audioBuffer.getDuration()}s)")
                return audioBuffer
            }
        } catch (e: Exception) {
            Logger.error(e, "Failed to load OGG audio: $identifier")
            throw RuntimeException("Failed to load OGG audio: $identifier", e)
        } finally {
            inputStream.close()
        }
    }

    /**
     * Create an empty AudioBuffer for streaming or manual data upload
     * @return Empty AudioBuffer ready for data upload
     */
    fun createEmptyBuffer(): AudioBuffer {
        return AudioBuffer()
    }

    /**
     * Get audio file information without loading the full audio data
     * @param resourcePath Path to the resource
     * @return AudioInfo containing metadata
     */
    fun getAudioInfo(resourcePath: String): AudioInfo {
        val inputStream = AudioLoader::class.java.classLoader.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Audio resource not found: $resourcePath")

        try {
            val fileData = inputStream.readBytes()
            val fileBuffer = MemoryUtil.memAlloc(fileData.size)
            fileBuffer.put(fileData).flip()

            MemoryStack.stackPush().use { stack ->
                val info = STBVorbisInfo.malloc(stack)
                val error = stack.mallocInt(1)

                val decoder = STBVorbis.stb_vorbis_open_memory(fileBuffer, error, null)
                if (decoder == 0L) {
                    throw RuntimeException("Failed to open OGG for info: ${error.get(0)}")
                }

                STBVorbis.stb_vorbis_get_info(decoder, info)
                
                val channels = info.channels()
                val sampleRate = info.sample_rate()
                val samples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder)
                val duration = samples.toFloat() / sampleRate
                
                STBVorbis.stb_vorbis_close(decoder)
                MemoryUtil.memFree(fileBuffer)

                return AudioInfo(channels, sampleRate, samples, duration)
            }
        } finally {
            inputStream.close()
        }
    }

    /**
     * Check if a file is a supported audio format
     * @param filename The filename to check
     * @return True if the file extension is supported
     */
    fun isSupportedFormat(filename: String): Boolean {
        val extension = filename.substringAfterLast('.', "").lowercase()
        return extension == "ogg"
    }

    /**
     * Data class containing audio file metadata
     */
    data class AudioInfo(
        val channels: Int,
        val sampleRate: Int,
        val totalSamples: Int,
        val duration: Float
    )
}