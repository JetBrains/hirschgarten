package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericSourceRoot

internal class SourcesItemToPythonSourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, GenericSourceRoot> {
  private val sourceRootType = SourceRootTypeId("python-source")
  private val testSourceRootType = SourceRootTypeId("python-test")

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
  }

  private fun inferRootType(buildTarget: BuildTarget): SourceRootTypeId =
    if (buildTarget.tags.contains("test")) testSourceRootType else sourceRootType

  private fun toPythonSourceRoot(
    sourceRoot: SourceRoot,
    rootType: SourceRootTypeId,
  ): GenericSourceRoot {
    return GenericSourceRoot(
      sourcePath = sourceRoot.sourcePath,
      rootType = rootType,
    )
  }
}
