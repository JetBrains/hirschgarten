package org.jetbrains.bazel.sync.workspace

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.OutputLocationResolver
import org.jetbrains.bsp.protocol.isGenerated
import org.jetbrains.bsp.protocol.toExecrootPath
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path

@ApiStatus.Internal
class DefaultOutputLocationResolver(private val bazelInfo: BazelInfo) : OutputLocationResolver {

  override fun resolve(
    location: OutputLocation,
    localOverride: LocalRepositoryMapping?,
  ): Path? = try {
    when (location) {
      is OutputLocation.Host -> Path(location.absolutePath)
      is OutputLocation.Workspace -> bazelInfo.workspaceRoot.resolve(location.relativePath)
      is OutputLocation.Output -> bazelInfo.execRoot.resolve(location.toExecrootPath())
      is OutputLocation.External -> resolveExternal(location, localOverride)
    }
  }
  catch (_: InvalidPathException) {
    null
  }

  private fun resolveExternal(location: OutputLocation.External, localOverride: LocalRepositoryMapping?): Path {
    if (location.isGenerated) {
      return bazelInfo.execRoot.resolve(location.toExecrootPath()).normalize()
    }
    val overriddenRoot = localOverride?.localRepositories?.get(location.repoName)
    if (overriddenRoot != null) {
      return bazelInfo.workspaceRoot.resolve(overriddenRoot).resolve(location.relativePath)
    }
    // a source file lives in the output base in both layouts
    return bazelInfo.outputBase.resolve("external")
      .resolve(location.repoName)
      .resolve(location.relativePath)
  }
}
