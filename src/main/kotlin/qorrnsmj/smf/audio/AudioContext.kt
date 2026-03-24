package qorrnsmj.smf.audio

import org.lwjgl.openal.AL
import org.lwjgl.openal.ALC
import org.lwjgl.openal.ALC10.*
import org.lwjgl.openal.ALCCapabilities
import org.lwjgl.system.MemoryUtil.NULL
import org.tinylog.kotlin.Logger

/**
 * Manages OpenAL context and device initialization/cleanup
 */
object AudioContext {
    private var device: Long = NULL
    private var context: Long = NULL
    private var isInitialized = false

    /**
     * Initialize OpenAL context and device
     */
    fun initialize() {
        if (isInitialized) {
            Logger.warn("AudioContext already initialized")
            return
        }

        Logger.info("Initializing OpenAL context...")

        // Open default audio device
        device = alcOpenDevice(null as String?)
        if (device == NULL) {
            throw RuntimeException("Failed to open OpenAL device")
        }

        // Create OpenAL context
        context = alcCreateContext(device, null as IntArray?)
        if (context == NULL) {
            alcCloseDevice(device)
            throw RuntimeException("Failed to create OpenAL context")
        }

        // Make context current
        if (!alcMakeContextCurrent(context)) {
            alcDestroyContext(context)
            alcCloseDevice(device)
            throw RuntimeException("Failed to make OpenAL context current")
        }

        // Create OpenAL capabilities for this context
        val alcCapabilities: ALCCapabilities = ALC.createCapabilities(device)
        AL.createCapabilities(alcCapabilities)

        isInitialized = true
        Logger.info("OpenAL context initialized successfully")
        
        // Log device information
        val deviceName = alcGetString(device, ALC_DEVICE_SPECIFIER)
        Logger.info("OpenAL Device: $deviceName")
    }

    /**
     * Cleanup OpenAL context and device
     */
    fun cleanup() {
        if (!isInitialized) {
            Logger.warn("AudioContext not initialized")
            return
        }

        Logger.info("Cleaning up OpenAL context...")

        // Make no context current
        alcMakeContextCurrent(NULL)

        // Destroy context
        if (context != NULL) {
            alcDestroyContext(context)
            context = NULL
        }

        // Close device
        if (device != NULL) {
            alcCloseDevice(device)
            device = NULL
        }

        isInitialized = false
        Logger.info("OpenAL context cleaned up successfully")
    }

    /**
     * Check if OpenAL context is initialized
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Get the current OpenAL device handle
     */
    fun getDevice(): Long = device

    /**
     * Get the current OpenAL context handle
     */
    fun getContext(): Long = context
}