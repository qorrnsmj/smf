package qorrnsmj.smf.util.impl

/**
 * Represents objects that can be resized to specific dimensions.
 * Implementing classes must define how to adjust to the given width and height.
 */
interface Resizable {
    fun resize(width: Int, height: Int)
}
