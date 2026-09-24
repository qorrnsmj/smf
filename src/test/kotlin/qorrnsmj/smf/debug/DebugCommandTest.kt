package qorrnsmj.smf.debug

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class DebugCommandTest {
    @Test
    fun validatesCommandsBeforeTheyReachTheGameThread() {
        assertEquals(listOf(1f, -2f, 3f), DebugCommand.parse("teleport 1 -2 3").values)
        assertEquals("stall", DebugCommand.parse("screenshot stall").label)
        assertEquals(600f, DebugCommand.parse("step 600").values.single())
        for (text in listOf("teleport NaN 0 0", "move Infinity 0 0", "step 0", "step 1.5",
            "step 601", "look 0 90", "screenshot ../escape", "status extra", "unknown", "teleport 1 2")) {
            assertFails(text) { DebugCommand.parse(text) }
        }
    }

    @Test
    fun jsonEscapesWindowsPathsAndControlCharacters() {
        assertEquals("\"C:\\\\test\\n\\\"image\\\"\"", DebugJson.encode("C:\\test\n\"image\""))
        assertEquals("{\"paused\":true,\"values\":[1,2]}", DebugJson.encode(mapOf("paused" to true, "values" to listOf(1, 2))))
    }
}
