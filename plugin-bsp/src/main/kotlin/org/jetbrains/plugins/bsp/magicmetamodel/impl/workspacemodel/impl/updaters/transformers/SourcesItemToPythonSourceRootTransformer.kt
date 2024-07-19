package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.BuildTarget
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import java.nio.file.Path
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.GenericSourceRoot

internal class SourcesItemToPythonSourceRootTransformer :
    WorkspaceModelEntityPartitionTransformer<BuildTargetAndSourceItem, GenericSourceRoot> {
  private val sourceRootType = SourceRootTypeId("python-source")
  private val testSourceRootType = SourceRootTypeId("python-test")

  override fun transform(inputEntities: List<BuildTargetAndSourceItem>): List<GenericSourceRoot> {
    val allSourceRoots = super.transform(inputEntities)
    val allSourceRootsPaths = allSourceRoots.mapTo(hashSetOf()) { it.sourcePath }

    return allSourceRoots.filter { isNotAChildOfAnySourceDir(it, allSourceRootsPaths) }
  }

  private fun isNotAChildOfAnySourceDir(
      sourceRoot: GenericSourceRoot,
      allSourceRootsPaths: Set<Path>,
  ): Boolean {
    var sourcePathParent = sourceRoot.sourcePath.parent
    while (sourcePathParent != null) {
      if (sourcePathParent in allSourceRootsPaths) return false
      sourcePathParent = sourcePathParent.parent
    }
    return true
  }

  override fun transform(inputEntity: BuildTargetAndSourceItem): List<GenericSourceRoot> {
    val rootType = inferRootType(inputEntity.buildTarget)

    return SourceItemToSourceRootTransformer.transform(inputEntity.sourcesItem.sources).map {
      toPythonSourceRoot(it, rootType)
    }
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
