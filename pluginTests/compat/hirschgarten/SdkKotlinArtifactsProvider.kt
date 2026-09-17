package org.jetbrains.bazel.test.compat

import com.intellij.openapi.application.PathManager
import org.jetbrains.kotlin.idea.testFramework.TestKotlinArtifactsProvider
import java.nio.file.Path
import kotlin.io.path.isDirectory

/**
 * The `.idea` dir the test runner creates makes the Kotlin plugin think it runs from sources, so it asks for the
 * compiler instead of using the bundled one. The SDK ships it in `plugins/Kotlin/kotlinc/`.
 */
internal class SdkKotlinArtifactsProvider : TestKotlinArtifactsProvider {
  // plugins/Kotlin/lib/kotlin-plugin.jar -> plugins/Kotlin
  private val pluginDir: Path by lazy {
    val jar = PathManager.getJarForClass(TestKotlinArtifactsProvider::class.java)
              ?: error("No jar for ${TestKotlinArtifactsProvider::class.java.name}")
    jar.parent.parent.also { check(it.resolve("kotlinc").isDirectory()) { "No kotlinc/ in $it" } }
  }

  override fun getKotlincCompilerCli(): Path = pluginDir.resolve("kotlinc")

  // lib/jps/ is not on the test classpath or in the runfiles, and Bazel tests never start a JPS build.
  override fun getJpsPluginClasspath(): List<Path> =
    throw UnsupportedOperationException("Kotlin JPS plugin is not available in Bazel plugin tests")
}
