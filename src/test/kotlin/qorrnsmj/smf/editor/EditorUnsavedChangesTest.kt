package qorrnsmj.smf.editor

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EditorUnsavedChangesTest {
    @Test
    fun terrainAndPaintChangesAppearAndClearWhenRestored() {
        val terrain = EditorTerrainData(5)
        val splatmaps = EditorSplatmapData(5)
        val state = EditorUnsavedState()
        state.markClean("level", terrain, splatmaps)

        assertFalse(state.hasChanges("level", terrain, splatmaps))
        terrain.set(2, 3, 12f)
        assertTrue(state.hasChanges("level", terrain, splatmaps))
        terrain.set(2, 3, 0f)
        assertFalse(state.hasChanges("level", terrain, splatmaps))

        splatmaps.paint(2, 3, 0, 1f)
        assertTrue(state.hasChanges("level", terrain, splatmaps))
        splatmaps.replace(splatmaps.width, splatmaps.height, 0, emptyMap())
        assertFalse(state.hasChanges("level", terrain, splatmaps))
    }

    @Test
    fun levelMetadataChangesAppear() {
        val terrain = EditorTerrainData(5)
        val splatmaps = EditorSplatmapData(5)
        val state = EditorUnsavedState()
        state.markClean("level", terrain, splatmaps)

        assertTrue(state.hasChanges("changed level", terrain, splatmaps))
    }
}
