package qorrnsmj.smf.audio

import org.lwjgl.openal.AL10.*
import org.lwjgl.openal.AL11.AL_SEC_OFFSET
import org.lwjgl.system.MemoryUtil
import org.tinylog.kotlin.Logger

/**
 * Wrapper class for OpenAL audio sources
 */
class AudioSource {
    private val sourceId: Int = alGenSources()
    private var disposed = false

    init {
        if (sourceId == AL_INVALID_VALUE) {
            throw RuntimeException("Failed to create OpenAL audio source")
        }
    }

    /**
     * Attach an AudioBuffer to this source
     */
    fun setBuffer(buffer: AudioBuffer?) {
        checkNotDisposed()
        val bufferId = buffer?.getId() ?: AL_NONE
        alSourcei(sourceId, AL_BUFFER, bufferId)
        checkALError("Failed to set audio buffer")
    }

    /**
     * Play the audio source
     */
    fun play() {
        checkNotDisposed()
        alSourcePlay(sourceId)
        checkALError("Failed to play audio source")
    }

    /**
     * Pause the audio source
     */
    fun pause() {
        checkNotDisposed()
        alSourcePause(sourceId)
        checkALError("Failed to pause audio source")
    }

    /**
     * Stop the audio source
     */
    fun stop() {
        checkNotDisposed()
        alSourceStop(sourceId)
        checkALError("Failed to stop audio source")
    }

    /**
     * Check if the source is currently playing
     */
    fun isPlaying(): Boolean {
        checkNotDisposed()
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PLAYING
    }

    /**
     * Check if the source is paused
     */
    fun isPaused(): Boolean {
        checkNotDisposed()
        return alGetSourcei(sourceId, AL_SOURCE_STATE) == AL_PAUSED
    }

    /**
     * Check if the source is stopped
     */
    fun isStopped(): Boolean {
        checkNotDisposed()
        val state = alGetSourcei(sourceId, AL_SOURCE_STATE)
        return state == AL_STOPPED || state == AL_INITIAL
    }

    /**
     * Set the volume (gain) of the source
     * @param volume Volume level (0.0 = silent, 1.0 = normal, >1.0 = amplified)
     */
    fun setVolume(volume: Float) {
        checkNotDisposed()
        alSourcef(sourceId, AL_GAIN, volume.coerceAtLeast(0f))
        checkALError("Failed to set audio volume")
    }

    /**
     * Get the current volume
     */
    fun getVolume(): Float {
        checkNotDisposed()
        return alGetSourcef(sourceId, AL_GAIN)
    }

    /**
     * Set the pitch of the source
     * @param pitch Pitch multiplier (0.5 = half speed/octave down, 2.0 = double speed/octave up)
     */
    fun setPitch(pitch: Float) {
        checkNotDisposed()
        alSourcef(sourceId, AL_PITCH, pitch.coerceAtLeast(0.01f))
        checkALError("Failed to set audio pitch")
    }

    /**
     * Get the current pitch
     */
    fun getPitch(): Float {
        checkNotDisposed()
        return alGetSourcef(sourceId, AL_PITCH)
    }

    /**
     * Set whether the source should loop
     */
    fun setLooping(loop: Boolean) {
        checkNotDisposed()
        alSourcei(sourceId, AL_LOOPING, if (loop) AL_TRUE else AL_FALSE)
        checkALError("Failed to set audio looping")
    }

    /**
     * Check if the source is set to loop
     */
    fun isLooping(): Boolean {
        checkNotDisposed()
        return alGetSourcei(sourceId, AL_LOOPING) == AL_TRUE
    }

    /**
     * Set the 3D position of the source
     */
    fun setPosition(x: Float, y: Float, z: Float) {
        checkNotDisposed()
        val buffer = MemoryUtil.memAllocFloat(3)
        try {
            buffer.put(x).put(y).put(z).flip()
            alSourcefv(sourceId, AL_POSITION, buffer)
            checkALError("Failed to set audio position")
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Set the 3D velocity of the source (for doppler effect)
     */
    fun setVelocity(x: Float, y: Float, z: Float) {
        checkNotDisposed()
        val buffer = MemoryUtil.memAllocFloat(3)
        try {
            buffer.put(x).put(y).put(z).flip()
            alSourcefv(sourceId, AL_VELOCITY, buffer)
            checkALError("Failed to set audio velocity")
        } finally {
            MemoryUtil.memFree(buffer)
        }
    }

    /**
     * Get current playback position in seconds
     */
    fun getPlaybackPosition(): Float {
        checkNotDisposed()
        return alGetSourcef(sourceId, AL_SEC_OFFSET)
    }

    /**
     * Set playback position in seconds
     */
    fun setPlaybackPosition(seconds: Float) {
        checkNotDisposed()
        alSourcef(sourceId, AL_SEC_OFFSET, seconds.coerceAtLeast(0f))
        checkALError("Failed to set playback position")
    }

    /**
     * Get the OpenAL source ID
     */
    fun getId(): Int {
        checkNotDisposed()
        return sourceId
    }

    /**
     * Check if source is valid and not disposed
     */
    fun isValid(): Boolean = !disposed && alIsSource(sourceId)

    /**
     * Dispose of the source and free OpenAL resources
     */
    fun dispose() {
        if (!disposed) {
            if (alIsSource(sourceId)) {
                stop() // Stop before deleting
                alDeleteSources(sourceId)
                Logger.debug("AudioSource disposed: $sourceId")
            }
            disposed = true
        }
    }

    private fun checkNotDisposed() {
        if (disposed) {
            throw IllegalStateException("AudioSource has been disposed")
        }
    }

    private fun checkALError(message: String) {
        val error = alGetError()
        if (error != AL_NO_ERROR) {
            throw RuntimeException("$message: OpenAL error $error")
        }
    }
}
