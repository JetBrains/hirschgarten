rootProject.name = "intellij-bsp"
include("test-utils", "magicmetamodel")

pluginManagement {
  repositories {
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    gradlePluginPortal()
  }
}