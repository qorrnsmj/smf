package qorrnsmj.smf.graphic.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RenderProfileTest {
    @Test
    fun `ids match enum names`() {
        assertEquals(RenderProfile.SHADOWED, RenderProfile.fromId("SHADOWED"))
        assertEquals(RenderProfile.UNSHADOWED, RenderProfile.fromId("UNSHADOWED"))
        assertEquals(RenderProfile.SHADOWED, RenderProfile.fromId(null))
    }

    @Test
    fun `legacy aliases are rejected`() {
        assertFailsWith<IllegalStateException> {
            RenderProfile.fromId("shadowed")
        }
    }
}
