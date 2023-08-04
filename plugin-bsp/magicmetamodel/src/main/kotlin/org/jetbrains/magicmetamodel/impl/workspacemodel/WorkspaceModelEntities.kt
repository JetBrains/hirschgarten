package org.jetbrains.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import org.jetbrains.magicmetamodel.impl.ModuleState
import java.nio.file.Path

public data class BuildTargetInfo(
  val id: BuildTargetId,
  val displayName: String? = null,
  val dependencies: List<BuildTargetId> = emptyList(),
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
  val languageIds: LanguageIds = emptyList(),
)

public fun BuildTarget.toBuildTargetInfo(): BuildTargetInfo =
  BuildTargetInfo(
    id = id.uri,
    displayName = displayName,
    dependencies = dependencies.map { it.uri },
    capabilities = capabilities.toModuleCapabilities(),
    languageIds = languageIds,
  )

public abstract class WorkspaceModelEntity

public data class ContentRoot(
  val path: Path,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

public interface ResourceRootEntity
public interface EntityDependency

public data class GenericSourceRoot(
  val sourcePath: Path,
  val rootType: String,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

public data class ResourceRoot(
  val resourcePath: Path,
) : WorkspaceModelEntity(), ResourceRootEntity

public data class Library(
  val displayName: String,
  val sourceJars: List<String> = listOf(),
  val classJars: List<String> = listOf(),
  val capabilities: ModuleCapabilities = ModuleCapabilities()
) : WorkspaceModelEntity(), ResourceRootEntity

public data class ModuleDependency(
  val moduleName: String,
) : WorkspaceModelEntity(), EntityDependency

public data class LibraryDependency(
  val libraryName: String,
  val isProjectLevelLibrary: Boolean = false
) : WorkspaceModelEntity(), EntityDependency

/**
This class holds basic module data that are not language-specific
 */
public data class GenericModuleInfo(
  val name: String,
  val type: String,
  val modulesDependencies: List<ModuleDependency>,
  val librariesDependencies: List<LibraryDependency>,
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
  val languageIds: LanguageIds = listOf(),
  val associates: List<ModuleDependency> = listOf(),
) : WorkspaceModelEntity() {

  internal constructor(
    name: String,
    type: String,
    modulesDependencies: List<ModuleDependency>,
    librariesDependencies: List<LibraryDependency>,
    capabilities: ModuleCapabilities = ModuleCapabilities(),
    languageIds: Map<String, String>,
    associates: List<ModuleDependency> = listOf(),
  ) : this(
    name,
    type,
    modulesDependencies,
    librariesDependencies,
    capabilities,
    languageIdsFromMap(languageIds),
    associates
  )

  internal val languageIdsAsSingleEntryMap: Map<String, String>
    get() = languageIds.takeUnless { it.isEmpty() }?.let {
      mapOf(LANGUAGE_IDS to it.joinToString(LANGUAGE_IDS_SEPARATOR))
    }.orEmpty()

  private companion object {
    const val LANGUAGE_IDS = "languageIds"
    const val LANGUAGE_IDS_SEPARATOR = ","
    fun languageIdsFromMap(map: Map<String, String>): List<String> =
      map[LANGUAGE_IDS]?.split(LANGUAGE_IDS_SEPARATOR).orEmpty()
  }
}

public data class ModuleCapabilities(
  val canRun: Boolean = false,
  val canTest: Boolean = false,
  val canCompile: Boolean = false,
  val canDebug: Boolean = false
) {
  public constructor(map: Map<String, String>) : this(
    map[KEYS.CAN_RUN.name]?.toBoolean() ?: false,
    map[KEYS.CAN_TEST.name]?.toBoolean() ?: false,
    map[KEYS.CAN_COMPILE.name]?.toBoolean() ?: false,
    map[KEYS.CAN_DEBUG.name]?.toBoolean() ?: false,
  )

  public fun asMap(): Map<String, String> =
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
}

internal fun BuildTargetCapabilities.toModuleCapabilities() =
  ModuleCapabilities(canRun, canTest, canCompile, canDebug)

public interface Module {
  public fun toState(): ModuleState
  public fun getModuleName(): String
}
