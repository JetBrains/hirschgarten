import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.*
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
  // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
  alias(libs.plugins.intellij)
  // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
  alias(libs.plugins.changelog)
  alias(libs.plugins.composeDesktop)
  alias(libs.plugins.shadow)
  id("intellijbsp.kotlin-conventions")
}

val myToken: String by project
val releaseChannel: String by project

group = Plugin.group
version = Plugin.version

dependencies {
  implementation(project(":magicmetamodel"))
  testImplementation(project(":test-utils"))
  implementation(libs.bsp4j)
  implementation(libs.gson)
  implementation(libs.coursier)
  implementation(compose.desktop.linux_arm64)
  implementation(compose.desktop.linux_x64)
  implementation(compose.desktop.macos_arm64)
  implementation(compose.desktop.macos_x64)
  implementation(compose.desktop.windows_x64)
  implementation(libs.jewelIdeLafBridge)

  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
}

tasks.runIde{
  jvmArgs("-Didea.log.trace.categories=" +
    "#org.jetbrains.plugins.bsp," +
    "#org.jetbrains.magicmetamodel.impl.PerformanceLogger")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
  pluginName.set(Plugin.name)
  version.set(Platform.version)
  type.set(Platform.type)
  downloadSources.set(Platform.downloadSources)
  updateSinceUntilBuild.set(true)
  plugins.set(Platform.plugins)
}

// Configure gradle-changelog-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
  version.set(Plugin.version)
  groups.set(emptyList())
}

subprojects {
  apply(plugin = "org.jetbrains.intellij")

  intellij {
    version.set(Platform.version)
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
repositories {
  mavenCentral()
  maven("https://packages.jetbrains.team/maven/p/kpm/public/")
  maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

tasks {
    shadowJar {
      archiveClassifier = null
      relocate("kotlinx.serialization", "shadow.kotlinx.serialization")
      relocate("io.ktor", "shadow.io.ktor")
      relocate("kotlinx.datetime", "shadow.kotlinx.datetime")
      val exclusions = listOf("kotlin-stdlib", "slf4j", "kotlin-reflect", "kotlinx-coroutines", "lockback")
      exclude { element ->
        exclusions.any { it in element.name }
      }

    }
  patchPluginXml {
    version.set(Plugin.version)
    sinceBuild.set(Plugin.sinceBuild)
    untilBuild.set(Plugin.untilBuild)

    // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
    pluginDescription.set(
      File(projectDir, "README.md").readText().lines().run {
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

  runPluginVerifier {
    ideVersions.set(pluginVerifierIdeVersions.split(',').map(String::trim).filter(String::isNotEmpty))
    failureLevel.set(setOf(
      FailureLevel.COMPATIBILITY_PROBLEMS,
      FailureLevel.NOT_DYNAMIC
    ))
  }

  prepareSandbox {
    pluginJar = shadowJar.flatMap { it.archiveFile }
    runtimeClasspathFiles = objects.fileCollection()
  }

  val buildShadowPlugin by registering(Zip::class) {
    from(shadowJar) {
      into("org.jetbrains.bsp/lib")
    }
    from(jarSearchableOptions) {
      into("org.jetbrains.bsp/lib")
    }
    archiveBaseName = project.name + "-shadow"
    destinationDirectory = layout.buildDirectory.dir("distributions")
  }

  val publishShadowPlugin by registering(PublishPluginTask::class) {
    group = "publishing"
    distributionFile = buildShadowPlugin.flatMap { it.archiveFile }
    dependsOn("patchChangelog")
    token.set(provider { myToken })
    // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    // Release channel is set via command-line param "releaseChannel"
    // Marketplace token is set via command-line parm "myToken"
    // Example command "./gradlew publishPlugin -PmyToken="perm:YOUR_TOKEN -PreleaseChannel=nightly"
    channels.set(provider { listOf(releaseChannel) })
  }


  publishPlugin {
    dependsOn("patchChangelog")
    token.set(provider { myToken })
    // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    // Release channel is set via command-line param "releaseChannel"
    // Marketplace token is set via command-line parm "myToken"
    // Example command "./gradlew publishPlugin -PmyToken="perm:YOUR_TOKEN -PreleaseChannel=nightly"
    channels.set(provider { listOf(releaseChannel) })
  }
}

tasks {
  test {
    if (project.findProperty("exclude.integration.test") == "true") {
      filter {
        excludeTest(
          "org.jetbrains.plugins.bsp.integrationtest.NonOverlappingTest",
          "Compute non overlapping targets for bazelbuild_bazel project"
        )
      }
    }
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}