package org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.plugins.bsp.impl.magicmetamodel.impl.workspacemodel.PythonLibrary

internal object DependencySourcesItemToPythonLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, PythonLibrary> {
  override fun transform(inputEntity: DependencySourcesItem): List<PythonLibrary> =
    inputEntity.sources.mapNotNull { PythonLibrary(listOf(it)) }
}
