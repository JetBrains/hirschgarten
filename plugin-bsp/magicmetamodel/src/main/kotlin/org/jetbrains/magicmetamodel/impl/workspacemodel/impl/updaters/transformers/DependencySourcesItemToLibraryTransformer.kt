package org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.transformers

import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.Library
import java.net.URI
import kotlin.io.path.nameWithoutExtension
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
      unusedSourceJars
    )
  }

  private fun toLibraryFromClassesAndSourcesAndReturnUnusedClassesAndSources(inputEntity: DependencySourcesAndJavacOptions): Triple<List<Library>, List<String>, List<String>> {
    val unusedDependencySources = inputEntity.dependencySources.sources.toMutableSet()
    val unusedClasses = inputEntity.javacOptions?.classpath.orEmpty().toMutableSet()

    val result = inputEntity.javacOptions?.classpath.orEmpty()
      .mapNotNull { classJar ->
        findSourceJarForClassJar(classJar, unusedDependencySources)?.let { sourceJar ->
          unusedDependencySources.remove(sourceJar)
          unusedClasses.remove(classJar)

          Library(
            displayName = calculateDisplayName(classJar),
            classesJar = toJarString(classJar),
            sourcesJar = toJarString(sourceJar),
          )
        }
      }

    return Triple(result, unusedClasses.toList(), unusedDependencySources.toList())
  }

  private fun toLibraryFromClassJars(classJars: List<String>) =
    classJars.map {
      Library(
        displayName = calculateDisplayName(it),
        classesJar = toJarString(it),
        sourcesJar = null,
      )
    }

  private fun toLibraryFromSourceJars(classJars: List<String>) =
    classJars.map {
      Library(
        displayName = calculateDisplayName(it),
        classesJar = null,
        sourcesJar = toJarString(it),
      )
    }

  private fun findSourceJarForClassJar(classJar: String, sourceJars: Set<String>): String? =
    sourceJars.find { removeSourcesSuffix(it).startsWith(classJar) }

  /**
   * When generating the display name, check first if a uri comes from maven repository, if it does, return
   * the trimmed name, if not (internal dependencies), return the whole uri path for uniqueness.
   * */
  private fun calculateDisplayName(uri: String): String {
    val depName = URI.create(uri).toPath().nameWithoutExtension
    return if (isUriMaven(uri)) "BSP: $depName" else "BSP: $uri"
  }

  /**
   * Check if a dependency uri comes from maven repository.
   * */
  private fun isUriMaven(uri: String): Boolean = uri.contains("repo.maven.apache.org")

  private fun removeSourcesSuffix(path: String): String =
    path.replace("-sources", "")

  private fun toJarString(dependencyUri: String): String =
    "jar://${removeUriFilePrefix(dependencyUri)}!/"

  private fun removeUriFilePrefix(uri: String): String =
    URI.create(uri).toPath().toString()
}
