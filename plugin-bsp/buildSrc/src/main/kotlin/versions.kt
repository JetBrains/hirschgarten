// IntelliJ Platform Artifacts Repositories
// -> https://plugins.jetbrains.com/docs/intellij/intellij-artifacts.html
object Plugin {
  const val group = "org.jetbrains"
  const val name = "intellij-bsp"
  const val version = "2024.1.0-EAP"

  // See https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
// for insight into build numbers and IntelliJ Platform versions.
  const val sinceBuild = "241.11761.10"
  const val untilBuild = "241.11761.*"
}

// Plugin Verifier integration -> https://github.com/JetBrains/gradle-intellij-plugin//plugin-verifier-dsl
// See https://jb.gg/intellij-platform-builds-list for available build versions.
const val pluginVerifierIdeVersions = "241.11761.10"

object Platform {
  const val type = "IU"
  const val version = "241.11761.10-EAP-SNAPSHOT"
  const val downloadSources = true

  // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
  // Example: platformPlugins =" com.intellij.java, com.jetbrains.php:203.4449.22"
  val plugins =
    listOf("com.intellij.java", "org.jetbrains.kotlin", "PythonCore:241.11761.10", "org.jetbrains.android:241.11761.10",
      "org.jetbrains.plugins.go:241.11761.10")
}

const val javaVersion = "17"
const val kotlinVersion = "1.9"