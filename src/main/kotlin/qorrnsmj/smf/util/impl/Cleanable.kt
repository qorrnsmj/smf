package qorrnsmj.smf.util.impl

/**
 * Represents an object that can be cleaned up to release resources.
 *
 * Classes implementing this interface must provide a concrete implementation of the `cleanup` method
 * to define the resource cleanup behavior.
 */
interface Cleanable {
    fun cleanup()
}
