package org.jetbrains.bazel.matcher

import io.kotest.matchers.Matcher
import io.kotest.matchers.collections.containAll
import io.kotest.matchers.should
import org.jetbrains.bazel.clion.sync.ArtifactLocation

/**
 * An [ArtifactLocation] without its [ArtifactLocation.resolvedPath], which
 * points into the temporary directory the test project was copied to and
 * thus cannot be compared against a constant.
 *
 * The defaults describe a source file of the main workspace, which is what
 * test projects mostly consist of.
 */
internal data class ExpectedArtifactLocation(
  val relativePath: String,
  val rootPath: String = "",
  val isSource: Boolean = true,
  val isExternal: Boolean = false,
) {

  companion object {

    fun fromArtifactLocation(location: ArtifactLocation): ExpectedArtifactLocation {
      return ExpectedArtifactLocation(location.relativePath, location.rootPath, location.isSource, location.isExternal)
    }
  }
}

/**
 * Matches a collection of artifact locations, where root paths in bazel-bin
 * (e.g. bazel-out/k8-fastbuild/bin) can be matched against the symlinked
 * named.
 */
private fun containArtifacts(expected: Collection<ExpectedArtifactLocation>): Matcher<Collection<ArtifactLocation>> {
  return containAll(expected.map { it.resolveBazelBin() }).contramap { locations ->
    locations.map { ExpectedArtifactLocation.fromArtifactLocation(it).resolveBazelBin() }
  }
}

internal fun Collection<ArtifactLocation>.shouldContainArtifacts(expected: Collection<ExpectedArtifactLocation>) {
  should(containArtifacts(expected))
}

internal fun Collection<ArtifactLocation>.shouldContainArtifacts(vararg relativePaths: String) {
  shouldContainArtifacts(relativePaths.map(::ExpectedArtifactLocation))
}

internal fun Collection<ArtifactLocation>.shouldContainArtifact(
  relativePath: String,
  rootPath: String = "",
  isSource: Boolean = true,
  isExternal: Boolean = false,
) {
  shouldContainArtifacts(listOf(ExpectedArtifactLocation(relativePath, rootPath, isSource, isExternal)))
}

private fun ExpectedArtifactLocation.resolveBazelBin(): ExpectedArtifactLocation {
  return copy(rootPath = rootPath.resolveBazelBin())
}
