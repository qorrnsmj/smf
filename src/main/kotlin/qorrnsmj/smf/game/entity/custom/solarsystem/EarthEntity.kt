package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.EntityModels

class EarthEntity : PlanetEntity(EntityModels.EMPTY) {
    override val sunDistance = 2.0f
    override val spinSpeed = 1f / 1f
    override val orbitSpeed = 1f / 1f
    override val axialTilt = 23.4f
}
