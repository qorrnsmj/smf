package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.model.EntityModels

class JupiterEntity : PlanetEntity(EntityModels.JUPITER) {
    override val sunDistance = 4f
    override val spinSpeed = 1f / 0.414f
    override val orbitSpeed = 1f / 11.9f
    override val axialTilt = 25.2f
}
