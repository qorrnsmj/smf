package qorrnsmj.smf.audio

import org.lwjgl.openal.AL10.*
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger
import java.nio.ShortBuffer

/**
 * Wrapper class for OpenAL audio buffers
 */
class AudioBuffer {
    private val bufferId: Int = alGenBuffers()
    private var disposed = false

    init {
        if (bufferId == AL_INVALID_VALUE) {
            throw RuntimeException("Failed to create OpenAL audio buffer")
        }
    }

    /**
     * Upload audio data to the buffer
     * @param data PCM audio data
     * @param format OpenAL audio format (AL_FORMAT_MONO16, AL_FORMAT_STEREO16, etc.)
     * @param sampleRate Sample rate in Hz
     */
    fun uploadData(data: ShortBuffer, format: Int, sampleRate: Int) {
        checkNotDisposed()
        alBufferData(bufferId, format, data, sampleRate)
        checkALError("Failed to upload audio data")
    }

    /**
     * Upload audio data to the buffer (ByteArray variant)
     * @param data PCM audio data
     * @param format OpenAL audio format
     * @param sampleRate Sample rate in Hz
     */
    fun uploadData(data: ByteArray, format: Int, sampleRate: Int) {
        checkNotDisposed()
        val buffer = MemoryUtil.memAlloc(data.size)
        try {
            buffer.put(data).flip()
            alBufferData(bufferId, format, buffer, sampleRate)
            checkALError("Failed to upload audio data")
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Get the OpenAL buffer ID
     */
    fun getId(): Int {
        checkNotDisposed()
        return bufferId
    }

    /**
     * Get buffer size in bytes
     */
    fun getSize(): Int {
        checkNotDisposed()
        return alGetBufferi(bufferId, AL_SIZE)
    }

    /**
     * Get buffer frequency (sample rate)
     */
    fun getFrequency(): Int {
        checkNotDisposed()
        return alGetBufferi(bufferId, AL_FREQUENCY)
    }

    /**
     * Get buffer format
     */
    fun getFormat(): Int {
        checkNotDisposed()
        val channels = alGetBufferi(bufferId, AL_CHANNELS)
        val bits = alGetBufferi(bufferId, AL_BITS)

        return when {
            channels == 1 && bits == 8 -> AL_FORMAT_MONO8
            channels == 1 && bits == 16 -> AL_FORMAT_MONO16
            channels == 2 && bits == 8 -> AL_FORMAT_STEREO8
            channels == 2 && bits == 16 -> AL_FORMAT_STEREO16
            else -> AL_FORMAT_MONO16
        }
    }

    /**
     * Get buffer duration in seconds
     */
    fun getDuration(): Float {
        checkNotDisposed()
        val size = getSize()
        val frequency = getFrequency()
        val format = getFormat()
        
        // Calculate bytes per sample based on format
        val bytesPerSample = when (format) {
            AL_FORMAT_MONO8 -> 1
            AL_FORMAT_MONO16 -> 2
            AL_FORMAT_STEREO8 -> 2
            AL_FORMAT_STEREO16 -> 4
            else -> 2 // default fallback
        }
        
        return if (frequency > 0 && bytesPerSample > 0) {
            size.toFloat() / (frequency * bytesPerSample)
        } else {
            0f
        }
    }

    /**
     * Check if buffer is valid and not disposed
     */
    fun isValid(): Boolean = !disposed && alIsBuffer(bufferId)

    /**
     * Dispose of the buffer and free OpenAL resources
     */
    fun dispose() {
        if (!disposed) {
            if (alIsBuffer(bufferId)) {
                alDeleteBuffers(bufferId)
                Logger.debug("AudioBuffer disposed: $bufferId")
            }
            disposed = true
        }
    }

    private fun checkNotDisposed() {
        if (disposed) {
            throw IllegalStateException("AudioBuffer has been disposed")
        }
    }

    private fun checkALError(message: String) {
        val error = alGetError()
        if (error != AL_NO_ERROR) {
            throw RuntimeException("$message: OpenAL error $error")
        }
    }

    companion object {
        /**
         * Create an AudioBuffer with immediate data upload
         */
        fun create(data: ShortBuffer, format: Int, sampleRate: Int): AudioBuffer {
            val buffer = AudioBuffer()
            buffer.uploadData(data, format, sampleRate)
            return buffer
        }

        /**
         * Create an AudioBuffer with immediate data upload (ByteArray variant)
         */
        fun create(data: ByteArray, format: Int, sampleRate: Int): AudioBuffer {
            val buffer = AudioBuffer()
            buffer.uploadData(data, format, sampleRate)
            return buffer
        }
    }
}
