package org.jetbrains.bazel.scala.sdk

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo.TargetIdeInfo
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import java.nio.file.Path
import java.util.regex.Pattern

internal class ScalaSdkResolver(private val bazelPathsResolver: BazelPathsResolver) {
  fun resolveSdk(targetInfo: TargetIdeInfo, localRepositories : LocalRepositoryMapping): ScalaSdk? {
    if (!targetInfo.hasScalaTargetInfo()) {
      return null
    }
    val scalaTarget = targetInfo.scalaTargetInfo
    val compilerJars = bazelPathsResolver.resolvePaths(scalaTarget.compilerClasspathList, localRepositories).sorted()
    val maybeVersions = compilerJars.mapNotNull(::extractVersion)
    if (maybeVersions.none()) {
      return null
    }
    val version = maybeVersions.distinct().maxOf { it }
    return ScalaSdk(
      name = "",
      scalaVersion = version,
      sdkJars = compilerJars.map(bazelPathsResolver::resolve).map { it.toUri() },
    )
  }

  private fun extractVersion(path: Path): String? {
    val name = path.fileName.toString()
    val matcher = VERSION_PATTERN.matcher(name)
    return if (matcher.matches()) matcher.group(1) else null
  }

  companion object {
    // The version may carry a pre-release/build suffix (3.10.0-RC2, 3.8.0-RC1-bin-20250901-abc-NIGHTLY);
    // -sources/-javadoc classifier jars are not treated as versions.
    private val VERSION_PATTERN =
      Pattern.compile(
        "(?:processed_)?scala3?-(?:library|compiler|reflect)(?:_3)?-" +
          "(\\d[.\\d]*(?:-[0-9A-Za-z.-]+)?)(?<!-sources)(?<!-javadoc)\\.jar",
      )
  }
}
