package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.EntityModels

class NeptuneEntity : PlanetEntity(EntityModels.EMPTY) {
    override val sunDistance = 7f
    override val spinSpeed = 1f / 0.671f
    override val orbitSpeed = 1f / 165f
    override val axialTilt = 28.3f
}
