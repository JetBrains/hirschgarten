package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.BuildTarget

val JAVA_SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")
val JAVA_TEST_SOURCE_ROOT_TYPE = SourceRootTypeId("java-test")
val JAVA_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-resource")
val JAVA_TEST_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-test-resource")

class SourcesItemToJavaSourceRootTransformer : WorkspaceModelEntityPartitionTransformer<BuildTarget, JavaSourceRoot> {
  override fun transform(inputEntity: BuildTarget): List<JavaSourceRoot> {
    val rootType = inferRootType(inputEntity)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sources)
      .map { toJavaSourceRoot(it, rootType) }
  }

  private fun inferRootType(buildTarget: BuildTarget): SourceRootTypeId =
    if (buildTarget.kind.ruleType == RuleType.TEST) JAVA_TEST_SOURCE_ROOT_TYPE else JAVA_SOURCE_ROOT_TYPE

  private fun toJavaSourceRoot(sourceRoot: SourceRoot, rootType: SourceRootTypeId): JavaSourceRoot =
    JavaSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      packagePrefix = sourceRoot.jvmPackagePrefix ?: "",
      rootType = rootType,
    )
}
