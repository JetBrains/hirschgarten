package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.commons.symlinks.BazelConvenienceSymlinksScanner
import org.jetbrains.bazel.sync.workspace.languages.jvm.BazelConventionalRoots
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractJvmBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractKotlinBuildTarget
import org.jetbrains.bazel.sync.workspace.languages.jvm.extractScalaBuildTarget
import org.jetbrains.bazel.sync.workspace.snapshot.isTestTarget
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.toRepoRelativePath
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

private val log = logger<ResourceRootBuilder>()

// RC: replaces `ResourcesItemToJavaResourceRootTransformer` + `JavaResourceEntityUpdater`;
// the strip-prefix merging logic is moved as-is, the `ResourceRoot` wrapper is dropped
@ApiStatus.Internal
object ResourceRootBuilder {
  data class ResolvedResourceRoot(
    val resourcePath: Path,
    val rootType: SourceRootTypeId,
    val relativeOutputPath: String = DEFAULT_RELATIVE_OUTPUT_PATH,
  )

  fun resolve(
    target: BuildTarget,
    baseDirectory: Path?,
    symlinksScanner: BazelConvenienceSymlinksScanner,
    resolveLocation: (OutputLocation) -> Path?,
  ): List<ResolvedResourceRoot> {
    val rootType = target.inferRootType()
    val resourceFiles = target.resources.resolvePaths(resolveLocation)
    if (resourceFiles.isEmpty()) return emptyList()
    val stripPrefixes = extractStripPrefixOrNull(target, resolveLocation)
                        ?: defaultStripPrefixes(target, resourceFiles, resolveLocation, symlinksScanner.workspaceRoot)
    val resourcesSet = resourceFiles.toSet()
    val dirtinessCache = DirtinessCache(resourcesSet, symlinksScanner)
    val result = stripPrefixes.fold(MergeResult(leftovers = resourcesSet)) { acc, prefix ->
      acc.mergeUsing(prefix, dirtinessCache)
    }
    val leftoverPaths = collapseLeftoversByTopology(
      leftovers = result.leftovers,
      alreadyMerged = result.merged,
      ceilingFor = { parent -> collapseCeiling(parent, baseDirectory, symlinksScanner.workspaceRoot) },
      dirtinessCache = dirtinessCache,
    )
    return (result.merged + leftoverPaths).map { path ->
      ResolvedResourceRoot(
        resourcePath = path,
        rootType = rootType,
        relativeOutputPath = computeRelativeOutputPath(rootForFqnComputation(path), stripPrefixes),
      )
    }
  }

  fun write(
    resources: List<ResolvedResourceRoot>,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ) {
    if (resources.isEmpty()) {
      return
    }
    val entitySource = parentModuleEntity.entitySource
    val contentRootEntities = resources.map { resource ->
      ContentRootEntity(
        url = resource.resourcePath.toResolvedVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
        entitySource = entitySource,
      ) {
        this.sourceRoots = listOf(
          SourceRootEntity(
            url = resource.resourcePath.toVirtualFileUrl(virtualFileUrlManager),
            rootTypeId = resource.rootType,
            entitySource = entitySource,
          ) {
            this.javaResourceRoots = listOf(
              JavaResourceRootPropertiesEntity(
                generated = DEFAULT_GENERATED,
                relativeOutputPath = resource.relativeOutputPath,
                entitySource = entitySource,
              ),
            )
          },
        )
      }
    }
    storage.modifyModuleEntity(parentModuleEntity) {
      contentRoots += contentRootEntities
    }
  }

  private fun BuildTarget.inferRootType(): SourceRootTypeId =
    if (isTestTarget()) {
      JAVA_TEST_RESOURCE_ROOT_TYPE
    }
    else {
      JAVA_RESOURCE_ROOT_TYPE
    }

  /**
   * The reference point for computing `relativeOutputPath`. For a directory resource root the
   * directory itself is the reference; for a file-level root the file's parent is, so a file at
   * `kotlin/messages/XXX.properties` and a directory root at `kotlin/messages/` produce the same
   * relative output path when both sit inside a strip prefix `kotlin/`.
   */
  private fun rootForFqnComputation(path: Path): Path =
    if (path.isDirectory()) path else path.parent ?: path

  /**
   * A strip prefix is the directory which Bazel maps to the classpath root. When a resource root can't be the strip prefix
   * itself (e.g., the strip prefix also contains sources) and sits strictly inside it, expose the path from the strip prefix
   * as the resource's package prefix, so `getResource` / `@PropertyKey` resolve the same path as at runtime.
   */
  private fun computeRelativeOutputPath(referencePath: Path, stripPrefixes: Set<Path>): String {
    val enclosing = stripPrefixes
                      .filter { referencePath != it && referencePath.startsWith(it) }
                      .maxByOrNull { it.nameCount } ?: return DEFAULT_RELATIVE_OUTPUT_PATH
    val relative = enclosing.relativize(referencePath)
    return (0 until relative.nameCount).joinToString("/") { relative.getName(it).name }
  }

  private fun collapseLeftoversByTopology(
    leftovers: Set<Path>,
    alreadyMerged: List<Path>,
    ceilingFor: (Path) -> Path?,
    dirtinessCache: DirtinessCache,
  ): List<Path> {
    val collapsed = LinkedHashSet<Path>()
    val byParent = leftovers.groupBy { it.parent }
    for ((parent, group) in byParent) {
      val collapseTarget = if (parent == null) {
        null
      }
      else {
        val aggressive = ceilingFor(parent)?.let { findHighestCollapseAncestor(parent, it, alreadyMerged, dirtinessCache) }
        aggressive ?: parent.takeIf { canCollapseStrictly(it, byParent, alreadyMerged, dirtinessCache) }
      }
      if (collapseTarget != null) {
        collapsed.add(collapseTarget)
      }
      else {
        collapsed.addAll(group)
      }
    }
    return collapsed.toList()
  }

  private fun canCollapseStrictly(
    parent: Path,
    byParent: Map<Path?, List<Path>>,
    alreadyMerged: List<Path>,
    dirtinessCache: DirtinessCache,
  ): Boolean {
    // any other leftover group sitting in a subdirectory of parent would have its classpath
    // name change if we collapsed here, so refuse. Cheap O(P) over the byParent keyset
    // instead of O(N) over allResourceFiles.
    val hasNestedLeftoverGroup = byParent.any { (otherParent, _) ->
      otherParent != null && otherParent != parent && otherParent.startsWith(parent)
    }
    return !hasNestedLeftoverGroup && canCollapseToPath(parent, alreadyMerged, dirtinessCache)
  }

  /**
   * Aggressive mode: walk up from the leftover's immediate parent toward the ceiling and pick the
   * highest path that still satisfies cleanliness (see DirtinessCache) / no-overlap / no-source-nesting.
   */
  private fun findHighestCollapseAncestor(
    immediateParent: Path,
    ceiling: Path,
    alreadyMerged: List<Path>,
    dirtinessCache: DirtinessCache,
  ): Path? {
    if (!immediateParent.startsWith(ceiling)) {
      return null
    }
    var best: Path? = null
    var candidate: Path? = immediateParent
    while (candidate != null && candidate.startsWith(ceiling)) {
      if (!canCollapseToPath(candidate, alreadyMerged, dirtinessCache)) {
        break
      }
      best = candidate
      if (candidate == ceiling) {
        break
      }
      candidate = candidate.parent
    }
    return best
  }

  private fun canCollapseToPath(
    parent: Path,
    alreadyMerged: List<Path>,
    dirtinessCache: DirtinessCache,
  ): Boolean {
    // no source-content-root rejection: a resource root nested inside a source content root is
    // fine because we set `relativeOutputPath` on the resulting root so its files keep the same
    // FQN they would have had through the enclosing strip prefix.
    val overlapsMerged = alreadyMerged.any { it.startsWith(parent) || parent.startsWith(it) }
    return !overlapsMerged && !dirtinessCache.isDirty(parent)
  }

  private class DirtinessCache(
    private val allResourceFiles: Set<Path>,
    private val symlinksScanner: BazelConvenienceSymlinksScanner,
  ) {
    private val cache = HashMap<VirtualFile, Boolean>()

    fun isDirty(path: Path): Boolean {
      val file = path.findVirtualFile() ?: path.refreshAndFindVirtualFileOrDirectory()
      if (file == null) {
        log.warn("Resource path does not exist, skipping dirtiness check: $path")
        return false
      }
      return isDirty(file)
    }

    private fun isDirty(file: VirtualFile): Boolean = cache.getOrPut(file) {
      file.containsExtraFilesIgnoringBazelSymlinks()
    }

    private fun VirtualFile.containsExtraFilesIgnoringBazelSymlinks(): Boolean {
      if (!isDirectory) {
        return toNioPath() !in allResourceFiles
      }
      if (children == null) {
        return false
      }
      // symlinked directories are never walked
      return children.any { child ->
        when {
          child.`is`(VFileProperty.SYMLINK) -> child.isDirtySymlink()
          child.isDirectory -> isDirty(child) // recursion goes through the memoizing cache
          else -> child.toNioPath() !in allResourceFiles
        }
      }
    }

    private fun VirtualFile.isDirtySymlink(): Boolean = when {
      symlinksScanner.isBazelSymlink(this) -> false
      isDirectory -> true // symlinked directory: foreign content, dirty without descending
      else -> toNioPath() !in allResourceFiles // symlink to a file (or broken): treat like a regular file entry
    }
  }

  // here we have two distinct cases:
  //  - candidate is located under OWNING target package, then use it as celling
  //  - candidate is outside OWNING target package, then target is probably a filegroup
  //    in this case treat closest bazel package as collapse celling, generally safe
  // by widening scope of resource file merging we prevent creating single file resource content roots per
  // resource file which with big enough resource file set can cause OOM
  private fun collapseCeiling(parent: Path, baseDirectory: Path?, workspaceRoot: Path): Path? =
    if (baseDirectory != null && parent.startsWith(baseDirectory)) baseDirectory else findClosestOwningPackage(parent, workspaceRoot)

  private fun findClosestOwningPackage(path: Path, workspaceRoot: Path): Path? {
    var next: Path? = path
    while (next != null && next.startsWith(workspaceRoot)) {
      val candidate = next

      // hit celling, bail
      if (candidate == workspaceRoot) {
        return null
      }

      // nearest package found
      if (Constants.BUILD_FILE_NAMES.any { candidate.resolve(it).isRegularFile() }) {
        return candidate
      }

      next = candidate.parent
    }
    return null
  }

  private fun MergeResult.mergeUsing(stripPrefix: Path, dirtinessCache: DirtinessCache): MergeResult {
    val stripPrefixAncestors = setOf(stripPrefix)
    val newLeftovers = leftovers.filterNotTo(mutableSetOf()) { it.isUnder(stripPrefixAncestors) }
    if (leftovers.size == newLeftovers.size) return this
    if (dirtinessCache.isDirty(stripPrefix)) return this
    return MergeResult(
      merged = merged.plusElement(stripPrefix),
      leftovers = newLeftovers,
    )
  }

  private fun extractStripPrefixOrNull(target: BuildTarget, resolveLocation: (OutputLocation) -> Path?) = extractJvmBuildTarget(target)
    ?.resolvedResourceStripPrefix
    ?.let(resolveLocation)
    ?.let(::setOf)

  private fun defaultStripPrefixes(
    target: BuildTarget,
    resources: List<Path>,
    resolveLocation: (OutputLocation) -> Path?,
    workspaceRoot: Path,
  ): Set<Path> = when {
    extractKotlinBuildTarget(target) != null -> defaultStripPrefixesKotlin(target, resolveLocation, workspaceRoot)
    extractScalaBuildTarget(target) != null -> defaultStripPrefixesScala(target, resources, resolveLocation, workspaceRoot)
    extractJvmBuildTarget(target) != null -> defaultStripPrefixesJava(target, resolveLocation, workspaceRoot)
    else -> emptySet()
  }

  /**
   * Returns the strip prefixes that `rules_java` uses when `resource_strip_prefix` is not set.
   * Each prefix is the resource root that [BazelConventionalRoots.javaResourceRootIndex] finds.
   */
  private fun defaultStripPrefixesJava(
    target: BuildTarget,
    resolveLocation: (OutputLocation) -> Path?,
    workspaceRoot: Path,
  ): Set<Path> = stripPrefixes(target, resolveLocation, workspaceRoot, BazelConventionalRoots::javaResourceRootIndex)

  /**
   * Returns the strip prefixes that `rules_kotlin` uses when `resource_strip_prefix` is not set.
   * Each prefix ends with the first marker of [KOTLIN_RESOURCE_MARKERS] that the path contains.
   */
  private fun defaultStripPrefixesKotlin(
    target: BuildTarget,
    resolveLocation: (OutputLocation) -> Path?,
    workspaceRoot: Path,
  ): Set<Path> = stripPrefixes(target, resolveLocation, workspaceRoot) { it.markerEndIndex(KOTLIN_RESOURCE_MARKERS) }

  /**
   * Returns the strip prefixes that `rules_scala` uses when `resource_strip_prefix` is not set.
   * Each prefix ends with the first marker of [SCALA_RESOURCE_MARKERS] that the path contains.
   * A resource of an external repository also gets the root of that repository as a prefix.
   */
  private fun defaultStripPrefixesScala(
    target: BuildTarget,
    resources: List<Path>,
    resolveLocation: (OutputLocation) -> Path?,
    workspaceRoot: Path,
  ): Set<Path> {
    val externalPrefixes = resources.mapNotNullTo(mutableSetOf()) { resource ->
      val index = resource.indexOfFirst { it.name == "external" }
      val next = index + 1
      if (index == -1) return@mapNotNullTo null
      if (next >= resource.nameCount) return@mapNotNullTo null
      resource.rootSubpath(until = next + 1)
    }
    val conventionalPrefixes = stripPrefixes(target, resolveLocation, workspaceRoot) { it.markerEndIndex(SCALA_RESOURCE_MARKERS) }
    return externalPrefixes + conventionalPrefixes
  }

  /**
   * Returns a strip prefix for each resource of [target].
   * [rootIndex] gives the index of the resource root in the segments of a path relative to the root of the repository.
   * Bazel uses this relative path, so a directory above the workspace root does not count.
   */
  private fun stripPrefixes(
    target: BuildTarget,
    resolveLocation: (OutputLocation) -> Path?,
    workspaceRoot: Path,
    rootIndex: (segments: List<String>) -> Int?,
  ): Set<Path> = target
    .resources
    .getOutputLocations()
    .mapNotNullTo(HashSet()) { location ->
      val path = resolveLocation(location) ?: return@mapNotNullTo null
      val relativePath = location.toRepoRelativePath()?.invariantSeparatorsPathString
                         ?: path.relativeToRootOrNull(workspaceRoot)
                         ?: return@mapNotNullTo null
      // a hard link of a generated file has another path, so the prefix is unknown
      if (!path.endsWith(relativePath)) return@mapNotNullTo null
      val segments = relativePath.split('/').filter { it.isNotEmpty() }
      val index = rootIndex(segments) ?: return@mapNotNullTo null
      path.rootSubpath(until = path.nameCount - (segments.lastIndex - index))
    }

  private fun Path.relativeToRootOrNull(root: Path): String? =
    if (startsWith(root) && this != root) root.relativize(this).invariantSeparatorsPathString else null

  /**
   * Returns the index of the last segment of the first marker in [markers] that the directories of this path contain.
   * The rules check the markers in their order, and each marker can match at any depth.
   * A marker matches whole segments, so `kotlin` does not match `mykotlinlib`.
   */
  private fun List<String>.markerEndIndex(markers: List<List<String>>): Int? {
    val directories = subList(0, lastIndex)
    return markers.firstNotNullOfOrNull { marker ->
      val start = directories.windowed(marker.size).indexOf(marker)
      if (start >= 0) start + marker.lastIndex else null
    }
  }

  private fun Path.rootSubpath(until: Int) = root?.resolve(subpath(0, until)) ?: subpath(0, until)

  private data class MergeResult(
    val merged: List<Path> = emptyList(),
    val leftovers: Set<Path> = emptySet(),
  )

  private const val DEFAULT_GENERATED = false
  private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""

  /**
   * The markers of `_CONVENTIONAL_RESOURCE_PATHS` in
   * [compile.bzl](https://github.com/bazelbuild/rules_kotlin/blob/v2.4.21/kotlin/internal/jvm/compile.bzl) of `rules_kotlin`, in its order.
   */
  private val KOTLIN_RESOURCE_MARKERS: List<List<String>> =
    listOf("src/main/java", "src/main/resources", "src/test/java", "src/test/resources", "kotlin").map { it.split('/') }

  /**
   * The markers of `_target_path_by_default_prefixes` in
   * [resources.bzl](https://github.com/bazelbuild/rules_scala/blob/master/scala/private/resources.bzl) of `rules_scala`, in its order.
   */
  private val SCALA_RESOURCE_MARKERS: List<List<String>> = listOf(listOf("resources"), listOf("java"))
}
