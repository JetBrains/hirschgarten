package org.jetbrains.bsp.protocol

import com.google.devtools.intellij.aspect.Common
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import org.jetbrains.bsp.protocol.OutputLocation.External
import org.jetbrains.bsp.protocol.OutputLocation.Host
import org.jetbrains.bsp.protocol.OutputLocation.Output
import org.jetbrains.bsp.protocol.OutputLocation.Workspace

internal object OutputLocationParser {
  // marker Bazel emits for a path relative to the execution root
  private const val PROC_SELF_CWD = "/proc/self/cwd"
  private const val BAZEL_OUT = "bazel-out"
  private const val EXTERNAL = "external"
  private const val EXTERNAL_PARENT = ".."

  /**
   * Parses a raw path that Bazel spells in the execroot coordinates,
   * for example a toolchain path or an include directory.
   *
   * A workspace convenience symlink name, for example `bazel-bin`, maps to [Workspace].
   * The workspace root then resolves it through the symlink.
   * An empty input maps to `Workspace("")`.
   */
  fun parseExecrootPath(raw: String): OutputLocation {
    // strip bazel /proc/self/cwd marker
    val path = when {
      raw == PROC_SELF_CWD -> ""
      raw.startsWith("$PROC_SELF_CWD/") -> raw.removePrefix("$PROC_SELF_CWD/")
      else -> raw
    }
    if (OSAgnosticPathUtil.isAbsolute(path)) {
      return Host(path)
    }

    val segments = path.split('/').filter { it.isNotEmpty() }
    val first = segments.firstOrNull()
    return when {
      // build artifact, it accumulates under the execution root, independent of the symlink settings
      first == BAZEL_OUT -> outputLocation(afterBazelOut = segments.drop(1))

      // file in an external workspace. `external/<repo>/...` is the classic layout and
      // `../<repo>/...` is the sibling repository layout.
      (first == EXTERNAL || first == EXTERNAL_PARENT) && segments.size >= 2 ->
        External(
          repoName = segments[1],
          relativePath = segments.drop(2).joinToString("/"),
          siblingLayout = first == EXTERNAL_PARENT,
        )

      // else, in the main workspace, execution root mirrors the workspace at the top level
      else -> Workspace(relativePath = segments.joinToString("/"))
    }
  }

  /**
   * Parses an aspect [Common.ArtifactLocation].
   *
   * The aspect has two constructors with different guarantees, see `common/artifact_location.bzl`:
   * - `from_file` derives the fields from a `File`. The root path of a generated file
   *   starts with `bazel-out` and the flags are reliable.
   * - `from_execpath` wraps a raw string, for example `java_home`. It always sets
   *   `is_source = false`, and the relative path can be absolute, for example a `local_jdk` home.
   *
   * So the root path decides [Output], the `is_external` flag decides [External],
   * and an `from_execpath` leftover is an execroot spelling for [parseExecrootPath].
   */
  fun parse(location: Common.ArtifactLocation): OutputLocation {
    val rootSegments = location.rootPath.split('/').filter { it.isNotEmpty() }
    return when {
      // generated file, likely from `bazel-out/k8-opt/bin` or similar path
      rootSegments.firstOrNull() == BAZEL_OUT ->
        outputLocation(afterBazelOut = rootSegments.drop(1), relativePath = location.relativePath)

      // external repo file
      location.isExternal -> parseExternalSource(rootSegments, location)

      // TODO: delete after `Common.ArtifactLocation` cannot return execroot path
      // `from_execroot` result
      !location.isSource -> parseExecrootPath(location.relativePath)

      // source file from main repo
      else -> Workspace(location.relativePath)
    }
  }

  /**
   * Extracts the canonical repository name from external location.
   *
   * The name embedded in a path is already canonical, so no repo mapping is needed.
   * Every aspect constructor emits an external root of exactly two segments:
   * `external/<repo>` or, in the sibling repository layout, `../<repo>`.
   */
  private fun parseExternalSource(rootSegments: List<String>, location: Common.ArtifactLocation): OutputLocation =
    if (rootSegments.size == 2 && (rootSegments[0] == EXTERNAL_PARENT || rootSegments[0] == EXTERNAL)) {
      External(
        repoName = rootSegments[1],
        relativePath = location.relativePath,
        siblingLayout = rootSegments[0] == EXTERNAL_PARENT,
      )
    }
    else {
      Workspace(relativePath = location.relativePath)
    }

  private fun outputLocation(afterBazelOut: List<String>, relativePath: String = ""): Output {
    // take e.g., `k8-opt/bin`
    val rootSegments = afterBazelOut.take(2)
    // remaining is leftover
    val leftover = afterBazelOut.drop(2).joinToString("/")
    return Output(OutputRoot.of(rootSegments), joinNonEmpty(leftover, relativePath))
  }
}
