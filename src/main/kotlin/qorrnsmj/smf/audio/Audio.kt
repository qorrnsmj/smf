package qorrnsmj.smf.audio

import org.tinylog.kotlin.Logger

/**
 * Singleton for managing loaded audio resources, following SMF's resource management pattern
 * Similar to Textures, EntityModels, Skyboxes, etc.
 */
object Audio {
    private val bgmBuffers = mutableMapOf<String, AudioBuffer>()
    private val sfxBuffers = mutableMapOf<String, AudioBuffer>()
    private var isLoaded = false

    /**
     * Load all audio resources from the assets folder
     */
    fun load() {
        if (isLoaded) {
            Logger.warn("Audio resources already loaded")
            return
        }

        Logger.info("Loading audio resources...")

        try {
            // Load BGM files
            loadBGMFiles()
            
            // Load SFX files  
            loadSFXFiles()

            isLoaded = true
            Logger.info("Audio resources loaded successfully (BGM: ${bgmBuffers.size}, SFX: ${sfxBuffers.size})")
        } catch (e: Exception) {
            Logger.error(e, "Failed to load audio resources")
            cleanup() // Cleanup any partially loaded resources
            throw e
        }
    }

    /**
     * Load background music files from assets/audio/bgm/
     */
    private fun loadBGMFiles() {
        val bgmPath = "assets/audio/bgm"
        val bgmFiles = findAudioFiles(bgmPath)
        
        bgmFiles.forEach { filename ->
            try {
                val resourcePath = "$bgmPath/$filename"
                val buffer = AudioLoader.loadOGG(resourcePath)
                val name = filename.substringBeforeLast('.') // Remove extension
                bgmBuffers[name] = buffer
                Logger.debug("Loaded BGM: $name")
            } catch (e: Exception) {
                Logger.warn("Failed to load BGM file: $filename - ${e.message}")
            }
        }
    }

    /**
     * Load sound effect files from assets/audio/se/
     */
    private fun loadSFXFiles() {
        val sfxPath = "assets/audio/se"
        val sfxFiles = findAudioFiles(sfxPath)
        
        sfxFiles.forEach { filename ->
            try {
                val resourcePath = "$sfxPath/$filename"
                val buffer = AudioLoader.loadOGG(resourcePath)
                val name = filename.substringBeforeLast('.') // Remove extension
                sfxBuffers[name] = buffer
                Logger.debug("Loaded SFX: $name")
            } catch (e: Exception) {
                Logger.warn("Failed to load SFX file: $filename - ${e.message}")
            }
        }
    }

    /**
     * Find all supported audio files in a resource directory
     * This is a simple implementation - in practice you might want to use
     * resource scanning libraries or list files explicitly
     */
    private fun findAudioFiles(basePath: String): List<String> {
        val knownFiles = when (basePath) {
            "assets/audio/bgm" -> listOf("test_bgm.ogg")
            "assets/audio/se" -> listOf("test_se.ogg")
            else -> emptyList()
        }
        
        return knownFiles.filter { AudioLoader.isSupportedFormat(it) }
    }

    /**
     * Get a BGM buffer by name
     * @param name Name of the BGM file (without extension)
     * @return AudioBuffer or null if not found
     */
    fun getBGM(name: String): AudioBuffer? {
        if (!isLoaded) {
            Logger.warn("Audio resources not loaded yet")
            return null
        }
        return bgmBuffers[name]
    }

    /**
     * Get an SFX buffer by name
     * @param name Name of the SFX file (without extension)
     * @return AudioBuffer or null if not found
     */
    fun getSFX(name: String): AudioBuffer? {
        if (!isLoaded) {
            Logger.warn("Audio resources not loaded yet")
            return null
        }
        return sfxBuffers[name]
    }

    /**
     * Get all loaded BGM names
     */
    fun getBGMNames(): Set<String> = bgmBuffers.keys

    /**
     * Get all loaded SFX names
     */
    fun getSFXNames(): Set<String> = sfxBuffers.keys

    /**
     * Check if a specific BGM is loaded
     */
    fun hasBGM(name: String): Boolean = bgmBuffers.containsKey(name)

    /**
     * Check if a specific SFX is loaded
     */
    fun hasSFX(name: String): Boolean = sfxBuffers.containsKey(name)

    /**
     * Load a single audio file dynamically
     * @param name Identifier for the audio
     * @param resourcePath Path to the resource
     * @param isBGM True if this is background music, false if SFX
     * @return AudioBuffer or null if loading failed
     */
    fun loadAudio(name: String, resourcePath: String, isBGM: Boolean): AudioBuffer? {
        try {
            val buffer = AudioLoader.loadOGG(resourcePath)
            if (isBGM) {
                bgmBuffers[name] = buffer
                Logger.info("Dynamically loaded BGM: $name")
            } else {
                sfxBuffers[name] = buffer
                Logger.info("Dynamically loaded SFX: $name")
            }
            return buffer
        } catch (e: Exception) {
            Logger.error(e, "Failed to dynamically load audio: $name")
            return null
        }
    }

    /**
     * Remove a specific audio buffer
     */
    fun removeAudio(name: String, isBGM: Boolean) {
        if (isBGM) {
            bgmBuffers.remove(name)?.dispose()
        } else {
            sfxBuffers.remove(name)?.dispose()
        }
    }

    /**
     * Get audio resource statistics
     */
    fun getStats(): AudioResourceStats {
        return AudioResourceStats(
            bgmCount = bgmBuffers.size,
            sfxCount = sfxBuffers.size,
            totalBuffers = bgmBuffers.size + sfxBuffers.size,
            isLoaded = isLoaded
        )
    }

    /**
     * Cleanup all loaded audio resources
     */
    fun cleanup() {
        Logger.info("Cleaning up audio resources...")
        
        bgmBuffers.values.forEach { it.dispose() }
        bgmBuffers.clear()
        
        sfxBuffers.values.forEach { it.dispose() }
        sfxBuffers.clear()
        
        isLoaded = false
        Logger.info("Audio resources cleaned up")
    }

    /**
     * Check if audio resources are loaded
     */
    fun isLoaded(): Boolean = isLoaded

    /**
     * Data class for audio resource statistics
     */
    data class AudioResourceStats(
        val bgmCount: Int,
        val sfxCount: Int,
        val totalBuffers: Int,
        val isLoaded: Boolean
    )

    // Convenience methods for common operations

    /**
     * Play a BGM track by name
     */
    fun playBGM(name: String, volume: Float = 1.0f, loop: Boolean = true): Boolean {
        val buffer = getBGM(name) ?: return false
        AudioManager.playBGM(buffer, volume, loop)
        return true
    }

    /**
     * Play an SFX by name
     */
    fun playSFX(name: String, volume: Float = 1.0f, pitch: Float = 1.0f, loop: Boolean = false): AudioSource? {
        val buffer = getSFX(name) ?: return null
        return AudioManager.playSFX(buffer, volume, pitch, loop)
    }
}
