package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourcesItem

internal data class BuildTargetAndSourceItem(val buildTarget: BuildTarget, val sourcesItem: SourcesItem)

internal val JAVA_SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")
internal val JAVA_TEST_SOURCE_ROOT_TYPE = SourceRootTypeId("java-test")
internal val JAVA_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-resource")
internal val JAVA_TEST_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-test-resource")

internal class SourcesItemToJavaSourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, JavaSourceRoot> {
  override fun transform(inputEntity: BuildTargetAndSourceItem): List<JavaSourceRoot> {
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sourcesItem.sources)
      .map { toJavaSourceRoot(it, rootType) }
  }

  private fun inferRootType(buildTarget: BuildTarget): SourceRootTypeId =
    if (buildTarget.tags.contains("test")) JAVA_TEST_SOURCE_ROOT_TYPE else JAVA_SOURCE_ROOT_TYPE

  private fun toJavaSourceRoot(sourceRoot: SourceRoot, rootType: SourceRootTypeId): JavaSourceRoot =
    JavaSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      packagePrefix = sourceRoot.jvmPackagePrefix ?: "",
      rootType = rootType,
    )
}
