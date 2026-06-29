package qorrnsmj.smf.game.level

object LevelFactory {
    fun create(levelName: String): Level {
        val definition = LevelDefinitionLoader.load(levelName)
        return create(definition)
    }

    fun create(definition: LevelDefinition): Level {
        val className = definition.className
        if (className.isNullOrBlank()) {
            return JsonLevel(definition)
        }

        val type = Class.forName(className)
        require(Level::class.java.isAssignableFrom(type)) {
            "Configured level class must extend Level: $className"
        }

        val constructorWithDefinition = type.constructors.firstOrNull { constructor ->
            constructor.parameterTypes.contentEquals(arrayOf(LevelDefinition::class.java))
        }
        if (constructorWithDefinition != null) {
            return constructorWithDefinition.newInstance(definition) as Level
        }

        val constructorWithString = type.constructors.firstOrNull { constructor ->
            constructor.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        if (constructorWithString != null) {
            return constructorWithString.newInstance(definition.resourcePath) as Level
        }

        return type.getDeclaredConstructor().newInstance() as Level
    }
}
