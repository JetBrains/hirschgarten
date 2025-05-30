package org.jetbrains.bazel.server.sync.languages.scala

import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.server.paths.BazelPathsResolver
import java.nio.file.Path
import java.util.regex.Pattern

class ScalaSdkResolver(private val bazelPathsResolver: BazelPathsResolver) {
  fun resolveSdk(targetInfo: BspTargetInfo.TargetInfo): ScalaSdk? {
    if (!targetInfo.hasScalaTargetInfo()) {
      return null
    }
    val scalaTarget = targetInfo.scalaTargetInfo
    val compilerJars =
      bazelPathsResolver.resolvePaths(scalaTarget.compilerClasspathList).sorted()
    val maybeVersions = compilerJars.mapNotNull(::extractVersion)
    if (maybeVersions.none()) {
      return null
    }
    val version = maybeVersions.distinct().maxOf { it }
    return ScalaSdk(
      version,
      compilerJars.map(bazelPathsResolver::resolve),
    )
  }

  private fun extractVersion(path: Path): String? {
    val name = path.fileName.toString()
    val matcher = VERSION_PATTERN.matcher(name)
    return if (matcher.matches()) matcher.group(1) else null
  }

  companion object {
    private val VERSION_PATTERN =
      Pattern.compile("(?:processed_)?scala3?-(?:library|compiler|reflect)(?:_3)?-([.\\d]+)\\.jar")
  }
}
