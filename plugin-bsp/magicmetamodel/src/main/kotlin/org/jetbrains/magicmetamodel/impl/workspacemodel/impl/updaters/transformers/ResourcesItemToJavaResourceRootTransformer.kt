package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot
import java.nio.file.Path

internal class ResourcesItemToJavaResourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, JavaResourceRoot> {

  override fun transform(inputEntity: ResourcesItem): List<JavaResourceRoot> =
    inputEntity.resources
      .map { toJavaResourceRoot(it) }
      .filter { it.resourcePath.isPathInProjectBasePath(projectBasePath) }
      .distinct()

  private fun toJavaResourceRoot(resourcePath: String) =
    JavaResourceRoot(
      resourcePath = RawUriToDirectoryPathTransformer.transform(resourcePath)
    )
}

internal fun Path.isPathInProjectBasePath(projectBasePath: Path) =
  this.toAbsolutePath().startsWith(projectBasePath.toAbsolutePath())
