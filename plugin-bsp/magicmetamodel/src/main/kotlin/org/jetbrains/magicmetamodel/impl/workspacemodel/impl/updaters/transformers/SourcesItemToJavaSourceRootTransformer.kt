package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourcesItem
import com.intellij.util.io.isAncestor
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaSourceRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.SourceRoot
import java.net.URI
import java.nio.file.Path

internal data class BuildTargetAndSourceItem(
  val buildTarget: BuildTarget,
  val sourcesItem: SourcesItem,
)

internal class SourcesItemToJavaSourceRootTransformer(private val projectBasePath: Path):
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, JavaSourceRoot> {

  private val sourceRootType = "java-source"
  private val testSourceRootType = "java-test"

  override fun transform(inputEntities: List<BuildTargetAndSourceItem>): List<JavaSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)

    return allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRoots) }
  }

  private fun isNotAChildOfAnySourceDir(sourceRoot: JavaSourceRoot, allSourceRoots: List<JavaSourceRoot>): Boolean =
    allSourceRoots.none { it.sourcePath.isAncestor(sourceRoot.sourcePath.parent) }

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<JavaSourceRoot> {
    val sourceRoots = getSourceRootsAsURIs(inputEntity.sourcesItem)
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sourcesItem.sources)
      .map { toJavaSourceRoot(it, sourceRoots, rootType, inputEntity.buildTarget.id) }
      .filter { it.sourcePath.isPathInProjectBasePath(projectBasePath) }
  }

  private fun getSourceRootsAsURIs(sourcesItem: SourcesItem): List<URI> =
    // TODO?
    sourcesItem.roots.orEmpty().map { URI.create(it) }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun toJavaSourceRoot(
    sourceRoot: SourceRoot,
    sourceRoots: List<URI>,
    rootType: String,
    targetId: BuildTargetIdentifier
  ): JavaSourceRoot {
    val packagePrefix = calculatePackagePrefix(sourceRoot, sourceRoots)

    return JavaSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      generated = sourceRoot.generated,
      packagePrefix = packagePrefix.packagePrefix,
      rootType = rootType,
      targetId = targetId,
    )
  }

  private fun calculatePackagePrefix(sourceRoot: SourceRoot, sourceRoots: List<URI>): JavaSourceRootPackagePrefix {
    val packageDetails = JavaSourcePackageDetails(
      sourceURI = sourceRoot.sourcePath.let { if (sourceRoot.isFile) it.parent else it }.toUri(),
      sourceRoots = sourceRoots
    )

    return JavaSourcePackageDetailsToJavaSourceRootPackagePrefixTransformer.transform(packageDetails)
  }
}
