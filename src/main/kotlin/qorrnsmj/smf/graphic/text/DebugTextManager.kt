package qorrnsmj.smf.graphic.text

import qorrnsmj.smf.graphic.text.FontLoader
import qorrnsmj.smf.graphic.text.Font
import qorrnsmj.smf.graphic.text.TextElement
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.util.Cleanable

/**
 * Manages debug text display for FPS and coordinate information
 */
class DebugTextManager : Cleanable {
    
    private var font: Font? = null
    private val debugElements = mutableListOf<TextElement>()
    
    fun initialize() {
        font = FontLoader.loadAssetFont("Inconsolata.ttf", 16f)
    }
    
    /**
     * Update debug information for this frame
     */
    fun updateDebugInfo(cameraPosition: Vector3f, fps: Int, ups: Int) {
        val currentFont = font ?: return
        val textColor = Vector3f(0.3f, 0.3f, 0.3f)
        
        debugElements.clear()
        
        // Add combined FPS/UPS display
        debugElements.add(
            TextElement(
                text = "FPS/UPS: $fps/$ups",
                font = currentFont,
                x = 10f,
                y = 25f,
                color = textColor
            )
        )
        
        // Add camera position
        debugElements.add(
            TextElement(
                text = String.format("Position: %.1f, %.1f, %.1f", 
                    cameraPosition.x, cameraPosition.y, cameraPosition.z),
                font = currentFont,
                x = 10f,
                y = 50f,
                color = textColor
            )
        )
        
        // Add instructions
        debugElements.add(
            TextElement(
                text = "Controls: WASD - Move, Space - Jump, Mouse - Look",
                font = currentFont,
                x = 10f,
                y = 75f,
                color = textColor
            )
        )
    }
    
    /**
     * Get current debug text elements for rendering
     */
    fun getDebugElements(): List<TextElement> = debugElements
    
    override fun cleanup() {
        font?.cleanup()
        debugElements.clear()
    }
}
