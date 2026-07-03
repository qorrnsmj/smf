package qorrnsmj.smf.game.level

object LevelFactory {
    fun create(levelName: String): Level {
        val definition = LevelDefinitionLoader.load(levelName)
        return create(definition)
    }

    fun create(definition: LevelDefinition): Level {
        return JsonLevel(definition)
    }
}
