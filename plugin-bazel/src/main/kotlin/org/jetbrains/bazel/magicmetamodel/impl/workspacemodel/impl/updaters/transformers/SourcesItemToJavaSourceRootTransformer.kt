package org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.JavaSourceRoot
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.SourcesItem
import java.nio.file.Path
import kotlin.io.path.toPath

internal data class BuildTargetAndSourceItem(val buildTarget: BuildTarget, val sourcesItem: SourcesItem)

internal val JAVA_SOURCE_ROOT_TYPE = SourceRootTypeId("java-source")
internal val JAVA_TEST_SOURCE_ROOT_TYPE = SourceRootTypeId("java-test")
internal val JAVA_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-resource")
internal val JAVA_TEST_RESOURCE_ROOT_TYPE = SourceRootTypeId("java-test-resource")

internal class SourcesItemToJavaSourceRootTransformer(private val workspaceModelEntitiesFolderMarker: Boolean = false) :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, JavaSourceRoot> {
  override fun transform(inputEntity: BuildTargetAndSourceItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRoots(inputEntity.sourcesItem)
    val rootType = inferRootType(inputEntity.buildTarget)

    return if (workspaceModelEntitiesFolderMarker) {
      sourceRoots.map { it.toJavaSourceRoot(rootType) }
    } else {
      SourceItemToSourceRootTransformer.transform(inputEntity.sourcesItem.sources).map { toJavaSourceRoot(it, sourceRoots, rootType) }
    }
  }

  private fun getSourceRoots(sourcesItem: SourcesItem): Set<Path> =
    sourcesItem.roots
      .map { it.safeCastToURI().toPath() }
      .toSet()

  private fun inferRootType(buildTarget: BuildTarget): SourceRootTypeId =
    if (buildTarget.tags.contains("test")) JAVA_TEST_SOURCE_ROOT_TYPE else JAVA_SOURCE_ROOT_TYPE

  private fun Path.toJavaSourceRoot(rootType: SourceRootTypeId): JavaSourceRoot =
    JavaSourceRoot(
      sourcePath = this,
      generated = false,
      packagePrefix = "",
      rootType = rootType,
    )

  private fun toJavaSourceRoot(
    sourceRoot: SourceRoot,
    sourceRoots: Set<Path>,
    rootType: SourceRootTypeId,
  ): JavaSourceRoot {
    val packagePrefix = calculatePackagePrefix(sourceRoot, sourceRoots)

    return JavaSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      packagePrefix = packagePrefix.packagePrefix,
      rootType = rootType,
    )
  }

  private fun calculatePackagePrefix(sourceRoot: SourceRoot, sourceRoots: Set<Path>): JavaSourceRootPackagePrefix {
    val packagePrefixFromData = sourceRoot.jvmPackagePrefix
    if (packagePrefixFromData != null) return JavaSourceRootPackagePrefix(packagePrefixFromData)
    val packageDetails =
      JavaSourcePackageDetails(
        source = sourceRoot.sourcePath.let { if (sourceRoot.isFile) it.parent else it },
        sourceRoots = sourceRoots,
      )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}
