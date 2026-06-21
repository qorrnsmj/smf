package qorrnsmj.smf.audio

import org.tinylog.kotlin.Logger
import qorrnsmj.smf.SMF

/**
 * Singleton for managing loaded audio resources, following SMF's resource management pattern.
 * Similar to `Textures` and other resource holders.
 */
object Audios {
    //lateinit var TEST_BGM: AudioBuffer
    //lateinit var TEST_SE: AudioBuffer

    fun load() {
        Logger.info("Loading audio resources...")

        //TEST_BGM = loadBGMAudio("test_bgm.ogg")
        //TEST_SE = loadSEAudio("test_se.ogg")

        Logger.info("Audio resources loaded successfully")
    }

    private fun loadBGMAudio(fileName: String): AudioBuffer {
        return AudioLoader.loadOGG("assets/audio/bgm/$fileName")
    }

    private fun loadSEAudio(fileName: String): AudioBuffer {
        return AudioLoader.loadOGG("assets/audio/se/$fileName")
    }
}
