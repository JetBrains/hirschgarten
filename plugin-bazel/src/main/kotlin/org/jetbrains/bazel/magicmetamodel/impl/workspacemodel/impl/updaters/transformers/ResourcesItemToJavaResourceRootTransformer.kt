package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.utils.isUnder
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.ResourceItem
import org.jetbrains.bsp.protocol.UltimateBuildTarget
import kotlin.io.path.walk

class ResourcesItemToJavaResourceRootTransformer(
  private val targetsMap: Map<Label, BuildTarget>,
) : WorkspaceModelEntityPartitionTransformer<RawBuildTarget, ResourceRoot> {
  override fun transform(inputEntity: RawBuildTarget): List<ResourceRoot> {
    val rootType = inputEntity.inferRootType()
    return inputEntity.resources
      .flatMap { toJavaResourceRoot(it, rootType) }
      .distinct()
  }

  private fun toJavaResourceRoot(resource: ResourceItem, rootType: SourceRootTypeId): List<ResourceRoot> = when (resource) {
    is ResourceItem.File -> listOf(ResourceRoot(resource.path, rootType))
    is ResourceItem.Target -> targetsMap[resource.label].targetToResourceRoots(rootType)
  }

  private fun BuildTarget?.targetToResourceRoots(rootType: SourceRootTypeId): List<ResourceRoot> {
    this ?: return emptyList()
    val ultimate = this.data as? UltimateBuildTarget ?: return emptyList()
    val stripPrefix = ultimate.stripPrefix ?: return ultimate.resources.map { ResourceRoot(it, rootType, ultimate.addPrefix) }
    val stripPrefixAncestors = setOf(stripPrefix)
    val resourcesSet = ultimate.resources.toSet()
    return ultimate.resources
      .filterNot { it.isUnder(stripPrefixAncestors) }
      .map { ResourceRoot(it, rootType) }
      .plusElement(
        ResourceRoot(
          resourcePath = stripPrefix,
          rootType = rootType,
          relativePath = ultimate.addPrefix,
          excluded = stripPrefix.walk()
            .filter { it !in resourcesSet }
            .toList(),
        ),
      )
  }


  private fun BuildTarget.inferRootType(): SourceRootTypeId =
    if (kind.ruleType == RuleType.TEST) JAVA_TEST_RESOURCE_ROOT_TYPE else JAVA_RESOURCE_ROOT_TYPE
}
