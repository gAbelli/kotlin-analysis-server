val analysisApiKotlinVersion: String by project
val intellijVersion: String by project

plugins {
    // Apply the Kotlin JVM Plugin to add support for Kotlin.
    kotlin("jvm")

    // Apply the application plugin to add support for building a CLI application in Java.
    application

    distribution
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()

    // Add Maven repos for Kotlin compiler etc.
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    maven(url = "https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-ide-plugin-dependencies")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-third-party-dependencies")
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    // Kotlin standard library
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.4")
    // LSP library
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:0.14.0")
    // IntelliJ IDEA APIs distributed as a library (required by the analysis API and Kotlin compiler)
    implementation("com.jetbrains.intellij.platform:core:$intellijVersion")
    implementation("com.jetbrains.intellij.platform:core-impl:$intellijVersion")
    implementation("com.jetbrains.intellij.platform:util:$intellijVersion")
    // Kotlin compiler and analysis API
    // See https://github.com/google/ksp/blob/319ddf/kotlin-analysis-api/build.gradle.kts
    implementation("org.jetbrains.kotlin:kotlin-compiler:$analysisApiKotlinVersion")
    implementation("org.jetbrains.kotlin:high-level-api-fir-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:high-level-api-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:low-level-api-fir-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:analysis-api-platform-interface-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:symbol-light-classes-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:analysis-api-standalone-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:high-level-api-impl-base-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:kotlin-compiler-common-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:kotlin-compiler-fir-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
    implementation("org.jetbrains.kotlin:kotlin-compiler-ir-for-ide:$analysisApiKotlinVersion") {
        isTransitive = false
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

testing {
    suites {
        // Configure the built-in test suite
        val test by getting(JvmTestSuite::class) {
            // Use Kotlin Test test framework
            useKotlinTest()
        }
    }
}

application {
    // Define the main class for the application.
    mainClass.set("dev.fwcd.kas.MainKt")
}
