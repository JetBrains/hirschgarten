rootProject.name = "intellij-bsp"
include("protocol", "workspacemodel", "jps-compilation", "performance-testing")

pluginManagement {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
  }
}
