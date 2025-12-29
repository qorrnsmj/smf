package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.EntityModels

class SaturnEntity : PlanetEntity(EntityModels.EMPTY) {
    override val sunDistance = 6f
    override val spinSpeed = 1f / 0.426f
    override val orbitSpeed = 1f / 29.5f
    override val axialTilt = 26.7f
}
