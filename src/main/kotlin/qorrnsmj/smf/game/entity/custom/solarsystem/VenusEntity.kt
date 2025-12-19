package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.model.EntityModels

class VenusEntity : PlanetEntity(EntityModels.VENUS) {
    override val sunDistance = 1.5f
    override val spinSpeed = -1f / 243f
    override val orbitSpeed = 1f / 0.615f
    override val axialTilt = 177f
}
