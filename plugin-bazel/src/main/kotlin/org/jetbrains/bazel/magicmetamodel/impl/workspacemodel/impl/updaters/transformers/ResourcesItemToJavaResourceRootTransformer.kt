package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.ResourcesItem
import kotlin.io.path.toPath

internal data class BuildTargetAndResourcesItem(val buildTarget: BuildTarget, val resourcesItem: ResourcesItem)

internal class ResourcesItemToJavaResourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndResourcesItem, ResourceRoot> {
  override fun transform(inputEntity: BuildTargetAndResourcesItem): List<ResourceRoot> {
    val rootType = inputEntity.buildTarget.inferRootType()
    return inputEntity.resourcesItem.resources
      .map { toJavaResourceRoot(it, rootType) }
      .distinct()
  }

  private fun toJavaResourceRoot(resourcePath: String, rootType: SourceRootTypeId) =
    ResourceRoot(
      resourcePath = resourcePath.safeCastToURI().toPath(),
      rootType = rootType,
    )

  private fun BuildTarget.inferRootType(): SourceRootTypeId =
    if (tags.contains("test")) JAVA_TEST_RESOURCE_ROOT_TYPE else JAVA_RESOURCE_ROOT_TYPE
}
