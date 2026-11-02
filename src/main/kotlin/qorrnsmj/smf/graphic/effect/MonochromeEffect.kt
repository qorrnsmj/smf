package qorrnsmj.smf.graphic.effect

import qorrnsmj.smf.graphic.resource.shader.ShaderProgram

class MonochromeEffect() : Effect(program) {
    override fun use() {
        program.use()
    }

    override fun unuse() {
    }

    companion object {
        val program = ShaderProgram("effect/monochrome.vert", "effect/monochrome.frag")
    }
}
