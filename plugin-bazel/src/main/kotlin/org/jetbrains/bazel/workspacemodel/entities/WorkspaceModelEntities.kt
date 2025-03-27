package org.jetbrains.bazel.workspacemodel.entities

import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
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
) {
  fun asMap(): Map<String, String> =
    mapOf(
      KEYS.CAN_RUN.name to canRun.toString(),
      KEYS.CAN_TEST.name to canTest.toString(),
      KEYS.CAN_COMPILE.name to canCompile.toString(),
    )

  private enum class KEYS {
    CAN_RUN,
    CAN_TEST,
    CAN_COMPILE,
  }

  fun isExecutable(): Boolean = canRun || canTest
}

fun BuildTargetCapabilities.toModuleCapabilities() = ModuleCapabilities(canRun == true, canTest == true, canCompile == true)

interface Module {
  fun getModuleName(): String
}

data class CompiledSourceCodeInsideJarExclude(val relativePathsInsideJarToExclude: Set<String>) : WorkspaceModelEntity()
