package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.utils.extractJvmBuildTarget
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.io.path.walk

class ResourcesItemToJavaResourceRootTransformer : WorkspaceModelEntityPartitionTransformer<RawBuildTarget, ResourceRoot> {

  override fun transform(inputEntity: RawBuildTarget): List<ResourceRoot> {
    val rootType = inputEntity.inferRootType()
    val stripPrefixes = extractStripPrefixes(inputEntity)
    val sourcesSet = inputEntity.sources.mapTo(mutableSetOf()) { it.path }
    if (stripPrefixes.isEmpty()) return rootsForResourcesWithoutPrefix(inputEntity.resources, rootType)
    val result = stripPrefixes.fold(MergeResult(leftovers = inputEntity.resources.toSet())) { acc, it ->
      acc.mergeUsing(it, sourcesSet)
    }
    return result
      .merged
      .map { ResourceRoot(resourcePath = it.path, rootType = rootType, excluded = it.excluded) }
      .plus(rootsForResourcesWithoutPrefix(result.leftovers.toList(), rootType))
  }

  // TODO BAZEL-2874
  //      Solution below probably won't work correctly and there seems to be no easy way to fix it for now
  private fun rootsForResourcesWithoutPrefix(
    resources: List<Path>,
    rootType: SourceRootTypeId
  ): List<ResourceRoot> = resources.map { ResourceRoot(resourcePath = it, rootType = rootType) }

  private fun MergeResult.mergeUsing(stripPrefix: Path, sourcesSet: Set<Path>): MergeResult {
    val stripPrefixAncestors = setOf(stripPrefix)
    if (leftovers.none { it.isUnder(stripPrefixAncestors) }) return this
    return MergeResult(
      merged = merged + MergedPath(
        path = stripPrefix,
        excluded = stripPrefix.walk()
          .filter { it !in sourcesSet && it !in leftovers }
          .toList(),
      ),
      leftovers = leftovers.filterNotTo(mutableSetOf()) { it.isUnder(stripPrefixAncestors) },
    )
  }

  private fun extractStripPrefixes(target: RawBuildTarget): Set<Path> {
    // Currently, for every rule set we use shared heuristic that should satisfy most of the cases.
    // Each language rule set has its own heuristic for a default strip prefix that might slightly differ from what we do.
    // In the case of resources highlighting issues (false positive or negative),
    // it might be worth investigating if the correct strip prefix is selected.
    // If not, maybe heuristic should be somehow expanded or split per language.

    // This is how rules_kotlin handle the default scenario
    // https://github.com/bazelbuild/rules_kotlin/blame/0f0ec19b4339f9dde4183d938e6acb465bc5e45a/kotlin/internal/jvm/compile.bzl#L154
    // This is how rules_scala handle the default scenario
    // https://github.com/bazel-contrib/rules_scala/blob/18518778580cc753c787d017923112fadf29a5e9/scala/private/resources.bzl#L32
    // This is how rules_java handle the default scenario
    // https://github.com/bazelbuild/bazel/blob/08e077e7a46b5f2137cf3335104219133f8d997f/src/main/java/com/google/devtools/build/lib/bazel/rules/java/BazelJavaSemantics.java#L63

    val stripPrefix = extractJvmBuildTarget(target)?.resourceStripPrefix
    if (stripPrefix != null) return setOf(stripPrefix)
    val srcWithResourcesGrandchild = target.resources.findSrcWithResourcesGrandchildPrefixes()
    val javaSegmentPath = target.resources.findPrefixBySegment(Path("java"))
    if (target.data is KotlinBuildTarget) {
      val kotlinSegmentPath = target.resources.findPrefixBySegment(Path("kotlin"))
      return srcWithResourcesGrandchild + javaSegmentPath + kotlinSegmentPath
    }
    return srcWithResourcesGrandchild + javaSegmentPath
  }

  private fun BuildTarget.inferRootType(): SourceRootTypeId =
    if (kind.ruleType == RuleType.TEST) JAVA_TEST_RESOURCE_ROOT_TYPE else JAVA_RESOURCE_ROOT_TYPE

  private data class MergeResult(
    val merged: List<MergedPath> = emptyList(),
    val leftovers: Set<Path> = emptySet(),
  )

  private data class MergedPath(
    val path: Path,
    val excluded: List<Path>,
  )

  private fun List<Path>.findSrcWithResourcesGrandchildPrefixes() = mapNotNullTo(mutableSetOf()) {
    val segments = it.toList()
    for (i in 0..<segments.size - 2) {
      if (segments.getOrNull(i)?.name == "src" && segments.getOrNull(i + 2)?.name == "resources") {
        return@mapNotNullTo it.root?.resolve(it.subpath(0, i + 3))
      }
    }
    null
  }

  private fun List<Path>.findPrefixBySegment(path: Path) = mapNotNullTo(mutableSetOf()) {
    it.subpathBeforeInclusiveOrNull(path)
  }

  private fun Path.subpathBeforeInclusiveOrNull(element: Path): Path? {
    val index = this.indexOf(element).takeIf { it >= 0 } ?: return null
    return this.root?.resolve(this.subpath(0, index + 1))
  }
}
