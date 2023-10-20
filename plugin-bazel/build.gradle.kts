import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.15.0"
    id("org.jetbrains.changelog") version "2.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}

dependencies {
    implementation("com.google.protobuf:protobuf-java:3.24.3")
    implementation("io.kotest:kotest-assertions-core:5.7.2")
    implementation("io.get-coursier:coursier_2.13:2.1.7")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.1")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.1")
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// Configure project's dependencies
repositories {
    mavenCentral()
}

// Set the JVM language level used to compile sources and generate files - Java 11 is required since 2020.3
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set(properties("pluginName"))
    version.set(properties("platformVersion"))
    type.set(properties("platformType"))

    pluginsRepositories {
        custom("https://plugins.jetbrains.com/plugins/nightly/20329")
    }

    val platformPlugins = properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty)
    val bspPlugin = findLatestCompatibleBspPlugin()
    plugins.set(platformPlugins + bspPlugin)
}

fun findLatestCompatibleBspPlugin(): String {
    val pluginId = "org.jetbrains.bsp"
    val buildVersion = "${properties("platformType")}-${properties("platformVersion")}"
    val version = findLatestCompatiblePluginVersion(pluginId, buildVersion, "nightly")
        ?: error("Couldn't find compatible BSP plugin version")

    return "$pluginId:$version"
}


fun findLatestCompatiblePluginVersion(id: String, buildVersion: String, channel: String): String? =
    PluginRepositoryFactory.create()
        .pluginManager
        .searchCompatibleUpdates(listOf(id), buildVersion, channel)
        .firstOrNull()
        ?.version

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    version.set(properties("pluginVersion"))
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
//qodana {
//    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
//    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
//    saveReport.set(true)
//    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
//}

detekt {
    autoCorrect = true
    ignoreFailures = false
    buildUponDefaultConfig = false
    config.from(files("$rootDir/detekt.yml"))
    parallel = true
}

tasks {

    patchPluginXml {
        version.set(properties("pluginVersion"))
        sinceBuild.set(properties("pluginSinceBuild"))
        untilBuild.set(properties("pluginUntilBuild"))

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription.set(
            projectDir.resolve("README.md").readText().lines().run {
                val start = "<!-- Plugin description -->"
                val end = "<!-- Plugin description end -->"

                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end))
            }.joinToString("\n").run { markdownToHTML(this) }
        )

        // Get the latest available change notes from the changelog file
        changeNotes.set(provider { changelog.renderItem(changelog.getLatest(), Changelog.OutputType.HTML) })
    }

    // Configure UI tests plugin
    // Read more: https://github.com/JetBrains/intellij-ui-test-robot
    runIdeForUiTests {
        systemProperty("robot-server.port", "8082")
        systemProperty("ide.mac.message.dialogs.as.sheets", "false")
        systemProperty("jb.privacy.policy.text", "<!--999.999-->")
        systemProperty("jb.consents.confirmation.enabled", "false")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token.set(System.getenv("PUBLISH_TOKEN"))
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
    }

// Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = properties("javaVersion")
        targetCompatibility = properties("javaVersion")
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = properties("javaVersion")
    }

    withType<KotlinCompile> {
        kotlinOptions.languageVersion = properties("kotlinVersion")
        kotlinOptions.apiVersion = properties("kotlinVersion")
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")
