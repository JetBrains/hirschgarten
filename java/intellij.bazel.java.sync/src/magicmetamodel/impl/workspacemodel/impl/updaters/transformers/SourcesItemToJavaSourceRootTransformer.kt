package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.openapi.project.Project
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.languages.projectview.testSources
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget

@ApiStatus.Internal
val JAVA_SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")
@ApiStatus.Internal
val JAVA_TEST_SOURCE_ROOT_TYPE = SourceRootTypeId("java-test")
@ApiStatus.Internal
val JAVA_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-resource")
internal val JAVA_TEST_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-test-resource")

@ApiStatus.Internal
class SourcesItemToJavaSourceRootTransformer(project: Project) : WorkspaceModelEntityPartitionTransformer<RawBuildTarget, JavaSourceRoot> {
  private val testSourcesGlob = ProjectViewGlobSet(project.rootDir.toNioPath(), project.projectView().testSources)

  override fun transform(inputEntity: RawBuildTarget): List<JavaSourceRoot> {
    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sources)
      .map { toJavaSourceRoot(it, inputEntity) }
  }

  private fun toJavaSourceRoot(sourceRoot: SourceRoot, buildTarget: BuildTarget): JavaSourceRoot {
    val rootType = if (buildTarget.kind.ruleType == RuleType.TEST || testSourcesGlob.matches(sourceRoot.sourcePath)) {
      JAVA_TEST_SOURCE_ROOT_TYPE
    }
    else {
      JAVA_SOURCE_ROOT_TYPE
    }
    return JavaSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      packagePrefix = sourceRoot.jvmPackagePrefix ?: "",
      rootType = rootType,
    )
  }
}
