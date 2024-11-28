package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

// TODO: 手直し
abstract class FixedTimestepGame : Game() {
    lateinit var timer: Timer

    override fun gameLoop() {
        var alpha: Float
        var delta: Float
        var accumulator = 0f
        val interval = 1f / TARGET_UPS

        running = true
        while (running) {
            // Checks if game should close
            if (glfwWindowShouldClose(window.id)) {
                running = false
            }

            // Gets delta time and update the accumulator
            delta = timer.getDelta()
            accumulator += delta

            // Handles input
            input()

            // Updates game and timer UPS if enough time has passed
            // Calculates alpha value for interpolation
            while (accumulator >= interval) {
                update()
                timer.updateUPS()
                accumulator -= interval
            }
            alpha = accumulator / interval

            // Renders game
            render(alpha)

            // Updates timer and window
            timer.updateFPS()
            timer.update()
            window.update()

            // Synchronizes if v-sync is disabled
            if (!window.vsync) {
                sync(TARGET_FPS)
            }
        }
    }

    protected fun update(delta: Float = 1f / TARGET_UPS) {
        stateMachine.update(delta)
    }

    protected fun render(alpha: Float = 1f) {
        stateMachine.render(alpha)
    }

    protected fun sync(fps: Int) {
        val lastLoopTime = timer.lastLoopTime
        var now = timer.getTime()
        val targetTime = 1f / fps

        while (now - lastLoopTime < targetTime) {
            Thread.yield()

            // This is optional if you want your game to stop consuming too much CPU,
            // but you will lose some accuracy because Thread.sleep(1)
            // could sleep longer than 1 millisecond
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                throw RuntimeException(e)
            }

            now = timer.getTime()
        }
    }

    companion object {
        const val TARGET_FPS = 60
        const val TARGET_UPS = 30
    }
}
