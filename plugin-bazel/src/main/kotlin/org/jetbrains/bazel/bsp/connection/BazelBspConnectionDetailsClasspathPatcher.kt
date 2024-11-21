package org.jetbrains.bazel.bsp.connection

import ch.epfl.scala.bsp4j.BspConnectionDetails
import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import org.jetbrains.bazel.settings.BAZEL_PLUGIN_ID
import org.jetbrains.bazel.settings.BSP_PLUGIN_ID
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

internal const val BAZEL_BSP_CONNECTION_FILE_ARGV_CLASSPATH_INDEX = 2

private const val UTIL_8_JAR_NAME = "util-8.jar"

/**
 * We need to update bazel-bsp connection details classpath:
 * the "default" (inside bazel-bsp) mechanism just takes a process classpath,
 * what works nicely for the installer in bazel-bsp since it contains all the things we need.
 * Unfortunately, when we do that here (in intellij-bazel) it doesn't work:
 * the classpath of this process is completely different, so we need to fix it!
 *
 * How we fix it:
 * - we use intellij-bsp lib jars - it contains bsp library and its dependencies
 * - we use intellij-bazel lib jars - it contains bazel-bsp jar and its dependencies minus bsp itself
 * - `util-8.jar` from the current classpath - it's a jar from the platform which contains kotlinx - we need it in the classpath
 *
 * IMPORTANT NOTE: we cannot use the whole classpath of the current process because it interferes
 * with log4j in bazel-bsp and the server doesn't produce log file.
 */
internal fun BspConnectionDetails.updateClasspath() {
  argv[BAZEL_BSP_CONNECTION_FILE_ARGV_CLASSPATH_INDEX] = calculateNewClasspath()
}

private fun BspConnectionDetails.calculateNewClasspath(): String {
  val bazelPluginClasspath = calculatePluginClasspath(BAZEL_PLUGIN_ID)
  val bspPluginClasspath = calculatePluginClasspath(BSP_PLUGIN_ID)
  val util8Jar = getUtil8Jar()

  return listOfNotNull(bazelPluginClasspath, bspPluginClasspath, util8Jar).joinToString(File.pathSeparator)
}

private fun calculatePluginClasspath(pluginIdString: String): String? {
  val pluginId = PluginId.findId(pluginIdString) ?: return null
  val pluginDescriptor = PluginManager.getInstance().findEnabledPlugin(pluginId) ?: return null
  val pluginPath = pluginDescriptor.pluginPath

  if (pluginPath.isJarFile()) return pluginPath.toString()

  val pluginJarsDir = pluginPath.resolve("lib")

  if (!pluginJarsDir.isDirectory()) return null

  val jarsList =
    Files
      .list(pluginJarsDir)
      .filter { it.isJarFile() }
      .map { it.toAbsolutePath().toString() }
      .toList()

  return jarsList.joinToString(separator = File.pathSeparator)
}

private fun Path.isJarFile() = isRegularFile() && name.endsWith(".jar")

private fun BspConnectionDetails.getUtil8Jar(): String =
  argv[BAZEL_BSP_CONNECTION_FILE_ARGV_CLASSPATH_INDEX]
    .split(File.pathSeparator)
    .first { it.endsWith(UTIL_8_JAR_NAME) }
