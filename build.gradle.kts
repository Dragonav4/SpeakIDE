plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.0.1"
}

group = "com.danilian.speakide"
version = "0.1.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    // Ktor client for OpenAI Whisper API - using 2.3.12 to avoid conflict with IntelliJ Platform
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Vosk — offline speech recognition with a Java API
    // Models are downloaded on first use (~50 MB for the small English model)
    implementation("com.alphacephei:vosk:0.3.45")

    implementation("com.github.axet:TarsosDSP:2.4-1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.ktor:ktor-client-mock:2.3.12")

    intellijPlatform {
        intellijIdeaCommunity("2024.3.3")

        instrumentationTools()
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}


intellijPlatform {
    instrumentCode = false
}
