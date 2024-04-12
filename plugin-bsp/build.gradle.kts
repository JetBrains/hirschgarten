import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.Constants
import org.jetbrains.intellij.platform.gradle.extensions.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask

plugins {
  // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
  alias(libs.plugins.intellij)
  // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
  alias(libs.plugins.changelog)
  id("java-test-fixtures")
  id("intellijbsp.kotlin-conventions")
}

val myToken: String by project
val releaseChannel: String by project

group = Plugin.group
version = Plugin.version

dependencies {
  implementation(project(":jps-compilation"))
  implementation(project(":protocol"))
  implementation(project(":workspacemodel"))
  implementation(libs.bsp4j) {
    exclude(group = "com.google.guava", "guava")
  }
  implementation(libs.gson)
  testImplementation(libs.junitJupiter)
  testImplementation(libs.kotest)
  testFixturesImplementation(libs.junitJupiter)
  testFixturesImplementation(libs.kotest)

  intellijPlatform {
    intellijIdeaCommunity(Platform.version)
    plugins(Platform.plugins)
    bundledPlugins(Platform.bundledPlugins)

    instrumentationTools()
    testFramework(TestFrameworkType.Plugin.Java)
    testFramework(TestFrameworkType.Platform.JUnit5)
  }
}

tasks.runIde {
  jvmArgs("-Didea.log.trace.categories=" +
    "#org.jetbrains.plugins.bsp," +
    "#org.jetbrains.magicmetamodel.impl.PerformanceLogger")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellijPlatform {
  pluginConfiguration {
    name = Plugin.name
    ideaVersion {
      sinceBuild = Plugin.sinceBuild
      untilBuild = Plugin.untilBuild
    }

    // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
    description = File(projectDir, "README.md").readText().lines().run {
      val start = "<!-- Plugin description -->"
      val end = "<!-- Plugin description end -->"

      if (!containsAll(listOf(start, end))) {
        throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
      }
      subList(indexOf(start) + 1, indexOf(end))
    }.joinToString("\n").run { markdownToHTML(this) }

    // Get the latest available change notes from the changelog file
    changeNotes = provider { changelog.renderItem(changelog.getLatest(), Changelog.OutputType.HTML) }
  }

  publishing {
    token = provider { myToken }
    // pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
    // Specify pre-release label to publish the plugin in a custom Release Channel. Read more:
    // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
    // Release channel is set via command-line param "releaseChannel"
    // Marketplace token is set via command-line parm "myToken"
    // Example command "./gradlew publishPlugin -PmyToken="perm:YOUR_TOKEN -PreleaseChannel=nightly"
    channels = provider { listOf(releaseChannel) }
  }

  verifyPlugin {
    ides {
      recommended()
    }
    failureLevel = setOf(
      VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
      VerifyPluginTask.FailureLevel.NOT_DYNAMIC
    )
  }
}

configurations {
  getByName(JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME) {
    extendsFrom(getByName(Constants.Configurations.INTELLIJ_PLATFORM_TEST_DEPENDENCIES))
  }
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

repositories {
  mavenCentral()
  intellijPlatform {
    defaultRepositories()
  }
}

tasks {
  test {
    classpath -= classpath.filter { it.name.contains("kotlin-compiler-embeddable") }
  }
}
