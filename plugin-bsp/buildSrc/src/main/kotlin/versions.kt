

object Platform {
  const val version = "242.18071.24"

  // Plugin Dependencies -> https://plugins.jetbrains.com/docs/intellij/plugin-dependencies.html
  // Example: platformPlugins =" com.intellij.java, com.jetbrains.php:203.4449.22"
  val plugins =
    listOf("PythonCore:$version", "org.jetbrains.android:$version")
  val bundledPlugins = listOf("com.intellij.java", "org.jetbrains.kotlin", "com.jetbrains.performancePlugin")

}

const val javaVersion = "17"
const val kotlinVersion = "1.9"
