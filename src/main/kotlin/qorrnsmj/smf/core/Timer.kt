package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFW

/**
 * The timer class is used for calculating delta time and also FPS and UPS
 * calculation.
 *
 * @author Heiko Brumme
 */
class Timer {
    private var fps = 0
    private var ups = 0
    private var fpsCount = 0
    private var upsCount = 0
    private var timeCount = 0f
    var lastLoopTime = 0.0

    /**
     * Initializes the timer.
     */
    init {
        lastLoopTime = getTime()
    }

    /**
     * Updates the FPS counter.
     */
    fun updateFPS() {
        fpsCount++
    }

    /**
     * Updates the UPS counter.
     */
    fun updateUPS() {
        upsCount++
    }

    /**
     * Updates FPS and UPS if a whole second has passed.
     */
    fun update() {
        if (timeCount > 1f) {
            fps = fpsCount
            fpsCount = 0

            ups = upsCount
            upsCount = 0

            timeCount -= 1f
        }
    }

    /**
     * Getter for the FPS.
     *
     * @return Frames per second
     */
    fun getFPS(): Int {
        return if (fps > 0) fps else fpsCount
    }

    /**
     * Getter for the UPS.
     *
     * @return Updates per second
     */
    fun getUPS(): Int {
        return if (ups > 0) ups else upsCount
    }

    /**
     * Returns the time elapsed since `glfwInit()` in seconds.
     *
     * @return System time in seconds
     */
    fun getTime(): Double {
        return GLFW.glfwGetTime()
    }

    /**
     * Returns the time that have passed since the last loop.
     *
     * @return Delta time in seconds
     */
    fun getDelta(): Float {
        val time = getTime()
        val delta = (time - lastLoopTime).toFloat()
        lastLoopTime = time
        timeCount += delta
        return delta
    }
}
