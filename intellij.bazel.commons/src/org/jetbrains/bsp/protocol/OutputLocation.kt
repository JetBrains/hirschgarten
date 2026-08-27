package org.jetbrains.bsp.protocol

import com.google.devtools.intellij.aspect.Common
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import java.nio.file.Path

/**
 * Represent location root under `bazel-out`, e.g. "k8-fastbuild/bin"
 */
@ConsistentCopyVisibility
@ApiStatus.Internal
data class OutputRoot private constructor(val segments: List<String>) {
  val path: String = segments.joinToString("/")

  companion object {
    private val interner = Interner.createWeakInterner<OutputRoot>()

    fun of(segments: List<String>): OutputRoot = interner.intern(OutputRoot(segments))
  }
}

@ApiStatus.Internal
sealed interface OutputLocation {
  // main workspace relative path
  data class Workspace(val relativePath: String) : OutputLocation

  // example: bazel-out/darwin_arm64-fastbuild/a/b/h.java
  // root -> bazel-out/darwin_arm64-fastbuild/
  // relativePath -> a/b/h.java
  data class Output(val root: OutputRoot, val relativePath: String) : OutputLocation

  // external repository relative path
  // repoName is canonical
  // siblingLayout marks the `../<repo>` form of `--experimental_sibling_repository_layout`
  // the default is the `external/<repo>` form
  data class External(val repoName: String, val relativePath: String, val siblingLayout: Boolean = false) : OutputLocation

  // host path, e.g. /usr/bin/clang
  data class Host(val absolutePath: String) : OutputLocation

  companion object {
    fun parseExecrootPath(raw: String): OutputLocation = OutputLocationParser.parseExecrootPath(raw)

    fun parse(location: Common.ArtifactLocation): OutputLocation = OutputLocationParser.parse(location)
  }
}

@get:ApiStatus.Internal
val OutputLocation.isGenerated: Boolean
  get() = when (this) {
    is OutputLocation.Output -> true
    is OutputLocation.External -> relativePath.startsWith("bazel-out/")
    else -> false
  }

@get:ApiStatus.Internal
val OutputLocation.isSource: Boolean
  get() = !isGenerated

@get:ApiStatus.Internal
val OutputLocation.isExternal: Boolean
  get() = this is OutputLocation.External

@get:ApiStatus.Internal
val OutputLocation.isInMainWorkspace: Boolean
  get() = this is OutputLocation.Workspace

@ApiStatus.Internal
fun OutputLocation.isUserCode(repoMapping: RepoMapping): Boolean {
  val localRepositories = repoMapping.getLocalRepositories().localRepositories
  return when (this) {
    is OutputLocation.Workspace -> true
    is OutputLocation.External -> repoName in localRepositories
    is OutputLocation.Output -> externalRepoOfOutput().let { repo -> repo == null || repo in localRepositories }
    is OutputLocation.Host -> false
  }
}

// canonical repository name when the output belongs to an external target, else null
private fun OutputLocation.Output.externalRepoOfOutput(): String? {
  if (!relativePath.startsWith("external/")) {
    return null
  }
  return relativePath.removePrefix("external/").substringBefore('/').takeIf { it.isNotEmpty() }
}

@ApiStatus.Internal
fun OutputLocation.toExecrootPath(): String = when (this) {
  is OutputLocation.External ->
    if (siblingLayout) {
      joinNonEmpty("..", repoName, relativePath)
    }
    else {
      joinNonEmpty("external", repoName, relativePath)
    }

  is OutputLocation.Output -> joinNonEmpty("bazel-out", root.path, relativePath)
  is OutputLocation.Workspace -> this.relativePath
  is OutputLocation.Host -> this.absolutePath
}

internal fun joinNonEmpty(vararg parts: String): String = parts.filter { it.isNotEmpty() }.joinToString("/")

@ApiStatus.Internal
interface OutputLocationCollection {
  fun isEmpty(): Boolean
  fun getOutputLocations(): Sequence<OutputLocation>

  companion object {
    val EMPTY: OutputLocationCollection = object : OutputLocationCollection {
      override fun isEmpty(): Boolean = true
      override fun getOutputLocations(): Sequence<OutputLocation> = sequenceOf()
    }
  }
}

@ApiStatus.Internal
interface OutputLocationResolver {

  /**
   * Resolves the [location] to the file it points to.
   * Respects local repositories override according to [localOverride].
   *
   * @param location output location
   * @param localOverride local repository override use to resolve files
   *
   * @return `null` when the stored path is not a valid path on this platform.
   */
  fun resolve(location: OutputLocation, localOverride: LocalRepositoryMapping? = null): Path?

  companion object {
    val NOOP: OutputLocationResolver = object : OutputLocationResolver {
      override fun resolve(location: OutputLocation, localOverride: LocalRepositoryMapping?): Path? = null
    }
  }
}
