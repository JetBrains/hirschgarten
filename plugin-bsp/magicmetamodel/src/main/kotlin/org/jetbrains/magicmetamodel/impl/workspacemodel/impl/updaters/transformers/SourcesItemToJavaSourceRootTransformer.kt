package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.SourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.SourceRoot
import java.net.URI

internal object SourcesItemToJavaSourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<SourcesItem, JavaSourceRoot> {

  override fun transform(inputEntity: SourcesItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRootsAsURIs(inputEntity)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sources)
      .map { toJavaSourceRoot(it, sourceRoots) }
  }

  private fun getSourceRootsAsURIs(sourcesItem: SourcesItem): List<URI> =
    // TODO?
    (sourcesItem.roots ?: emptyList()).map(URI::create)

  private fun toJavaSourceRoot(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRoot {
    val packagePrefix = calculatePackagePrefix(sourceRoot, sourceRoots)

    return JavaSourceRoot(
      sourceDir = sourceRoot.sourceDir,
      generated = sourceRoot.generated,
      packagePrefix = packagePrefix.packagePrefix
    )
  }

  private fun calculatePackagePrefix(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRootPackagePrefix {
    val packageDetails = JavaSourcePackageDetails(
      sourceDir = sourceRoot.sourceDir.toUri(),
      sourceRoots = sourceRoots
    )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}
