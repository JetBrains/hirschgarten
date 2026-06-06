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
import kotlin.io.path.name
import kotlin.io.path.visitFileTree

// RC: replaces `ResourcesItemToJavaResourceRootTransformer` + `JavaResourceEntityUpdater`;
// the strip-prefix merging logic is moved as-is, the `ResourceRoot` wrapper is dropped
@ApiStatus.Internal
object ResourceRootBuilder {
  data class ResolvedResourceRoot(
    val resourcePath: Path,
    val rootType: SourceRootTypeId,
  )

  fun resolve(
    target: RawBuildTarget,
    bazelProjectName: String,
    testTargets: Set<Label>,
  ): List<ResolvedResourceRoot> {
    val rootType = target.inferRootType(testTargets)
    val stripPrefixes = extractStripPrefixOrNull(target) ?: defaultStripPrefixes(target)
    val resourcesList = target.resources.getFiles().toList()
    if (stripPrefixes.isEmpty()) {
      return rootsForResourcesWithoutPrefix(resourcesList, rootType)
    }
    val resourcesSet = resourcesList.toSet()
    val result = stripPrefixes.fold(MergeResult(leftovers = resourcesSet)) { acc, prefix ->
      acc.mergeUsing(prefix, resourcesSet, bazelProjectName)
    }
    return result.merged.map { ResolvedResourceRoot(resourcePath = it, rootType = rootType) } +
      rootsForResourcesWithoutPrefix(result.leftovers.toList(), rootType)
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
    for (sourceRoot in sourceRoots) {
      addJavaResourceRootPropertiesEntity(storage, sourceRoot)
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
  ) {
    val entity = JavaResourceRootPropertiesEntity(
      generated = DEFAULT_GENERATED,
      relativeOutputPath = DEFAULT_RELATIVE_OUTPUT_PATH,
      entitySource = sourceRoot.entitySource,
    )
    storage.modifySourceRootEntity(sourceRoot) {
      javaResourceRoots += entity
    }
  }

  private fun rootsForResourcesWithoutPrefix(
    resources: List<Path>,
    rootType: SourceRootTypeId,
  ): List<ResolvedResourceRoot> = resources.map { ResolvedResourceRoot(resourcePath = it, rootType = rootType) }

  private fun MergeResult.mergeUsing(stripPrefix: Path, allResourceFiles: Set<Path>, bazelProjectName: String): MergeResult {
    val stripPrefixAncestors = setOf(stripPrefix)
    val newLeftovers = leftovers.filterNotTo(mutableSetOf()) { it.isUnder(stripPrefixAncestors) }
    if (leftovers.size == newLeftovers.size) return this
    if (stripPrefix.containsExtraFilesIgnoringBazelSymlinks(allResourceFiles, bazelProjectName)) return this
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
