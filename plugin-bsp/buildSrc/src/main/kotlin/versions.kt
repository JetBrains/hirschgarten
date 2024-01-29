// IntelliJ Platform Artifacts Repositories
// -> https://plugins.jetbrains.com/docs/intellij/intellij-artifacts.html
object Plugin {
  const val group = "org.jetbrains"
  const val name = "intellij-bsp"
  const val version = "2023.3.0-EAP"

// See https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
// for insight into build numbers and IntelliJ Platform versions.
  const val sinceBuild = "233.11799.6"
  const val untilBuild = "233.*"
}

// Plugin Verifier integration -> https://github.com/JetBrains/gradle-intellij-plugin//plugin-verifier-dsl
// See https://jb.gg/intellij-platform-builds-list for available build versions.
const val pluginVerifierIdeVersions = "233.11799.241"

object Platform {
  const val type = "IC"
  const val version = "233.11799.241"
  const val downloadSources = true

  // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
  // Example: platformPlugins =" com.intellij.java, com.jetbrains.php:203.4449.22"
  val plugins = listOf("com.intellij.java", "org.jetbrains.kotlin", "PythonCore:233.11799.241", "org.jetbrains.android:233.11799.272")
}

const val javaVersion = "17"
const val kotlinVersion = "1.9"