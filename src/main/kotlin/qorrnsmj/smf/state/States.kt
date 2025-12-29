package qorrnsmj.smf.state

import qorrnsmj.smf.state.custom.EmptyState
import qorrnsmj.smf.state.custom.GltfState
import qorrnsmj.smf.state.custom.SolarSystemState

object States {
    val EMPTY = EmptyState()
    val GLTF = GltfState()
    val EXAMPLE1 = GltfState()
    val SOLAR_SYSTEM = SolarSystemState()
}
