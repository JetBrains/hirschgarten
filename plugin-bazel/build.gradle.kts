import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.intellij.tasks.VerifyPluginTask

plugins {
    alias(libs.plugins.intellij)
    alias(libs.plugins.changelog)
    id("intellijbazel.kotlin-conventions")
    id("com.google.protobuf") version "0.9.4"
}

val myToken: String by project
val releaseChannel: String by project

dependencies {
    implementation(libs.bazelBsp) {
        exclude(group = "org.jetbrains.kotlinx")
        exclude(group = "ch.epfl.scala")
    }
    implementation("com.google.protobuf:protobuf-java:3.24.4")
    testImplementation(libs.junit5)
    testRuntimeOnly(libs.junitVintage)
    testImplementation(libs.kotest)
}

group = Plugin.group
version = Plugin.version

subprojects {
    apply(plugin = "org.jetbrains.intellij")
    intellij {
        version.set(Platform.snapshotVersion)
    }

    tasks.withType(PublishPluginTask::class.java) {
        enabled = false
    }

    tasks.withType(VerifyPluginTask::class.java) {
        enabled = false
    }

    tasks.withType(BuildSearchableOptionsTask::class.java) {
        enabled = false
    }

    tasks.withType(RunIdeTask::class.java) {
        enabled = false
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    pluginName.set(Plugin.name)
    version.set(Platform.snapshotVersion)
    type.set(Platform.type)

    pluginsRepositories {
        custom("https://plugins.jetbrains.com/plugins/nightly/20329")
    }

    val bspPlugin = findLatestCompatibleBspPlugin()
    plugins.set(Platform.plugins + bspPlugin)
}

fun findLatestCompatibleBspPlugin(): String {
    val pluginId = "org.jetbrains.bsp"
    val buildVersion = "${Platform.type}-${Platform.version}"
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
    version.set(Plugin.version)
    groups.set(emptyList())
}

// Configure Gradle Qodana Plugin - read more: https://github.com/JetBrains/gradle-qodana-plugin
//qodana {
//    cachePath.set(projectDir.resolve(".qodana").canonicalPath)
//    reportPath.set(projectDir.resolve("build/reports/inspections").canonicalPath)
//    saveReport.set(true)
//    showReport.set(System.getenv("QODANA_SHOW_REPORT")?.toBoolean() ?: false)
//}

tasks {

    patchPluginXml {
        version.set(Plugin.version)
        sinceBuild.set(Plugin.sinceBuild)
        untilBuild.set(Plugin.untilBuild)

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

    prepareSandbox {
        val runtimeConfiguration = project.configurations.getByName(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME)
        runtimeClasspathFiles.set(
            runtimeConfiguration.exclude(
                mapOf(
                    "group" to "org.jetbrains.kotlinx",
                    "module" to "kotlinx-coroutines-core",
                )
            )
        )

        runtimeClasspathFiles.set(
            runtimeConfiguration.exclude(
                mapOf(
                    "group" to "ch.epfl.scala",
                    "module" to "bsp4j",
                )
            )
        )

        runtimeClasspathFiles.set(
            runtimeConfiguration.exclude(
                mapOf(
                    "group" to "org.jetbrains.kotlin",
                    "module" to "kotlin-stdlib",
                )
            )
        )
    }

    runPluginVerifier {
        ideVersions.set(pluginVerifierIdeVersions.split(',').map(String::trim).filter(String::isNotEmpty))
        failureLevel.set(setOf(
            RunPluginVerifierTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            RunPluginVerifierTask.FailureLevel.NOT_DYNAMIC
        ))
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
        token.set(provider { myToken })
        // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        // Release channel is set via command-line param "releaseChannel"
        // Marketplace token is set via command-line parm "myToken"
        // Example command "./gradlew publishPlugin -PmyToken="perm:YOUR_TOKEN -PreleaseChannel=nightly"
        channels.set(provider { listOf(releaseChannel) })
    }

// Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    test {
        useJUnitPlatform()
        testLogging {
            events("PASSED", "SKIPPED", "FAILED")
        }

        // disable the malfunctioned platform listener com.intellij.tests.JUnit5TestSessionListener
        // this listener caused the CI tests to fail with
        // AlreadyDisposedException: Already disposed: Application (containerState DISPOSE_COMPLETED)
        // TODO: follow up https://youtrack.jetbrains.com/issue/IDEA-337508/AlreadyDisposedException-Already-disposed-Application-containerState-DISPOSECOMPLETED-after-junit5-tests-on-TeamCity
        systemProperty("intellij.build.test.ignoreFirstAndLastTests", "true")
    }
}

sourceSets["main"].java.srcDirs("src/main/gen")
