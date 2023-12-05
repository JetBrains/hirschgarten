import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.tasks.BuildSearchableOptionsTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel
import org.jetbrains.intellij.tasks.VerifyPluginTask

plugins {
  // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
  alias(libs.plugins.intellij)
  // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
  alias(libs.plugins.changelog)

  id("intellijbsp.kotlin-conventions")
}

val myToken: String by project
val releaseChannel: String by project

group = Plugin.group
version = Plugin.version

dependencies {
  implementation(project(":magicmetamodel"))
  implementation(project(":protocol"))
  implementation(project(":workspacemodel"))
  testImplementation(project(":test-utils"))
  implementation(libs.bsp4j)
  implementation(libs.gson)
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

allprojects {
  tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    // disable the malfunctioned platform listener com.intellij.tests.JUnit5TestSessionListener
    // this listener caused the CI tests to fail with
    // AlreadyDisposedException: Already disposed: Application (containerState DISPOSE_COMPLETED)
    // TODO: follow up https://youtrack.jetbrains.com/issue/IDEA-337508/AlreadyDisposedException-Already-disposed-Application-containerState-DISPOSECOMPLETED-after-junit5-tests-on-TeamCity
    systemProperty("intellij.build.test.ignoreFirstAndLastTests", "true")
  }
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
}

tasks {
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
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}