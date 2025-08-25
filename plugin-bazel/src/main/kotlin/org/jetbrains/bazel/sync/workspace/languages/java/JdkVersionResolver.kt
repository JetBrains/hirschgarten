package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.bazelrunner.outputs.SpawnedProcess
import org.jetbrains.bazel.bazelrunner.outputs.spawnProcessBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class JdkVersionResolver {
  private val versions = ConcurrentHashMap<Path, Int?>()

  fun resolve(path: Path): Int? = versions.computeIfAbsent(path.toRealPath()) { resolveJavaVersion(it) }

  private fun resolveJavaVersion(path: Path): Int? = readFromReleaseFile(path) ?: readByRunningJavaBinary(path)

  private fun readFromReleaseFile(path: Path): Int? {
    val releasePath = path.resolve("release")
    if (Files.notExists(releasePath)) return null

    val text = Files.readString(releasePath)
    return parseVersion(text, releaseFileVersionPattern)
  }

  private fun readByRunningJavaBinary(path: Path): Int? {
    val javaPath = path.resolve("bin").resolve("java")
    if (Files.notExists(javaPath)) return null

    return firstLineOfJavaVersionOutput(javaPath)
      ?.let { parseVersion(it, quotedVersionPattern) }
  }

  private fun firstLineOfJavaVersionOutput(javaPath: Path): String? {
    val processSpawner = ProcessSpawner.getInstance()
    val process =

      processSpawner
        .spawnProcessBlocking(
          command = listOf(javaPath.toString(), "-version"),
          environment = emptyMap(),
          redirectErrorStream = true,
          workDirectory = null,
        )

    val result = process.waitFor()

    return if (result == 0) readLines(process).firstOrNull() else null
  }

  private fun readLines(process: SpawnedProcess) =
    process.inputStream
      .bufferedReader()
      .use { it.readText() }
      .lines()

  private val quotedVersionPattern = """"(\d+(?:.\d+)*)"""".toRegex()
  private val releaseFileVersionPattern = "JAVA_VERSION=${quotedVersionPattern.pattern}".toRegex()

  private fun parseVersion(text: String, regex: Regex): Int? =
    regex.find(text)?.let { match ->
      val version = match.groupValues[1].removePrefix("1.")
      return version.takeWhile { it != '.' }.toIntOrNull()
    }
}
