package qorrnsmj.smf.game.entity.custom.solarsystem

import qorrnsmj.smf.game.entity.model.EntityModels

class UranusEntity : PlanetEntity(EntityModels.URANUS) {
    override val sunDistance = 6f
    override val spinSpeed = -1f / 0.718f
    override val orbitSpeed = 1f / 84f
    override val axialTilt = 97.8f
}
