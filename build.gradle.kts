import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "qorrnsmj"
version = "0.4.1"

plugins {
    kotlin("jvm") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(22)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

val javaToolchainService = extensions.getByType<JavaToolchainService>()

dependencies {
    val lwjglVersion = "3.3.3"
    val imguiVersion = "1.90.0"
    val nativeTarget = "natives-windows"

    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("de.javagl:jgltf-model:2.0.4")
    implementation("org.tinylog:tinylog-impl:2.7.0")
    implementation("org.tinylog:tinylog-api-kotlin:2.7.0")

    implementation("org.lwjgl:lwjgl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-stb:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-assimp:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-glfw:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-opengl:$lwjglVersion")
    implementation("org.lwjgl:lwjgl-openal:$lwjglVersion")
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")

    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-assimp:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$nativeTarget")
    runtimeOnly("org.lwjgl:lwjgl-openal:$lwjglVersion:$nativeTarget")
    runtimeOnly("io.github.spair:imgui-java-natives-windows:$imguiVersion")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_22)
        }
    }

    test {
        useJUnitPlatform()
    }

    val smfMainClass = "qorrnsmj.smf.SMF"
    val javaLauncher22 = javaToolchainService.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(22))
    }

    register<JavaExec>("runGame") {
        group = "application"
        description = "Runs SMF in game mode."
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(smfMainClass)
        javaLauncher.set(javaLauncher22)
    }

    register<JavaExec>("runEditor") {
        group = "application"
        description = "Runs SMF in editor mode."
        classpath = sourceSets["main"].runtimeClasspath
        mainClass.set(smfMainClass)
        args("--editor")
        javaLauncher.set(javaLauncher22)
    }

    shadowJar {
        archiveBaseName = "SMF"
        archiveVersion = "${rootProject.version}"
        archiveClassifier = ""
        manifest {
            attributes["Main-Class"] = "qorrnsmj.smf.SMF"
        }
    }
}
