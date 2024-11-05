package qorrnsmj.smf.core

import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import qorrnsmj.smf.core.Game
import qorrnsmj.smf.state.States.EXAMPLE1
import qorrnsmj.smf.state.States.EXAMPLE2
import qorrnsmj.smf.state.custom.ExampleState1
import qorrnsmj.smf.state.custom.ExampleState2

// 自分のに手直し
abstract class FixedTimestepGame : Game() {
    override fun gameLoop() {
        var alpha: Float
        var delta: Float
        var accumulator = 0f
        val interval = 1f / TARGET_UPS

        while (running) {
            // Check if game should close
            if (glfwWindowShouldClose(window.id)) {
                running = false
            }

            // Get delta time and update the accumulator
            delta = timer.getDelta()
            accumulator += delta

            // Handle input
            input()

            // Update game and timer UPS if enough time has passed
            // Calculate alpha value for interpolation
            while (accumulator >= interval) {
                update()
                timer.updateUPS()
                accumulator -= interval
            }
            alpha = accumulator / interval

            // Render game
            render(alpha)

            // Update timer and window
            timer.updateFPS()
            timer.update()
            window.update()

            // Synchronize if v-sync is disabled
            if (!window.isVSyncEnabled) {
                sync(TARGET_FPS)
            }
        }
    }
}
