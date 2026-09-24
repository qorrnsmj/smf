package qorrnsmj.smf.editor

internal class EditorUnsavedState {
    private var savedMapJson: String? = null
    private var savedTerrainSize = 0 to 0
    private var savedTerrainHeights = FloatArray(0)
    private var checkedTerrainRevision = -1L
    private var cachedTerrainDirty = false
    private var savedSplatSize = 0 to 0
    private var savedSplatMaxTextureIndex = 0
    private var savedSplatMaps = emptyMap<Int, FloatArray>()
    private var checkedSplatRevision = -1L
    private var cachedSplatDirty = false

    fun hasChanges(mapJson: String, terrain: EditorTerrainData, splatmaps: EditorSplatmapData): Boolean {
        val saved = savedMapJson ?: return false
        return mapJson != saved || terrainDirty(terrain) || splatDirty(splatmaps)
    }

    fun markClean(mapJson: String, terrain: EditorTerrainData, splatmaps: EditorSplatmapData) {
        savedMapJson = mapJson
        markTerrainClean(terrain)
        markSplatClean(splatmaps)
    }

    fun terrainDirty(terrain: EditorTerrainData): Boolean {
        if (checkedTerrainRevision != terrain.revision) {
            cachedTerrainDirty = savedTerrainSize != (terrain.width to terrain.height) ||
                !savedTerrainHeights.contentEquals(terrain.heights)
            checkedTerrainRevision = terrain.revision
        }
        return cachedTerrainDirty
    }

    fun splatDirty(splatmaps: EditorSplatmapData): Boolean {
        if (checkedSplatRevision != splatmaps.revision) {
            val currentMaps = splatmaps.snapshotMaps()
            cachedSplatDirty = savedSplatSize != (splatmaps.width to splatmaps.height) ||
                savedSplatMaxTextureIndex != splatmaps.maxTextureIndex ||
                currentMaps.keys != savedSplatMaps.keys ||
                currentMaps.any { (index, data) -> !data.contentEquals(savedSplatMaps.getValue(index)) }
            checkedSplatRevision = splatmaps.revision
        }
        return cachedSplatDirty
    }

    fun markTerrainClean(terrain: EditorTerrainData) {
        savedTerrainSize = terrain.width to terrain.height
        savedTerrainHeights = terrain.heights.copyOf()
        checkedTerrainRevision = terrain.revision
        cachedTerrainDirty = false
    }

    fun markSplatClean(splatmaps: EditorSplatmapData) {
        savedSplatSize = splatmaps.width to splatmaps.height
        savedSplatMaxTextureIndex = splatmaps.maxTextureIndex
        savedSplatMaps = splatmaps.snapshotMaps()
        checkedSplatRevision = splatmaps.revision
        cachedSplatDirty = false
    }
}
