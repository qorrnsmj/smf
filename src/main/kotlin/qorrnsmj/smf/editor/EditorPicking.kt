package qorrnsmj.smf.editor

import org.lwjgl.glfw.GLFW.glfwGetCursorPos
import qorrnsmj.smf.SMF
import qorrnsmj.smf.math.Vector3f
import qorrnsmj.smf.math.Vector4f
import qorrnsmj.smf.util.MVP
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal object EditorPicking {
    fun currentMouseRay(context: EditorContext): Ray {
        val camera = context.activeCamera()
        val x = DoubleArray(1)
        val y = DoubleArray(1)
        glfwGetCursorPos(SMF.window.id, x, y)

        val localX = x[0].toFloat() - context.viewportX
        val localY = y[0].toFloat() - context.viewportY
        val ndcX = (2f * localX) / context.viewportWidth - 1f
        val ndcY = 1f - (2f * localY) / context.viewportHeight
        val clip = Vector4f(ndcX, ndcY, -1f, 1f)
        val projection = MVP.getPerspectiveMatrix(context.viewportWidth / context.viewportHeight)
        val eye = projection.invert().multiply(clip)
        val world = camera.getViewMatrix().invert().multiply(Vector4f(eye.x, eye.y, -1f, 0f))
        return Ray(camera.position, Vector3f(world.x, world.y, world.z).normalize())
    }

    fun intersectGround(ray: Ray): Vector3f? {
        if (abs(ray.direction.y) < 0.0001f) return null
        val distance = -ray.origin.y / ray.direction.y
        if (distance < 0f) return null
        return ray.origin.add(ray.direction.scale(distance))
    }

    fun pickObject(context: EditorContext, ray: Ray): Int {
        var bestIndex = -1
        var bestDistance = Float.POSITIVE_INFINITY

        for ((index, placed) in context.placedObjects.withIndex()) {
            val distance = intersectAabb(ray, placed.editorMin(), placed.editorMax()) ?: continue
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }

        return bestIndex
    }

    fun pickEventArea(context: EditorContext, ray: Ray): Int {
        var bestIndex = -1
        var bestDistance = Float.POSITIVE_INFINITY

        for ((index, area) in context.eventAreas.withIndex()) {
            val halfSize = area.size.scale(0.5f)
            val distance = intersectAabb(ray, area.position.subtract(halfSize), area.position.add(halfSize)) ?: continue
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }

        return bestIndex
    }

    private fun intersectAabb(ray: Ray, aabbMin: Vector3f, aabbMax: Vector3f): Float? {
        var tMin = 0f
        var tMax = Float.POSITIVE_INFINITY

        fun slab(origin: Float, direction: Float, slabMin: Float, slabMax: Float): Boolean {
            if (abs(direction) < 0.0001f) {
                return origin in slabMin..slabMax
            }

            var near = (slabMin - origin) / direction
            var far = (slabMax - origin) / direction
            if (near > far) {
                val temp = near
                near = far
                far = temp
            }

            tMin = max(tMin, near)
            tMax = min(tMax, far)
            return tMin <= tMax
        }

        if (!slab(ray.origin.x, ray.direction.x, aabbMin.x, aabbMax.x)) return null
        if (!slab(ray.origin.y, ray.direction.y, aabbMin.y, aabbMax.y)) return null
        if (!slab(ray.origin.z, ray.direction.z, aabbMin.z, aabbMax.z)) return null
        return tMin
    }
}
