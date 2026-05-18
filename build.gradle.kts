plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
    id("org.jetbrains.intellij.platform") version "2.14.0"
}

group = "com.danilian.speakide"
version = "0.1.4"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}
val ktorVersion = "3.4.3"

dependencies {
    implementation("io.ktor:ktor-client-core:$ktorVersion") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-cio:$ktorVersion") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion") {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "org.jetbrains.kotlin")
        exclude(group = "org.slf4j")
    }

    // JSON serialization (IntelliJ Platform bundles it, so compileOnly will compile but won't be bundled)
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Vosk — offline speech recognition with a Java API
    // Models are downloaded on first use (~50 MB for the small English model)
    // JNA is excluded because IntelliJ Platform already bundles it
    implementation("com.alphacephei:vosk:0.3.32") {
        exclude(group = "net.java.dev.jna", module = "jna")
    }
    implementation("io.github.givimad:whisper-jni:1.7.1")

    implementation("com.github.axet:TarsosDSP:2.4-1")

    // ml-llm plugin classes — needed to compile against ChatSessionHostListener,
    // ChatSession, ChatSessionState. compileOnly: provided at runtime by the plugin.
    val mlLlmHome =
        "${System.getProperty("user.home")}/Library/Application Support/JetBrains/IntelliJIdea2026.1/plugins/ml-llm"
    compileOnly(fileTree("$mlLlmHome/lib/modules") { include("*.jar") })
    compileOnly(files("$mlLlmHome/lib/ml-llm.jar"))

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

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

tasks.processResources {
    from(voskNativeDir) {
        include("libvosk.dylib")
        into("darwin-aarch64")
    }
    from(voskNativeDir) {
        include("libvosk.dylib")
        into("darwin-x86-64")
    }
}


