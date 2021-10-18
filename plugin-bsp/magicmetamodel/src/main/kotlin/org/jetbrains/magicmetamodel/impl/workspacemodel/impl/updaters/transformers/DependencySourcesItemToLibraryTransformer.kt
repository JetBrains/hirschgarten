package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import java.net.URI
import kotlin.io.path.toPath

internal object DependencySourcesItemToLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesItem, Library> {

  override fun transform(inputEntity: DependencySourcesItem): List<Library> {
    return inputEntity.sources.map(this::toLibrary)
  }

  @Suppress("ForbiddenComment")
  // TODO: name and version should be extracted, maybe BSP prefix as well?
  private fun toLibrary(dependencyUri: String): Library =
    Library(
      displayName = dependencyUri,
      sourcesJar = toSourcesJar(dependencyUri),
      classesJar = toClassesJar(dependencyUri),
    )

  private fun toSourcesJar(dependencyUri: String): String =
    "jar://${removeUriFilePrefix(dependencyUri)}!/"

  private fun toClassesJar(dependencyUri: String): String {
    val dependencyPath = removeUriFilePrefix(dependencyUri)
    val dependencyClassesPath = dependencyPath.replace("-sources\\.jar\$".toRegex(), ".jar")

    return "jar://$dependencyClassesPath!/"
  }

  private fun removeUriFilePrefix(dependencyUri: String): String =
    URI.create(dependencyUri).toPath().toString()
}
