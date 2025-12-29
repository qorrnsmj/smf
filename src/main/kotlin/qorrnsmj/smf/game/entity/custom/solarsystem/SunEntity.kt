package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.EntityModels

class SunEntity : PlanetEntity(EntityModels.EMPTY) {
    override val sunDistance = 0f
    override val spinSpeed = 1f / 27.3f
    override val orbitSpeed = 0f
    override val axialTilt = 7.25f
}
