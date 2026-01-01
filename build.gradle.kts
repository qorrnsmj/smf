group = "qorrnsmj"
version = "0.2.2"

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    val lwjglVersion = "3.3.3"
    val nativeTarget = "natives-windows"

    implementation(kotlin("stdlib-jdk8"))
    implementation("de.javagl:jgltf-model:2.0.4")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:tinylog-api-kotlin:2.7.0")

    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-assimp:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$nativeTarget")
}

tasks {
    shadowJar {
        archiveBaseName = "SMF"
        archiveVersion = "${rootProject.version}"
        archiveClassifier = ""
        manifest {
            attributes["Main-Class"] = "qorrnsmj.smf.SMF"
        }
    }
}
