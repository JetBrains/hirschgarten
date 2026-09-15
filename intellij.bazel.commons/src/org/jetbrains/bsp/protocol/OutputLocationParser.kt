package org.jetbrains.bsp.protocol

import com.google.devtools.intellij.aspect.Common
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bsp.protocol.OutputLocation.External
import org.jetbrains.bsp.protocol.OutputLocation.Output
import org.jetbrains.bsp.protocol.OutputLocation.Workspace
import kotlin.io.path.Path

@ApiStatus.Internal
class OutputLocationParser(
  private val bazelPathsResolver: BazelPathsResolver,
  private val hardLinks: BazelOutFileHardLinks,
) {
  /**
   * Parses a raw path that Bazel spells in the execroot coordinates,
   * for example a toolchain path or an include directory.
   *
   * A workspace convenience symlink name, for example `bazel-bin`, maps to [Workspace].
   * The workspace root then resolves it through the symlink.
   * An empty input maps to `Workspace("")`.
   */
  suspend fun parseExecrootPath(raw: List<String>): List<OutputLocation> = raw
    .map { OutputLocationParserWithoutHardlink.parseExecrootPath(it) }
    .also { hardLinkOutputs(it) }

  /**
   * Prefer to use the override that takes a `List` for parallel IO during hardlink creation
   */
  suspend fun parseExecrootPath(raw: String): OutputLocation = parseExecrootPath(listOf(raw)).single()

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
  suspend fun parse(locations: List<Common.ArtifactLocation>): List<OutputLocation> {
    val parsed = locations.map { OutputLocationParserWithoutHardlink.parse(it) }
    hardLinkOutputs(parsed)
    return parsed
  }

  private suspend fun hardLinkOutputs(parsed: List<OutputLocation>) {
    val outputArtifacts = parsed.filterIsInstance<Output>().map { output ->
      bazelPathsResolver.relativePathToExecRootAbsolute(Path(output.toExecrootPath()))
    }
    hardLinks.createOutputFileHardLinks(outputArtifacts)
  }

  /**
   * Prefer to use the override that takes a `List` for parallel IO during hardlink creation
   */
  suspend fun parse(location: Common.ArtifactLocation): OutputLocation = parse(listOf(location)).single()
}
