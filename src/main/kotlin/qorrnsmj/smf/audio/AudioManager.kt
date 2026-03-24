package qorrnsmj.smf.audio

import org.lwjgl.BufferUtils
import org.lwjgl.openal.AL10.*
import org.tinylog.kotlin.Logger

/**
 * Main audio management singleton that coordinates all audio operations
 */
object AudioManager {
    private var isInitialized = false
    private val audioSources = mutableListOf<AudioSource>()
    private val availableSources = mutableSetOf<AudioSource>()
    private val activeSources = mutableSetOf<AudioSource>()

    // Audio settings
    private var masterVolume = 1.0f
    private var bgmVolume = 0.8f
    private var sfxVolume = 1.0f

    // Special sources for BGM
    private var bgmSource: AudioSource? = null
    private var currentBGMBuffer: AudioBuffer? = null

    /**
     * Initialize the audio manager
     * This should be called after AudioContext.initialize()
     */
    fun initialize() {
        if (isInitialized) {
            Logger.warn("AudioManager already initialized")
            return
        }

        if (!AudioContext.isReady()) {
            throw IllegalStateException("AudioContext must be initialized before AudioManager")
        }

        Logger.info("Initializing AudioManager...")

        // Create a pool of audio sources for sound effects
        createSourcePool(16) // Create 16 sources for sound effects

        // Create dedicated BGM source
        bgmSource = AudioSource()

        // Set up listener (player) position and orientation
        setupListener()

        isInitialized = true
        Logger.info("AudioManager initialized successfully")
    }

    /**
     * Create a pool of audio sources for efficient sound effect playback
     */
    private fun createSourcePool(count: Int) {
        repeat(count) {
            try {
                val source = AudioSource()
                audioSources.add(source)
                availableSources.add(source)
            } catch (e: Exception) {
                Logger.warn("Failed to create audio source ${it + 1}/$count: ${e.message}")
            }
        }
        Logger.info("Created ${availableSources.size} audio sources")
    }

    /**
     * Set up the audio listener (player position and orientation)
     */
    private fun setupListener() {
        // Set listener position (0, 0, 0)
        val posBuffer = BufferUtils.createFloatBuffer(3)
        posBuffer.put(floatArrayOf(0f, 0f, 0f)).flip()
        alListenerfv(AL_POSITION, posBuffer)
        
        // Set listener velocity (0, 0, 0)
        val velBuffer = BufferUtils.createFloatBuffer(3)
        velBuffer.put(floatArrayOf(0f, 0f, 0f)).flip()
        alListenerfv(AL_VELOCITY, velBuffer)
        
        // Set listener orientation (forward and up vectors)
        // Forward: (0, 0, -1), Up: (0, 1, 0)
        val oriBuffer = BufferUtils.createFloatBuffer(6)
        oriBuffer.put(floatArrayOf(0f, 0f, -1f, 0f, 1f, 0f)).flip()
        alListenerfv(AL_ORIENTATION, oriBuffer)
        
        // Set master volume
        alListenerf(AL_GAIN, masterVolume)
    }

    /**
     * Update the audio system (call this in the game loop)
     */
    fun update() {
        if (!isInitialized) return

        // Clean up finished sources
        val iterator = activeSources.iterator()
        while (iterator.hasNext()) {
            val source = iterator.next()
            if (source.isStopped()) {
                iterator.remove()
                source.setBuffer(null) // Unattach buffer
                availableSources.add(source)
            }
        }
    }

    /**
     * Play a sound effect
     * @param buffer The audio buffer containing the sound
     * @param volume Volume modifier (multiplied with SFX volume)
     * @param pitch Pitch modifier (1.0 = normal)
     * @param loop Whether to loop the sound
     * @return AudioSource playing the sound, or null if no sources available
     */
    fun playSFX(buffer: AudioBuffer, volume: Float = 1.0f, pitch: Float = 1.0f, loop: Boolean = false): AudioSource? {
        if (!isInitialized) {
            Logger.warn("AudioManager not initialized")
            return null
        }

        val source = getAvailableSource() ?: return null
        
        try {
            source.setBuffer(buffer)
            source.setVolume(volume * sfxVolume * masterVolume)
            source.setPitch(pitch)
            source.setLooping(loop)
            source.play()

            availableSources.remove(source)
            activeSources.add(source)

            return source
        } catch (e: Exception) {
            Logger.error(e, "Failed to play SFX")
            availableSources.add(source) // Return source to pool
            return null
        }
    }

    /**
     * Play background music
     * @param buffer The audio buffer containing the music
     * @param volume Volume modifier (multiplied with BGM volume)
     * @param loop Whether to loop the music (default: true)
     */
    fun playBGM(buffer: AudioBuffer, volume: Float = 1.0f, loop: Boolean = true) {
        if (!isInitialized) {
            Logger.warn("AudioManager not initialized")
            return
        }

        val source = bgmSource ?: return

        try {
            // Stop current BGM if playing
            if (source.isPlaying()) {
                source.stop()
            }

            source.setBuffer(buffer)
            source.setVolume(volume * bgmVolume * masterVolume)
            source.setPitch(1.0f)
            source.setLooping(loop)
            source.play()

            currentBGMBuffer = buffer
            Logger.info("Started BGM playback")
        } catch (e: Exception) {
            Logger.error(e, "Failed to play BGM")
        }
    }

    /**
     * Stop background music
     */
    fun stopBGM() {
        bgmSource?.stop()
        currentBGMBuffer = null
    }

    /**
     * Pause background music
     */
    fun pauseBGM() {
        bgmSource?.pause()
    }

    /**
     * Resume background music
     */
    fun resumeBGM() {
        bgmSource?.let { source ->
            if (source.isPaused()) {
                source.play()
            }
        }
    }

    /**
     * Check if BGM is currently playing
     */
    fun isBGMPlaying(): Boolean = bgmSource?.isPlaying() ?: false

    /**
     * Stop all currently playing sounds
     */
    fun stopAll() {
        stopBGM()
        activeSources.forEach { it.stop() }
        activeSources.clear()
        availableSources.addAll(audioSources)
    }

    /**
     * Set master volume (affects all audio)
     */
    fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        alListenerf(AL_GAIN, masterVolume)
        
        // Update BGM volume
        bgmSource?.setVolume(bgmVolume * masterVolume)
    }

    /**
     * Set background music volume
     */
    fun setBGMVolume(volume: Float) {
        bgmVolume = volume.coerceIn(0f, 1f)
        bgmSource?.setVolume(bgmVolume * masterVolume)
    }

    /**
     * Set sound effects volume
     */
    fun setSFXVolume(volume: Float) {
        sfxVolume = volume.coerceIn(0f, 1f)
        // Note: Active SFX will keep their current volume until they finish
    }

    /**
     * Get master volume
     */
    fun getMasterVolume(): Float = masterVolume

    /**
     * Get BGM volume
     */
    fun getBGMVolume(): Float = bgmVolume

    /**
     * Get SFX volume
     */
    fun getSFXVolume(): Float = sfxVolume

    /**
     * Update listener (player) position for 3D audio
     */
    fun setListenerPosition(x: Float, y: Float, z: Float) {
        val buffer = BufferUtils.createFloatBuffer(3)
        buffer.put(floatArrayOf(x, y, z)).flip()
        alListenerfv(AL_POSITION, buffer)
    }

    /**
     * Update listener orientation
     */
    fun setListenerOrientation(forwardX: Float, forwardY: Float, forwardZ: Float, 
                             upX: Float, upY: Float, upZ: Float) {
        val buffer = BufferUtils.createFloatBuffer(6)
        buffer.put(floatArrayOf(forwardX, forwardY, forwardZ, upX, upY, upZ)).flip()
        alListenerfv(AL_ORIENTATION, buffer)
    }

    /**
     * Get an available audio source from the pool
     */
    private fun getAvailableSource(): AudioSource? {
        return availableSources.firstOrNull()
    }

    /**
     * Get audio system statistics
     */
    fun getStats(): AudioStats {
        return AudioStats(
            totalSources = audioSources.size,
            activeSources = activeSources.size,
            availableSources = availableSources.size,
            isBGMPlaying = isBGMPlaying(),
            masterVolume = masterVolume,
            bgmVolume = bgmVolume,
            sfxVolume = sfxVolume
        )
    }

    /**
     * Cleanup all audio resources
     */
    fun cleanup() {
        if (!isInitialized) return

        Logger.info("Cleaning up AudioManager...")

        stopAll()
        
        // Dispose all sources
        audioSources.forEach { it.dispose() }
        audioSources.clear()
        availableSources.clear()
        activeSources.clear()
        
        bgmSource?.dispose()
        bgmSource = null
        currentBGMBuffer = null

        isInitialized = false
        Logger.info("AudioManager cleaned up")
    }

    /**
     * Check if the audio manager is ready to use
     */
    fun isReady(): Boolean = isInitialized && AudioContext.isReady()

    /**
     * Data class for audio system statistics
     */
    data class AudioStats(
        val totalSources: Int,
        val activeSources: Int,
        val availableSources: Int,
        val isBGMPlaying: Boolean,
        val masterVolume: Float,
        val bgmVolume: Float,
        val sfxVolume: Float
    )
}