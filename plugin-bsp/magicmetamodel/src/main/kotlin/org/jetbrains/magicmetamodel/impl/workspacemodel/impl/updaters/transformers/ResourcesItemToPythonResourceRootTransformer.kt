package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.PythonResourceRoot
import java.nio.file.Path

internal class ResourcesItemToPythonResourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, PythonResourceRoot> {

  override fun transform(inputEntity: ResourcesItem): List<PythonResourceRoot> =
    inputEntity.resources
      .map(this::toPythonResourceRoot)
      .filter { it.resourcePath.isPathInProjectBasePath(projectBasePath) }
      .distinct()

  private fun toPythonResourceRoot(resourcePath: String) =
    PythonResourceRoot(
      resourcePath = RawUriToDirectoryPathTransformer.transform(resourcePath)
    )
}
