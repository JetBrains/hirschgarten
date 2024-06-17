// IntelliJ Platform Artifacts Repositories
// -> https://plugins.jetbrains.com/docs/intellij/intellij-artifacts.html
object Plugin {
  const val group = "org.jetbrains"
  const val name = "Build Server Protocol (BSP)"
  const val version = "2024.1.0-EAP"

  // See https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
  // for insight into build numbers and IntelliJ Platform versions.
  const val sinceBuild = "241.15989.21"
  const val untilBuild = "241.*"
}

object Platform {
  const val version = "241.15989.150"

  // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
  // Example: platformPlugins =" com.intellij.java, com.jetbrains.php:203.4449.22"
  val plugins =
    listOf("PythonCore:241.15989.150", "org.jetbrains.android:241.15989.150")
  val bundledPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin", "com.jetbrains.performancePlugin")

}

const val javaVersion = "17"
const val kotlinVersion = "1.9"
