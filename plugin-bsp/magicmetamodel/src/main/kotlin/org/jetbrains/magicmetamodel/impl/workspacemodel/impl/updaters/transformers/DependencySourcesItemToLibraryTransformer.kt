package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import java.net.URI
import kotlin.io.path.toPath

internal data class DependencySourcesAndJvmClassPaths(
  val dependencySources: DependencySourcesItem,
  val jvmClassPaths: List<String>,
)

internal object DependencySourcesItemToLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJvmClassPaths, Library> {
  private data class LibrariesPreprocessedResult(
    val libraries: List<Library>,
    val unusedClassJars: List<String>,
    val unusedSourceJars: List<String>,
  )

  override fun transform(inputEntity: DependencySourcesAndJvmClassPaths): List<Library> {
    val librariesPreprocessedResult = preprocessLibraries(inputEntity)
    val unusedClassJars = librariesPreprocessedResult.unusedClassJars
    val unusedSourceJars = librariesPreprocessedResult.unusedSourceJars

    return librariesPreprocessedResult.libraries + toLibraryFromClassJars(unusedClassJars) + toLibraryFromSourceJars(
      unusedSourceJars,
    )
  }

  private fun preprocessLibraries(
    inputEntity: DependencySourcesAndJvmClassPaths,
  ): LibrariesPreprocessedResult {
    val unusedDependencySources = inputEntity.dependencySources.sources.toMutableSet()
    val allClasses = inputEntity.jvmClassPaths
    val unusedClasses = allClasses.toMutableSet()

    val result = allClasses
      .mapNotNull { classJar ->
        findSourceJarForClassJar(classJar, unusedDependencySources)?.let { sourceJar ->
          unusedDependencySources.remove(sourceJar)
          unusedClasses.remove(classJar)

          Library(
            displayName = calculateDisplayName(classJar),
            classJars = listOf(toJarString(classJar)),
            sourceJars = listOf(toJarString(sourceJar)),
          )
        }
      }

    return LibrariesPreprocessedResult(
      libraries = result,
      unusedClassJars = unusedClasses.toList(),
      unusedSourceJars = unusedDependencySources.toList()
    )
  }

  private fun toLibraryFromClassJars(classJars: List<String>) =
    classJars.map {
      Library(
        displayName = calculateDisplayName(it),
        classJars = listOf(toJarString(it)),
      )
    }

  private fun toLibraryFromSourceJars(sourceJars: List<String>) =
    sourceJars.map {
      Library(
        displayName = calculateDisplayName(it),
        sourceJars = listOf(toJarString(it)),
      )
    }

  private fun findSourceJarForClassJar(classJar: String, sourceJars: Set<String>): String? =
    sourceJars.find { removeSourcesSuffix(it).startsWith(classJar) }

  private fun calculateDisplayName(uri: String): String = "BSP: $uri"

  private fun removeSourcesSuffix(path: String): String =
    path.replace("-sources", "")

  private fun toJarString(dependencyUri: String): String =
    Library.formatJarString(removeUriFilePrefix(dependencyUri))

  private fun removeUriFilePrefix(uri: String): String =
    URI.create(uri).toPath().toString()
}
