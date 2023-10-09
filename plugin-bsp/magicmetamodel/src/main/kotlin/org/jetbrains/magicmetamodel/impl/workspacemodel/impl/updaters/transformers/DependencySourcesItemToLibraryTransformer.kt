package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import java.net.URI
import kotlin.io.path.toPath

internal data class DependencySourcesAndJavacOptions(
  val dependencySources: DependencySourcesItem,
  val javacOptions: JavacOptionsItem?,
)

internal object DependencySourcesItemToLibraryTransformer :
  WorkspaceModelEntityPartitionTransformer<DependencySourcesAndJavacOptions, Library> {
  override fun transform(inputEntity: DependencySourcesAndJavacOptions): List<Library> {
    val librariesWithUnused = toLibraryFromClassesAndSourcesAndReturnUnusedClassesAndSources(inputEntity)
    val unusedClassJars = librariesWithUnused.second
    val unusedSourceJars = librariesWithUnused.third

    return librariesWithUnused.first + toLibraryFromClassJars(unusedClassJars) + toLibraryFromSourceJars(
      unusedSourceJars,
    )
  }

  private fun toLibraryFromClassesAndSourcesAndReturnUnusedClassesAndSources(
    inputEntity: DependencySourcesAndJavacOptions,
  ): Triple<List<Library>, List<String>, List<String>> {
    val unusedDependencySources = inputEntity.dependencySources.sources.toMutableSet()
    val unusedClasses = inputEntity.javacOptions?.classpath.orEmpty().toMutableSet()

    val result = inputEntity.javacOptions?.classpath.orEmpty()
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

    return Triple(result, unusedClasses.toList(), unusedDependencySources.toList())
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
    "jar://${removeUriFilePrefix(dependencyUri)}!/"

  private fun removeUriFilePrefix(uri: String): String =
    URI.create(uri).toPath().toString()
}
