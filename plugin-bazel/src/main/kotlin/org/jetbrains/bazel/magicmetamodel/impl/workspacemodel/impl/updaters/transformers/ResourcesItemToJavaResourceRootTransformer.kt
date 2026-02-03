package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ScalaBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import java.nio.file.Path
import kotlin.collections.toList
import kotlin.io.path.Path
import kotlin.io.path.name

class ResourcesItemToJavaResourceRootTransformer : WorkspaceModelEntityPartitionTransformer<RawBuildTarget, ResourceRoot> {

  override fun transform(inputEntity: RawBuildTarget): List<ResourceRoot> {
    val rootType = inputEntity.inferRootType()
    val stripPrefixes = extractStripPrefixOrNull(inputEntity) ?: defaultStripPrefixes(inputEntity)
    if (stripPrefixes.isEmpty()) return rootsForResourcesWithoutPrefix(inputEntity.resources, rootType)
    val result = stripPrefixes.fold(MergeResult(leftovers = inputEntity.resources.toSet())) { acc, it ->
      acc.mergeUsing(stripPrefix = it)
    }
    return result
      .merged
      .map { ResourceRoot(resourcePath = it, rootType = rootType) }
      .plus(rootsForResourcesWithoutPrefix(result.leftovers.toList(), rootType))
  }

  // TODO BAZEL-2874
  //      Solution below probably won't work correctly and there seems to be no easy way to fix it for now
  private fun rootsForResourcesWithoutPrefix(
    resources: List<Path>,
    rootType: SourceRootTypeId,
  ): List<ResourceRoot> = resources.map { ResourceRoot(resourcePath = it, rootType = rootType) }

  private fun MergeResult.mergeUsing(stripPrefix: Path): MergeResult {
    val stripPrefixAncestors = setOf(stripPrefix)
    val newLeftovers = leftovers.filterNotTo(mutableSetOf()) { it.isUnder(stripPrefixAncestors) }
    if (leftovers.size == newLeftovers.size) return this
    return MergeResult(
      merged = merged.plusElement(stripPrefix),
      leftovers = newLeftovers,
    )
  }

  private fun extractStripPrefixOrNull(target: RawBuildTarget) = extractJvmBuildTarget(target)
    ?.resourceStripPrefix
    ?.let(::setOf)

  private fun defaultStripPrefixes(target: RawBuildTarget) = when (target.data) {
    is JvmBuildTarget -> defaultStripPrefixesJava(target)
    is KotlinBuildTarget -> defaultStripPrefixesKotlin(target)
    is ScalaBuildTarget -> defaultStripPrefixesScala(target)
    else -> emptySet()
  }

  /**
   * Based on logic from Bazel code. Split into:
   * * [takeSrcResourcesPrefixOrNull]
   * * [takeJavaLayoutPrefixOrNull]
   * */
  private fun defaultStripPrefixesJava(target: RawBuildTarget) = target
    .resources
    .mapNotNullTo(mutableSetOf()) { it.takeSrcResourcesPrefixOrNull() ?: it.takeJavaLayoutPrefixOrNull() }

  /**
   * Based on logic from [rules_kotlin](https://github.com/bazelbuild/rules_kotlin/blame/0f0ec19b4339f9dde4183d938e6acb465bc5e45a/kotlin/internal/jvm/compile.bzl#L154)
   *
   *  Differences:
   *  * `rules_kotlin` uses string to find conventional segments, which leads to bizarre behavior e.g.,
   *    resource under `kotliner/resource.txt` will be placed under `er/resource.txt`.
   *    It's not handled here because it seems unintentional in `rules_kotlin` implementation - we expect a segment strictly named `kotlin`.
   */
  private fun defaultStripPrefixesKotlin(target: RawBuildTarget): Set<Path> = kotlinConventionalSegments
    .flatMapTo(mutableSetOf()) {
      target.resources.findPrefixesEndingWith(it)
    }

  /**
   * Based on logic from [rules_scala](https://github.com/bazel-contrib/rules_scala/blob/18518778580cc753c787d017923112fadf29a5e9/scala/private/resources.bzl#L32)
   *
   * Differences:
   *  * `rules_scala` uses string to find conventional segments, which leads to bizarre behavior e.g.,
   *    resource under `resourcess/resource.txt` will be placed under `s/resource.txt`.
   *    It's not handled here because it seems unintentional in `rules_scala` implementation - we expect a segment strictly named `resources`.
   *  * `bazel-out` is not supported because it's excluded by default
   */
  private fun defaultStripPrefixesScala(target: RawBuildTarget): Set<Path> {
    val externalPrefixes = target.resources.mapNotNullTo(mutableSetOf()) { resource ->
      val index = resource.indexOfFirst { it.name == "external" }
      val next = index + 1
      if (index == -1) return@mapNotNullTo null
      if (next >= resource.nameCount) return@mapNotNullTo null
      resource.rootSubpath(until = next + 1)
    }
    val conventionalPrefixes = scalaConventionalSegments
      .flatMapTo(mutableSetOf()) {
        target.resources.findPrefixesEndingWith(it)
      }
    return externalPrefixes + conventionalPrefixes
  }

  /**
   * Based on [BazelJavaSemantics.getDefaultJavaResourcePath from Bazel](https://github.com/bazelbuild/bazel/blob/08e077e7a46b5f2137cf3335104219133f8d997f/src/main/java/com/google/devtools/build/lib/bazel/rules/java/BazelJavaSemantics.java#L65)
   */
  private fun Path.takeSrcResourcesPrefixOrNull(): Path? {
    for (i in 0..<nameCount - 2) {
      if (getName(i).name == "src" && getName(i + 2).name == "resources") {
        return this.rootSubpath(until = i + 3)
      }
    }
    return null
  }

  /**
   * Based on [JavaUtil.getJavaPath from Bazel](https://github.com/bazelbuild/bazel/blob/fa0223034211004e624b542065f355413dfb3413/src/main/java/com/google/devtools/build/lib/rules/java/JavaUtil.java#L70)
   *
   * Differences:
   * * Ignores src/main|tests/resources because it should be handled by [takeSrcResourcesPrefixOrNull]
   * * Ignores `rootIndex == 0` case from original implementation.
   *   It's hard to reproduce and test, because it requires `javatests`, `testsrc` and `java` directories to be directly under root e.g., `/`.
   */
  private fun Path.takeJavaLayoutPrefixOrNull(): Path? {
    forEachIndexed { segmentIndex, segment ->
      when (segment.name) {
        "javatests", "testsrc", "java" -> return this.takeSrcJavaOrNull(at = segmentIndex) ?: this.rootSubpath(until = segmentIndex + 1)
        "src" -> {
          for (subSegmentIndex in segmentIndex + 1 ..< nameCount - 1) {
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

  private fun List<Path>.findPrefixesEndingWith(path: Path) = mapNotNullTo(mutableSetOf()) {
    it.takePrefixEndingWithOrNull(path)
  }

  private fun Path.takePrefixEndingWithOrNull(suffix: Path): Path? {
    require(!suffix.isAbsolute)
    if (suffix.nameCount < 1) return null
    val first = suffix.getName(0) ?: return null
    val index = indexOf(first)
    if (index == -1) return null
    for (i in 1 ..< suffix.nameCount) {
      if (index + i >= nameCount) return null
      if (getName(index + i) != suffix.getName(i)) return null
    }
    return this.rootSubpath(until = index + suffix.nameCount)
  }

  private fun Path.rootSubpath(until: Int) = root?.resolve(subpath(0, until)) ?: subpath(0, until)

  private fun BuildTarget.inferRootType(): SourceRootTypeId = when (kind.ruleType) {
    RuleType.TEST -> JAVA_TEST_RESOURCE_ROOT_TYPE
    else -> JAVA_RESOURCE_ROOT_TYPE
  }

  private data class MergeResult(
    val merged: List<Path> = emptyList(),
    val leftovers: Set<Path> = emptySet(),
  )

  companion object {

    private val javaCommonPackagePrefixes = setOf("com", "org", "net")

    private val kotlinConventionalSegments = listOf(
      Path("src/main/java"),
      Path("src/main/resources"),
      Path("src/test/java"),
      Path("src/test/resources"),
      Path("kotlin"),
    )

    private val scalaConventionalSegments = listOf(
      Path("resources"),
      Path("java"),
    )
  }
}
