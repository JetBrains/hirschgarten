package org.jetbrains.bsp.protocol

import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import com.intellij.util.containers.Interner
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.Debug
import org.jetbrains.bazel.commons.LocalRepositoryMapping
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.getLocalRepositories
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.pathString

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


/**
 * Obtain relative path, for real path resolution use [OutputLocationResolver]
 */
@get:ApiStatus.Internal
val OutputLocation.relativeNioPath: Path
  get() = when (this) {
    is OutputLocation.Workspace -> Path(relativePath)
    is OutputLocation.Output -> Path(relativePath)
    is OutputLocation.External -> Path(relativePath)
    is OutputLocation.Host -> Path(absolutePath)
  }

@ApiStatus.Internal
fun OutputLocation.mapPath(transform: (path: Path) -> Path): OutputLocation {
  fun Path.toOutputRelativePath(): String = this.invariantSeparatorsPathString.also { require(!OSAgnosticPathUtil.isAbsolute(it)) }

  fun OutputLocation.withNioPath(path: Path): OutputLocation = when (this) {
    is OutputLocation.Workspace -> copy(relativePath = path.toOutputRelativePath())
    is OutputLocation.Output -> copy(relativePath = path.toOutputRelativePath())
    is OutputLocation.External -> copy(relativePath = path.toOutputRelativePath())
    is OutputLocation.Host -> copy(absolutePath = path.pathString.also { require(OSAgnosticPathUtil.isAbsolute(it)) })
  }

  return withNioPath(transform(relativeNioPath))
}


@Debug.Renderer(hasChildren = "!isEmpty()", childrenArray = "arrayOfLocations()")
@ApiStatus.Internal
interface OutputLocationCollection {
  fun isEmpty(): Boolean
  fun getOutputLocations(): Sequence<OutputLocation>

  @Suppress("unused")
  fun arrayOfLocations(): Array<OutputLocation> = getOutputLocations().toList().toTypedArray()

  companion object {
    val EMPTY: OutputLocationCollection = object : OutputLocationCollection {
      override fun isEmpty(): Boolean = true
      override fun getOutputLocations(): Sequence<OutputLocation> = sequenceOf()
      override fun equals(other: Any?): Boolean = other is OutputLocationCollection && other.isEmpty()
      override fun hashCode(): Int = EMPTY_CONTENT_HASH.asInt
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
}

private val EMPTY_CONTENT_HASH = contentHash(listOf())

@ApiStatus.Internal
fun OutputLocationCollection.contentEquals(other: OutputLocationCollection): Boolean =
  getOutputLocations().toHashSet() == other.getOutputLocations().toHashSet()

@ApiStatus.Internal
fun OutputLocationCollection.contentHashCode(): Int = contentHash().asInt

@ApiStatus.Internal
fun OutputLocationCollection.contentHash(): HashValue128 = contentHash(getOutputLocations().toHashSet())

// addition is commutative, using that property the same hash will be produced regardless of input order
// that mean we can avoid expensive sorting of location collection
@ApiStatus.Internal
fun contentHash(locations: Iterable<OutputLocation>): HashValue128 {
  val stream = Hashing.xxh3_128().hashStream()
  var hi = 0L
  var lo = 0L
  for (location in locations) {
    val hash = location.hash(stream.reset())
    hi += hash.mostSignificantBits
    lo += hash.leastSignificantBits
  }
  return HashValue128(hi, lo)
}

private fun OutputLocation.hash(stream: HashStream128): HashValue128 {
  when (this) {
    is OutputLocation.Workspace -> {
      stream.putByte(0)
      stream.putString(relativePath)
    }

    is OutputLocation.Output -> {
      stream.putByte(1)
      stream.putString(root.path)
      stream.putString(relativePath)
    }

    is OutputLocation.External -> {
      stream.putByte(2)
      stream.putString(repoName)
      stream.putString(relativePath)
      stream.putBoolean(siblingLayout)
    }

    is OutputLocation.Host -> {
      stream.putByte(3)
      stream.putString(absolutePath)
    }
  }
  return stream.get()
}
