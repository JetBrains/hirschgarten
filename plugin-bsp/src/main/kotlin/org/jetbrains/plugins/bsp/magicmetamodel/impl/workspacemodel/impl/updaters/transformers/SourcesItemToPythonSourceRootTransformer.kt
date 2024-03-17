package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericSourceRoot
import java.nio.file.Path

internal class SourcesItemToPythonSourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, GenericSourceRoot> {
  private val sourceRootType = "python-source"
  private val testSourceRootType = "python-test"

  override fun transform(inputEntities: List<BuildTargetAndSourceItem>): List<GenericSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)

    return allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRoots) }
  }

  private fun isNotAChildOfAnySourceDir(
    sourceRoot: GenericSourceRoot,
    allSourceRoots: List<GenericSourceRoot>,
  ): Boolean =
    allSourceRoots.none { sourceRoot.sourcePath.parent.startsWith(it.sourcePath) }

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<GenericSourceRoot> {
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer
      .transform(inputEntity.sourcesItem.sources)
      .map { toPythonSourceRoot(it, rootType) }
      .filter { it.sourcePath.isPathInProjectBasePath(projectBasePath) }
  }

  private fun inferRootType(buildTarget: BuildTarget): String =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun toPythonSourceRoot(
    sourceRoot: SourceRoot,
    rootType: String,
  ): GenericSourceRoot {
    return GenericSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      rootType = rootType,
    )
  }
}
