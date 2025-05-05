package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.workspacemodel.entities.ResourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path

internal class ResourcesItemToJavaResourceRootTransformer : WorkspaceModelEntityPartitionTransformer<BuildTarget, ResourceRoot> {
  override fun transform(inputEntity: BuildTarget): List<ResourceRoot> {
    val rootType = inputEntity.inferRootType()
    return inputEntity.resources
      .map { toJavaResourceRoot(it, rootType) }
      .distinct()
  }

  private fun toJavaResourceRoot(resourcePath: Path, rootType: SourceRootTypeId) =
    ResourceRoot(
      resourcePath = resourcePath,
      rootType = rootType,
    )

  private fun BuildTarget.inferRootType(): SourceRootTypeId =
    if (kind.ruleType == RuleType.TEST) JAVA_TEST_RESOURCE_ROOT_TYPE else JAVA_RESOURCE_ROOT_TYPE
}
