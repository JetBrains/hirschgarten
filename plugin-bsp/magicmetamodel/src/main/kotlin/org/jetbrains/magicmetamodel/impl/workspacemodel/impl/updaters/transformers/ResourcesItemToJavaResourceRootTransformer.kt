package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.ResourceRoot
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.toPath

internal class ResourcesItemToJavaResourceRootTransformer(private val projectBasePath: Path) :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, ResourceRoot> {
  override fun transform(inputEntity: ResourcesItem): List<ResourceRoot> =
    inputEntity.resources
      .map { toJavaResourceRoot(it) }
      .filter { it.resourcePath.isPathInProjectBasePath(projectBasePath) }
      .distinct()

  private fun toJavaResourceRoot(resourcePath: String) =
    ResourceRoot(
      resourcePath = URI.create(resourcePath).toPath(),
    )
}

internal fun Path.isPathInProjectBasePath(projectBasePath: Path) =
  this.toAbsolutePath().startsWith(projectBasePath.toAbsolutePath())
