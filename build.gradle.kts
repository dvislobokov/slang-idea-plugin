import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.0.21"
    // The version comes from the settings plugin in settings.gradle.kts (2.9.0,
    // the newest 2.x that still runs on Gradle 8.10; 2.10+ needs Gradle 8.13).
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// IntelliJ Platform 2024.2 (242) runs on and is compiled for Java 21.
kotlin {
    jvmToolchain(21)
    compilerOptions {
        // Do not use stdlib API newer than the Kotlin bundled with 2024.2.
        apiVersion = KotlinVersion.KOTLIN_1_9
    }
}

sourceSets {
    main {
        // Grammar-Kit / JFlex output (committed, see CLAUDE.md).
        java.srcDir("src/main/gen")
    }
}

dependencies {
    intellijPlatform {
        // The platform LSP API (com.intellij.platform.lsp.api) is only in commercial IDEs.
        intellijIdeaUltimate(providers.gradleProperty("platformVersion"))
        // TODO(verify): if compileKotlin cannot resolve com.intellij.platform.lsp.*, the LSP
        // API jar is a separate platform module in this IDE layout; add it explicitly:
        //   bundledModule("intellij.platform.lsp.impl")
        // (https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html)
        testFramework(TestFrameworkType.Platform)
        pluginVerifier()
        zipSigner()
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    // No forms and no @NotNull instrumentation needed; avoids the extra
    // instrumentation-tools download.
    instrumentCode = false
    buildSearchableOptions = false

    pluginConfiguration {
        id = "org.zell.idea"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = providers.gradleProperty("pluginUntilBuild")
        }
    }

    pluginVerification {
        ides {
            // The plugin must load in Community (without LSP) and in Ultimate (with LSP).
            // TODO(verify): these exact releases resolve (`./gradlew printProductsReleases`);
            // `recommended()` is the alternative.
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2024.2.4")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2024.2.4")
            create(IntelliJPlatformType.IntellijIdeaUltimate, "2025.2")
            create(IntelliJPlatformType.GoLand, "2024.2.3")
        }
    }
}

// ---------------------------------------------------------------------------
// Grammar-Kit: lexer from src/main/grammar/Zell.flex, parser + PSI from
// src/main/grammar/Zell.bnf (copied from the zell repo by tools/sync-zell.sh).
// ---------------------------------------------------------------------------

val generateZellLexer by tasks.registering(GenerateLexerTask::class) {
    sourceFile = layout.projectDirectory.file("src/main/grammar/Zell.flex")
    // purgeOldFiles wipes this whole directory: keep it lexer-only.
    targetOutputDir = layout.projectDirectory.dir("src/main/gen/org/zell/idea/lexer")
    purgeOldFiles = true
}

val generateZellParser by tasks.registering(GenerateParserTask::class) {
    sourceFile = layout.projectDirectory.file("src/main/grammar/Zell.bnf")
    targetRootOutputDir = layout.projectDirectory.dir("src/main/gen")
    pathToParser = "org/zell/idea/parser/ZellParser.java"
    pathToPsiRoot = "org/zell/idea/psi"
    purgeOldFiles = true
}

tasks {
    compileJava { dependsOn(generateZellLexer, generateZellParser) }
    compileKotlin { dependsOn(generateZellLexer, generateZellParser) }

    test {
        // The parser corpus test reads build/zell (tools/sync-zell.sh).
        systemProperty("zell.corpus", layout.buildDirectory.dir("zell").get().asFile.absolutePath)
        inputs.dir(layout.buildDirectory.dir("zell")).optional().withPropertyName("zellCorpus")
    }
}
