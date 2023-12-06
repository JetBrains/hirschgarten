import org.jetbrains.intellij.pluginRepository.PluginRepositoryFactory

plugins {
  scala
  id("intellijbazel.kotlin-conventions")
}

dependencies {
  implementation(libs.scala)
  implementation(libs.ideProbeDriver)
  implementation(libs.ideProbeRobot)
}

repositories {
  mavenCentral()
  maven("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies")
}

tasks.processResources {
  filesMatching("*.conf") {
    expand(
      ("plugin_version" to Plugin.version),
      ("platform_version" to Platform.version),
      ("bsp_id") to findLatestCompatibleBspPluginVersion(),
    )
  }
}

fun findLatestCompatibleBspPluginVersion(): String {
  val pluginId = "org.jetbrains.bsp"
  val buildVersion = "${Platform.type}-${Platform.version}"
  return findPluginRepositoryId(pluginId, buildVersion, "nightly")
    ?: error("Couldn't find compatible BSP plugin version")
}


fun findPluginRepositoryId(id: String, buildVersion: String, channel: String): String? =
  PluginRepositoryFactory.create()
    .pluginManager
    .searchCompatibleUpdates(listOf(id), buildVersion, channel)
    .firstOrNull()
    ?.id?.toString()