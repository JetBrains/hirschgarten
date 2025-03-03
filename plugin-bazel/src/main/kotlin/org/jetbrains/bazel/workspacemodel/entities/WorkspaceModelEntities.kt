package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.MavenCoordinates
import java.nio.file.Path

data class BuildTargetInfo(
  val id: Label,
  val displayName: String? = null,
  val dependencies: List<Label> = emptyList(),
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
  val tags: List<String> = emptyList(),
  val languageIds: LanguageIds = emptyList(),
  val baseDirectory: String? = null,
) {
  val buildTargetName: String = this.displayName ?: this.id.toShortString()
}

fun BuildTarget.toBuildTargetInfo(): BuildTargetInfo =
  BuildTargetInfo(
    id = id,
    displayName = displayName,
    dependencies = dependencies,
    capabilities = capabilities.toModuleCapabilities(),
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory,
  )

abstract class WorkspaceModelEntity

data class ContentRoot(val path: Path, val excludedPaths: List<Path> = ArrayList()) : WorkspaceModelEntity()

interface ResourceRootEntity

interface EntityDependency

data class GenericSourceRoot(
  val sourcePath: Path,
  val rootType: SourceRootTypeId,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

data class ResourceRoot(val resourcePath: Path, val rootType: SourceRootTypeId) :
  WorkspaceModelEntity(),
  ResourceRootEntity

data class Library(
  val displayName: String,
  val iJars: List<String> = listOf(),
  val sourceJars: List<String> = listOf(),
  val classJars: List<String> = listOf(),
  val mavenCoordinates: MavenCoordinates? = null,
) : WorkspaceModelEntity(),
  ResourceRootEntity {
  companion object {
    fun formatJarString(jar: String): String =
      if (jar.endsWith(".jar")) {
        "jar://${jar.safeCastToURI().path}!/"
      } else {
        // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
        jar
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
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
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
    capabilities: ModuleCapabilities = ModuleCapabilities(),
    languageIds: Map<String, String>,
    associates: List<IntermediateModuleDependency> = listOf(),
  ) : this(
    name,
    type,
    modulesDependencies,
    librariesDependencies,
    capabilities,
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

data class ModuleCapabilities(
  val canRun: Boolean = false,
  val canTest: Boolean = false,
  val canCompile: Boolean = false,
  val canDebug: Boolean = false,
) {
  fun asMap(): Map<String, String> =
    mapOf(
      KEYS.CAN_RUN.name to canRun.toString(),
      KEYS.CAN_DEBUG.name to canDebug.toString(),
      KEYS.CAN_TEST.name to canTest.toString(),
      KEYS.CAN_COMPILE.name to canCompile.toString(),
    )

  private enum class KEYS {
    CAN_RUN,
    CAN_DEBUG,
    CAN_TEST,
    CAN_COMPILE,
  }

  fun isExecutable(): Boolean = canRun || canTest
}

fun BuildTargetCapabilities.toModuleCapabilities() =
  ModuleCapabilities(canRun == true, canTest == true, canCompile == true, canDebug == true)

interface Module {
  fun getModuleName(): String
}

data class CompiledSourceCodeInsideJarExclude(val relativePathsInsideJarToExclude: Set<String>) : WorkspaceModelEntity()
