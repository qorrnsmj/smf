group = "qorrnsmj"
version = "1.0.0"

plugins {
    kotlin("jvm") version "1.9.24"
    id("java")
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

dependencies {
    val lwjglVersion = "3.3.3"
    val nativeTarget = "natives-windows"

    implementation(kotlin("stdlib-jdk8"))
    implementation("dev.romainguy:kotlin-math:1.5.3")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:tinylog-api-kotlin:2.7.0")

    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    //implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$nativeTarget")
    //implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
}

tasks {
    shadowJar {
        archiveBaseName.set("Test11_1")
        archiveVersion.set("${rootProject.version}")
        archiveClassifier.set("")
        manifest {
            attributes["Main-Class"] = "qorrnsmj.test.t11.Test11_1"
        }
    }
}
