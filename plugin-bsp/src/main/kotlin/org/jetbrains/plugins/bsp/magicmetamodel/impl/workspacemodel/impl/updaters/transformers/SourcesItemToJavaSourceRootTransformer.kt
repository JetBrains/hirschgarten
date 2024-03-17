package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaSourceRoot
import java.net.URI
import java.nio.file.Path

internal data class BuildTargetAndSourceItem(
  val buildTarget: BuildTarget,
  val sourcesItem: SourcesItem,
)

internal class SourcesItemToJavaSourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, JavaSourceRoot> {
  private val sourceRootType = "java-source"
  private val testSourceRootType = "java-test"

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRootsAsURIs(inputEntity.sourcesItem)
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sourcesItem.sources)
      .map { toJavaSourceRoot(it, sourceRoots, rootType) }
      .filter { it.sourcePath.isPathInProjectBasePath(projectBasePath) }
  }

  private fun getSourceRootsAsURIs(sourcesItem: SourcesItem): List<URI> =
    sourcesItem.roots.orEmpty().map { URI.create(it) }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun toJavaSourceRoot(
    sourceRoot: SourceRoot,
    sourceRoots: List<URI>,
    rootType: String,
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
    val packageDetails = JavaSourcePackageDetails(
      sourceURI = sourceRoot.sourcePath.let { if (sourceRoot.isFile) it.parent else it }.toUri(),
      sourceRoots = sourceRoots,
    )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}
