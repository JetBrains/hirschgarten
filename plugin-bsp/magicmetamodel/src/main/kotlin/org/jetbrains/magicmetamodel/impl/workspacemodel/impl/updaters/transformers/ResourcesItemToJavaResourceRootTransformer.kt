package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.ResourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.JavaResourceRoot

internal object ResourcesItemToJavaResourceRootTransformer :
  WorkspaceModelEntityPartitionTransformer<ResourcesItem, JavaResourceRoot> {

  override fun transform(inputEntity: ResourcesItem): List<JavaResourceRoot> =
    inputEntity.resources
      .map(this::toJavaResourceRoot)
      .distinct()

  private fun toJavaResourceRoot(resourcePath: String) =
    JavaResourceRoot(
      resourcePath = RawUriToDirectoryPathTransformer.transform(resourcePath)
    )
}
