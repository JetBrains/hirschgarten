package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path
import kotlin.io.path.extension

abstract class WorkspaceModelEntity

data class ContentRoot(val path: Path) : WorkspaceModelEntity()

interface ResourceRootEntity

interface EntityDependency

data class GenericSourceRoot(val sourcePath: Path, val rootType: SourceRootTypeId) : WorkspaceModelEntity()

data class ResourceRoot(val resourcePath: Path, val rootType: SourceRootTypeId) :
  WorkspaceModelEntity(),
  ResourceRootEntity

data class Library(
  val displayName: String,
  val iJars: List<Path> = listOf(),
  val sourceJars: List<Path> = listOf(),
  val classJars: List<Path> = listOf(),
  val mavenCoordinates: MavenCoordinates? = null,
) : WorkspaceModelEntity(),
  ResourceRootEntity {
  companion object {
    fun formatJarString(jar: Path): String =
      if (jar.extension == "jar") {
        "jar://$jar!/"
      } else {
        // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
        "file://$jar"
      }
  }
}

data class IntermediateModuleDependency(val moduleName: String) :
  WorkspaceModelEntity(),
  EntityDependency

data class IntermediateLibraryDependency(val libraryName: String, val isProjectLevelLibrary: Boolean = false) :
  WorkspaceModelEntity(),
  EntityDependency

/**
This class holds basic module data that are not language-specific
 */
data class GenericModuleInfo(
  val name: String,
  val type: ModuleTypeId,
  val modulesDependencies: List<IntermediateModuleDependency>,
  val librariesDependencies: List<IntermediateLibraryDependency>,
  val kind: TargetKind,
  val languageIds: LanguageIds = listOf(),
  val associates: List<IntermediateModuleDependency> = listOf(),
  val isDummy: Boolean = false,
  val isLibraryModule: Boolean = false,
) : WorkspaceModelEntity() {
  internal constructor(
    name: String,
    type: ModuleTypeId,
    modulesDependencies: List<IntermediateModuleDependency>,
    librariesDependencies: List<IntermediateLibraryDependency>,
    kind: TargetKind,
    languageIds: Map<String, String>,
    associates: List<IntermediateModuleDependency> = listOf(),
  ) : this(
    name,
    type,
    modulesDependencies,
    librariesDependencies,
    kind,
    languageIdsFromMap(languageIds),
    associates,
  )

  val languageIdsAsSingleEntryMap: Map<String, String>
    get() =
      languageIds
        .takeUnless { it.isEmpty() }
        ?.let {
          mapOf(LANGUAGE_IDS to it.joinToString(LANGUAGE_IDS_SEPARATOR))
        }.orEmpty()

  private companion object {
    const val LANGUAGE_IDS = "languageIds"
    const val LANGUAGE_IDS_SEPARATOR = ","

    fun languageIdsFromMap(map: Map<String, String>): List<String> = map[LANGUAGE_IDS]?.split(LANGUAGE_IDS_SEPARATOR).orEmpty()
  }
}

interface Module {
  fun getModuleName(): String
}

data class CompiledSourceCodeInsideJarExclude(val relativePathsInsideJarToExclude: Set<String>, val namesInsideJarToExclude: Set<String>) :
  WorkspaceModelEntity()
