package org.jetbrains.bazel.junit

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.junit.JUnitConfiguration
import org.jetbrains.bazel.config.isBazelProject
import java.nio.file.Paths

class JUnitRunConfigurationExtension : RunConfigurationExtension() {
  override fun <T : RunConfigurationBase<*>?> updateJavaParameters(
    configuration: T & Any,
    params: JavaParameters,
    runnerSettings: RunnerSettings?,
  ) {
    val junitConfiguration = configuration as? JUnitConfiguration ?: return
    val project = junitConfiguration.project
    if (!project.isBazelProject) return
    val filteredClasspaths = params.classPath.pathList.filterNot { extractFileNameFromClassPath(it)?.let(::isInterfaceJar) ?: false }
    params.apply {
      classPath.clear()
      classPath.addAll(filteredClasspaths)
    }
  }

  /**
   * a heuristic to know if a Bazel-provided jar is an interface jar
   */
  private fun isInterfaceJar(fileName: String): Boolean = fileName.endsWith("-ijar.jar") || fileName.endsWith("-hjar.jar")

  private fun extractFileNameFromClassPath(classPath: String): String? = Paths.get(classPath)?.fileName?.toString()

  override fun isApplicableFor(configuration: RunConfigurationBase<*>): Boolean = configuration is JUnitConfiguration
}
