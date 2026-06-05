package org.jetbrains.bazel.workspace.importer

import com.intellij.java.workspace.entities.JavaResourceRootPropertiesEntity
import com.intellij.java.workspace.entities.javaResourceRoots
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import com.intellij.platform.workspace.jps.entities.modifyContentRootEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.jps.entities.modifySourceRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.symlinks.BazelSymlinksCalculator
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import org.jetbrains.bsp.protocol.utils.extractKotlinBuildTarget
import org.jetbrains.bsp.protocol.utils.extractScalaBuildTarget
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.Path as KPath
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.visitFileTree

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
    target: RawBuildTarget,
    bazelProjectName: String,
    testTargets: Set<Label>,
    sourceContentRoots: List<Path> = emptyList(),
  ): List<ResolvedResourceRoot> {
    val rootType = target.inferRootType(testTargets)
    val stripPrefixes = extractStripPrefixOrNull(target) ?: defaultStripPrefixes(target)
    val aggressiveCeiling = target.aggressiveCollapseCeiling()
    val resourceFiles = target.resources.getFiles().toList()
    val canCollapseTopology = resourceFiles.any { it.startsWith(aggressiveCeiling) }
    if (stripPrefixes.isEmpty() && !canCollapseTopology) {
      return rootsForResourcesWithoutPrefix(resourceFiles, rootType, sourceContentRoots)
    }
    val resourcesSet = resourceFiles.toSet()
    val dirtinessCache = DirtinessCache(resourcesSet, bazelProjectName)
    val result = stripPrefixes.fold(MergeResult(leftovers = resourcesSet)) { acc, prefix ->
      acc.mergeUsing(prefix, dirtinessCache)
    }
    val leftoverPaths = collapseLeftoversByTopology(
      leftovers = result.leftovers,
      alreadyMerged = result.merged,
      aggressiveCeiling = aggressiveCeiling,
      dirtinessCache = dirtinessCache,
    )
    return (result.merged + leftoverPaths).map { path ->
      ResolvedResourceRoot(
        resourcePath = path,
        rootType = rootType,
        relativeOutputPath = computeRelativeOutputPath(rootForFqnComputation(path), sourceContentRoots),
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
    val contentRoots = addContentRoots(resources.map { it.resourcePath }, parentModuleEntity, virtualFileUrlManager, storage)
    val sourceRoots = (resources zip contentRoots).map { (resource, contentRoot) ->
      addSourceRootEntity(storage, contentRoot, resource, parentModuleEntity, virtualFileUrlManager)
    }
    for ((sourceRoot, resource) in sourceRoots zip resources) {
      addJavaResourceRootPropertiesEntity(storage, sourceRoot, resource.relativeOutputPath)
    }
  }

  private fun BuildTarget.inferRootType(testTargets: Set<Label>): SourceRootTypeId =
    if (kind.ruleType == RuleType.TEST || id in testTargets) {
      JAVA_TEST_RESOURCE_ROOT_TYPE
    }
    else {
      JAVA_RESOURCE_ROOT_TYPE
    }

  private fun addContentRoots(
    paths: List<Path>,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ): List<ContentRootEntity> {
    val entitySource = parentModuleEntity.entitySource
    val entities = paths.map { path ->
      ContentRootEntity(
        url = path.toResolvedVirtualFileUrl(virtualFileUrlManager),
        excludedPatterns = emptyList(),
        entitySource = entitySource,
      )
    }
    val updated = storage.modifyModuleEntity(parentModuleEntity) {
      contentRoots += entities
    }
    return updated.contentRoots.takeLast(entities.size)
  }

  private fun addSourceRootEntity(
    storage: MutableEntityStorage,
    contentRoot: ContentRootEntity,
    resource: ResolvedResourceRoot,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): SourceRootEntity {
    val entity = SourceRootEntity(
      url = resource.resourcePath.toVirtualFileUrl(virtualFileUrlManager),
      rootTypeId = resource.rootType,
      entitySource = parentModuleEntity.entitySource,
    )
    val updated = storage.modifyContentRootEntity(contentRoot) {
      sourceRoots += entity
    }
    return updated.sourceRoots.last()
  }

  private fun addJavaResourceRootPropertiesEntity(
    storage: MutableEntityStorage,
    sourceRoot: SourceRootEntity,
    relativeOutputPath: String,
  ) {
    val entity = JavaResourceRootPropertiesEntity(
      generated = DEFAULT_GENERATED,
      relativeOutputPath = relativeOutputPath,
      entitySource = sourceRoot.entitySource,
    )
    storage.modifySourceRootEntity(sourceRoot) {
      javaResourceRoots += entity
    }
  }

  private fun rootsForResourcesWithoutPrefix(
    resources: List<Path>,
    rootType: SourceRootTypeId,
    sourceContentRoots: List<Path>,
  ): List<ResolvedResourceRoot> = resources.map { path ->
    ResolvedResourceRoot(
      resourcePath = path,
      rootType = rootType,
      relativeOutputPath = computeRelativeOutputPath(path.parent ?: path, sourceContentRoots),
    )
  }

  /**
   * The reference point for computing `relativeOutputPath`. For a directory resource root the
   * directory itself is the reference; for a file-level root the file's parent is, so a file at
   * `kotlin/messages/XXX.properties` and a directory root at `kotlin/messages/` produce the same
   * relative output path when both sit inside a source root at `kotlin/`.
   */
  private fun rootForFqnComputation(path: Path): Path =
    if (path.isDirectory()) path else path.parent ?: path

  /**
   * When a resource root sits strictly inside one of the module's source content roots, expose
   * the path-from-enclosing-source-root as the resource's package prefix. This keeps `getResource`
   * / `@PropertyKey` resolution behaving as if the source root still owned the subtree - a pure
   * Java-visibility fix, not a content-root layout change.
   */
  private fun computeRelativeOutputPath(referencePath: Path, sourceContentRoots: List<Path>): String {
    val enclosing = sourceContentRoots
                      .filter { referencePath != it && referencePath.startsWith(it) }
                      .maxByOrNull { it.nameCount } ?: return DEFAULT_RELATIVE_OUTPUT_PATH
    val relative = enclosing.relativize(referencePath)
    return (0 until relative.nameCount).joinToString("/") { relative.getName(it).name }
  }

  private fun collapseLeftoversByTopology(
    leftovers: Set<Path>,
    alreadyMerged: List<Path>,
    aggressiveCeiling: Path,
    dirtinessCache: DirtinessCache,
  ): List<Path> {
    val collapsed = LinkedHashSet<Path>()
    val byParent = leftovers.groupBy { it.parent }
    for ((parent, group) in byParent) {
      val collapseTarget = if (parent == null) {
        null
      }
      else {
        val aggressive = findHighestCollapseAncestor(parent, aggressiveCeiling, alreadyMerged, dirtinessCache)
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
    // FQN they would have had through the enclosing source root.
    val overlapsMerged = alreadyMerged.any { it.startsWith(parent) || parent.startsWith(it) }
    return !overlapsMerged && !dirtinessCache.isDirty(parent)
  }

  private class DirtinessCache(
    private val allResourceFiles: Set<Path>,
    private val bazelProjectName: String,
  ) {
    private val cache = HashMap<Path, Boolean>()
    fun isDirty(path: Path): Boolean = cache.getOrPut(path) {
      path.containsExtraFilesIgnoringBazelSymlinks(allResourceFiles, bazelProjectName)
    }
  }

  private fun RawBuildTarget.aggressiveCollapseCeiling(): Path = baseDirectory

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

  private fun Path.containsExtraFilesIgnoringBazelSymlinks(files: Set<Path>, bazelProjectName: String): Boolean {
    var result = false
    visitFileTree(followLinks = true) {
      onPreVisitDirectory { directory, _ ->
        when {
          BazelSymlinksCalculator.isBazelSymlink(bazelProjectName, directory) -> FileVisitResult.SKIP_SUBTREE
          else -> FileVisitResult.CONTINUE
        }
      }
      onVisitFile { file, _ ->
        when (file) {
          !in files -> {
            result = true
            FileVisitResult.TERMINATE
          }

          else -> FileVisitResult.CONTINUE
        }
      }
    }
    return result
  }

  private fun extractStripPrefixOrNull(target: RawBuildTarget) = extractJvmBuildTarget(target)
    ?.resolvedResourceStripPrefix
    ?.let(::setOf)

  private fun defaultStripPrefixes(target: RawBuildTarget): Set<Path> = when {
    extractKotlinBuildTarget(target) != null -> defaultStripPrefixesKotlin(target)
    extractScalaBuildTarget(target) != null -> defaultStripPrefixesScala(target)
    extractJvmBuildTarget(target) != null -> defaultStripPrefixesJava(target)
    else -> emptySet()
  }

  private fun defaultStripPrefixesJava(target: RawBuildTarget): MutableSet<Path> = target
    .resources
    .getFiles()
    .mapNotNullTo(mutableSetOf()) { it.takeSrcResourcesPrefixOrNull() ?: it.takeJavaLayoutPrefixOrNull() }

  private fun defaultStripPrefixesKotlin(target: RawBuildTarget): Set<Path> {
    val resources = target.resources.getFiles().toList()
    return kotlinConventionalSegments
      .flatMapTo(mutableSetOf()) { resources.findPrefixesEndingWith(it) }
  }

  private fun defaultStripPrefixesScala(target: RawBuildTarget): Set<Path> {
    val resources = target.resources.getFiles().toList()
    val externalPrefixes = resources.mapNotNullTo(mutableSetOf()) { resource ->
      val index = resource.indexOfFirst { it.name == "external" }
      val next = index + 1
      if (index == -1) return@mapNotNullTo null
      if (next >= resource.nameCount) return@mapNotNullTo null
      resource.rootSubpath(until = next + 1)
    }
    val conventionalPrefixes = scalaConventionalSegments
      .flatMapTo(mutableSetOf()) { resources.findPrefixesEndingWith(it) }
    return externalPrefixes + conventionalPrefixes
  }

  private fun Path.takeSrcResourcesPrefixOrNull(): Path? {
    for (i in 0..<nameCount - 2) {
      if (getName(i).name == "src" && getName(i + 2).name == "resources") {
        return this.rootSubpath(until = i + 3)
      }
    }
    return null
  }

  private fun Path.takeJavaLayoutPrefixOrNull(): Path? {
    forEachIndexed { segmentIndex, segment ->
      when (segment.name) {
        "javatests", "testsrc", "java" -> return this.takeSrcJavaOrNull(at = segmentIndex) ?: this.rootSubpath(until = segmentIndex + 1)
        "src" -> {
          for (subSegmentIndex in segmentIndex + 1..<nameCount - 1) {
            val subSegment = getName(subSegmentIndex)
            if (subSegment.name == "src" || subSegment.name == "java" || subSegment.name == "javatests") {
              val packageCandidate = getName(subSegmentIndex + 1)
              if (packageCandidate.name in javaCommonPackagePrefixes) return this.rootSubpath(until = subSegmentIndex + 1)
              return this.takeSrcJavaOrNull(at = segmentIndex) ?: this.rootSubpath(until = segmentIndex + 1)
            }
          }
          return this.takeSrcJavaOrNull(at = segmentIndex) ?: this.rootSubpath(until = segmentIndex + 1)
        }
      }
    }
    return null
  }

  private fun Path.takeSrcJavaOrNull(at: Int): Path? {
    if (nameCount <= at + 2) return null
    if (getName(at).name != "src") return null
    if (getName(at + 1).name != "main" && getName(at + 1).name != "test") return null
    if (getName(at + 2).name == "java") return this.rootSubpath(at + 3)
    return null
  }

  private fun List<Path>.findPrefixesEndingWith(path: Path) = mapNotNullTo(mutableSetOf()) { it.takePrefixEndingWithOrNull(path) }

  private fun Path.takePrefixEndingWithOrNull(suffix: Path): Path? {
    require(!suffix.isAbsolute)
    if (suffix.nameCount < 1) return null
    val first = suffix.getName(0) ?: return null
    val index = indexOf(first)
    if (index == -1) return null
    for (i in 1..<suffix.nameCount) {
      if (index + i >= nameCount) return null
      if (getName(index + i) != suffix.getName(i)) return null
    }
    return this.rootSubpath(until = index + suffix.nameCount)
  }

  private fun Path.rootSubpath(until: Int) = root?.resolve(subpath(0, until)) ?: subpath(0, until)

  private data class MergeResult(
    val merged: List<Path> = emptyList(),
    val leftovers: Set<Path> = emptySet(),
  )

  private const val DEFAULT_GENERATED = false
  private const val DEFAULT_RELATIVE_OUTPUT_PATH = ""

  private val javaCommonPackagePrefixes = setOf("com", "org", "net")

  private val kotlinConventionalSegments = listOf(
    KPath("src/main/java"),
    KPath("src/main/resources"),
    KPath("src/test/java"),
    KPath("src/test/resources"),
    KPath("kotlin"),
  )

  private val scalaConventionalSegments = listOf(
    KPath("resources"),
    KPath("java"),
  )
}
