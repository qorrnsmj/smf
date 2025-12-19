package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.model.EntityModels

class MarsEntity : PlanetEntity(EntityModels.MARS) {
    override val sunDistance = 2.5f
    override val spinSpeed = 1f / 1.03f
    override val orbitSpeed = 1f / 1.88f
    override val axialTilt = 25.2f
}
