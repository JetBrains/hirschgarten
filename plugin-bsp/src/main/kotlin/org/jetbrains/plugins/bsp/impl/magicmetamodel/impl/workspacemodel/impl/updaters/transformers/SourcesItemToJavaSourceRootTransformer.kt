package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import org.jetbrains.plugins.bsp.workspacemodel.entities.JavaSourceRoot
import java.net.URI
import kotlin.io.path.toPath

internal data class BuildTargetAndSourceItem(val buildTarget: BuildTarget, val sourcesItem: SourcesItem)

internal class SourcesItemToJavaSourceRootTransformer(private val workspaceModelEntitiesFolderMarker: Boolean = false) :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, JavaSourceRoot> {
  private val sourceRootType = SourceRootTypeId("java-source")
  private val testSourceRootType = SourceRootTypeId("java-test")

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRootsAsURIs(inputEntity.sourcesItem)
    val rootType = inferRootType(inputEntity.buildTarget)

    return if (workspaceModelEntitiesFolderMarker) {
      sourceRoots.map { it.toJavaSourceRoot(rootType) }
    } else {
      SourceItemToSourceRootTransformer.transform(inputEntity.sourcesItem.sources).map { toJavaSourceRoot(it, sourceRoots, rootType) }
    }
  }

  private fun getSourceRootsAsURIs(sourcesItem: SourcesItem): List<URI> = sourcesItem.roots.orEmpty().map { it.safeCastToURI() }

  private fun inferRootType(buildTarget: BuildTarget): SourceRootTypeId =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun URI.toJavaSourceRoot(rootType: SourceRootTypeId): JavaSourceRoot =
    JavaSourceRoot(
      sourcePath = toPath(),
      generated = false,
      packagePrefix = "",
      rootType = rootType,
    )

  private fun toJavaSourceRoot(
    sourceRoot: SourceRoot,
    sourceRoots: List<URI>,
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

  private fun calculatePackagePrefix(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRootPackagePrefix {
    val packagePrefixFromData = sourceRoot.additionalData?.jvmPackagePrefix
    if (packagePrefixFromData != null) return JavaSourceRootPackagePrefix(packagePrefixFromData)
    val packageDetails =
      JavaSourcePackageDetails(
        sourceURI = sourceRoot.sourcePath.let { if (sourceRoot.isFile) it.parent else it }.toUri(),
        sourceRoots = sourceRoots,
      )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}
