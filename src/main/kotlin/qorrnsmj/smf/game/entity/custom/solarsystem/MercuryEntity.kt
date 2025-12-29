package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.EntityModels

class MercuryEntity : PlanetEntity(EntityModels.EMPTY) {
    override val sunDistance = 1f
    override val spinSpeed = -1f / 58.7f
    override val orbitSpeed = 1f / 0.241f
    override val axialTilt = 0.01f
}
