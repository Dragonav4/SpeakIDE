plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.14.0"
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

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Vosk — offline speech recognition with a Java API
    // Models are downloaded on first use (~50 MB for the small English model)
    // JNA is excluded because IntelliJ Platform already bundles it
    implementation("com.alphacephei:vosk:0.3.32") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }

    implementation("com.github.axet:TarsosDSP:2.4-1")

    // ml-llm plugin classes — needed to compile against ChatSessionHostListener,
    // ChatSession, ChatSessionState. compileOnly: provided at runtime by the plugin.
    val mlLlmHome = "${System.getProperty("user.home")}/Library/Application Support/JetBrains/IntelliJIdea2026.1/plugins/ml-llm"
    compileOnly(fileTree("$mlLlmHome/lib/modules") { include("*.jar") })
    compileOnly(files("$mlLlmHome/lib/ml-llm.jar"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.ktor:ktor-client-mock:2.3.12")

    intellijPlatform {
        local("/Applications/IntelliJ IDEA.app")

        localPlugin(file("${System.getProperty("user.home")}/Library/Application Support/JetBrains/IntelliJIdea2026.1/plugins/ml-llm"))

        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain(21)
}

val voskNativeDir = "${rootDir}/libs"

tasks.test {
    useJUnitPlatform()
    jvmArgs("-Djna.library.path=$voskNativeDir")
}

tasks.named<JavaExec>("runIde") {
    jvmArgs("-Djna.library.path=$voskNativeDir")
}

intellijPlatform {
    instrumentCode = false
}
