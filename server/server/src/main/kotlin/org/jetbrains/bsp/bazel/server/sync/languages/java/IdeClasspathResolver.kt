package org.jetbrains.bsp.bazel.server.sync.languages.java

import org.jetbrains.bazel.label.Label
import java.net.URI

class IdeClasspathResolver(
  private val label: Label,
  runtimeClasspath: Sequence<URI>,
  compileClasspath: Sequence<URI>,
) {
  private val runtimeJars: Set<String> = runtimeClasspath.map { it.toString() }.toSet()
  private val runtimeMavenJarSuffixes: Set<String> = runtimeJars.mapNotNull(::toMavenSuffix).toSet()
  private val compileJars: Sequence<String> = compileClasspath.map { it.toString() }

  fun resolve(): Sequence<URI> =
    compileJars
      .map(::findRuntimeEquivalent)
      .filterNot { isItJarOfTheCurrentTarget(it) }
      .map(URI::create)

  private fun findRuntimeEquivalent(compileJar: String): String {
    val runtimeJar = compileJar.replace(JAR_PATTERN, ".jar")
    if (runtimeJars.contains(runtimeJar)) {
      return runtimeJar
    }
    val headerSuffix = toMavenSuffix(compileJar)
    val mavenJarSuffix =
      headerSuffix?.let { s: String ->
        s.replace(
          "/header_([^/]+)\\.jar$".toRegex(),
          "/$1.jar",
        )
      }
    return mavenJarSuffix?.takeIf(runtimeMavenJarSuffixes::contains)?.let { suffix ->
      runtimeJars.find { jar: String -> jar.endsWith(suffix) }
    } ?: compileJar
  }

  private fun toMavenSuffix(uri: String): String? {
    listOf("/maven/", "/maven2/").forEach { indicator ->
      val index = uri.lastIndexOf(indicator)
      if (index >= 0) {
        return uri.substring(index + indicator.length)
      }
    }
    return null
  }

  private fun isItJarOfTheCurrentTarget(jar: String): Boolean {
    val targetPath = label.packagePath
    val targetJar = "$targetPath/${label.target}.jar"

    return jar.endsWith(targetJar)
  }

  companion object {
    private val JAR_PATTERN = ("((-[hi]jar)|(\\.abi))\\.jar\$").toRegex()

    fun resolveIdeClasspath(
      label: Label,
      runtimeClasspath: List<URI>,
      compileClasspath: List<URI>,
    ) = IdeClasspathResolver(
      label,
      runtimeClasspath.asSequence(),
      compileClasspath.asSequence(),
    ).resolve().toList()
  }
}
