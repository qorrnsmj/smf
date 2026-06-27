package qorrnsmj.smf.game.task.cutscene

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import qorrnsmj.smf.game.camera.Camera
import qorrnsmj.smf.math.Vector3f

class CutsceneTest {
    @Test
    fun `timed event fires once when timeline crosses its time`() {
        var firedCount = 0
        val cutscene = object : Cutscene(Camera()) {
            init {
                event(1f) { firedCount++ }
                endAt(2f)
            }
        }

        cutscene.update(0.5f)
        assertEquals(0, firedCount)

        cutscene.update(0.5f)
        cutscene.update(0.5f)
        assertEquals(1, firedCount)
        assertFalse(cutscene.isFinished())

        cutscene.update(0.5f)
        assertEquals(1, firedCount)
        assertTrue(cutscene.isFinished())
    }

    @Test
    fun `reset makes timeline and events reusable`() {
        var firedCount = 0
        val cutscene = object : Cutscene(Camera()) {
            init {
                event(0.25f) { firedCount++ }
                endAt(0.5f)
            }
        }

        cutscene.update(0.5f)
        cutscene.reset()
        cutscene.update(0.5f)

        assertEquals(2, firedCount)
        assertTrue(cutscene.isFinished())
    }

    @Test
    fun `skip only fires events marked fire on skip`() {
        var ordinaryEvents = 0
        var importantEvents = 0
        val cutscene = object : Cutscene(Camera()) {
            init {
                event(2f, skipPolicy = CutsceneSkipPolicy.SKIP) { ordinaryEvents++ }
                event(3f, skipPolicy = CutsceneSkipPolicy.FIRE_ON_SKIP) { importantEvents++ }
                endAt(4f)
            }
        }

        cutscene.skip()

        assertEquals(0, ordinaryEvents)
        assertEquals(1, importantEvents)
        assertTrue(cutscene.isFinished())
    }

    @Test
    fun `visual cues extend duration and select active subtitle`() {
        val cutscene = object : Cutscene(Camera()) {
            init {
                subtitle(1f, 3f, "Hello")
                fade(0f, 2f, 1f, 0f)
            }
        }

        assertEquals(4f, cutscene.durationSeconds)
        cutscene.update(1.5f)

        assertEquals("Hello", cutscene.visualState().subtitle?.text)
        assertTrue(cutscene.visualState().fadeAlpha in 0f..1f)
    }

    @Test
    fun `camera reaches final keyframe`() {
        val camera = Camera()
        val cutscene = object : Cutscene(camera) {
            init {
                cameraKeyframe(0f, Vector3f(0f, 0f, 0f), Vector3f(0f, 0f, -1f))
                cameraKeyframe(2f, Vector3f(10f, 5f, -2f), Vector3f(0f, 0f, 0f))
            }
        }

        cutscene.update(2f)

        assertEquals(10f, camera.position.x)
        assertEquals(5f, camera.position.y)
        assertEquals(-2f, camera.position.z)
    }

    @Test
    fun `introduction finishes at player eye position without moving player`() {
        val cutsceneCamera = Camera()
        val playerEyePosition = Vector3f(100f, 37f, 100f)
        val playerFront = Vector3f(0f, 0f, -1f)
        val cutscene = IntroductionCutscene(
            camera = cutsceneCamera,
            focusPosition = playerEyePosition,
            destinationEyePosition = playerEyePosition,
            destinationFront = playerFront,
        )

        cutscene.update(cutscene.durationSeconds)

        assertEquals(playerEyePosition.x, cutsceneCamera.position.x)
        assertEquals(playerEyePosition.y, cutsceneCamera.position.y)
        assertEquals(playerEyePosition.z, cutsceneCamera.position.z)
        assertTrue(cutsceneCamera.getFront().distanceTo(playerFront) < 0.0001f)
    }
}
