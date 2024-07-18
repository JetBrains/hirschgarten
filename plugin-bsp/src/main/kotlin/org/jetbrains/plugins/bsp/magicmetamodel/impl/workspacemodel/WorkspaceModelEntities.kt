package org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import com.intellij.platform.workspace.jps.entities.ModuleTypeId
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.ModuleState
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.nio.file.Path

public data class BuildTargetInfo(
  val id: BuildTargetId,
  val displayName: String? = null,
  val dependencies: List<BuildTargetId> = emptyList(),
  val capabilities: ModuleCapabilities = ModuleCapabilities(),
  val tags: List<String> = emptyList(),
  val languageIds: LanguageIds = emptyList(),
  val baseDirectory: String? = null,
)

public fun BuildTarget.toBuildTargetInfo(): BuildTargetInfo =
  BuildTargetInfo(
    id = id.uri,
    displayName = displayName,
    dependencies = dependencies.map { it.uri },
    capabilities = capabilities.toModuleCapabilities(),
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory,
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
  val rootType: SourceRootTypeId,
  val excludedPaths: List<Path> = ArrayList(),
) : WorkspaceModelEntity()

public data class ResourceRoot(
  val resourcePath: Path,
  val rootType: SourceRootTypeId,
) : WorkspaceModelEntity(), ResourceRootEntity

public data class Library(
  val displayName: String,
  val iJars: List<String> = listOf(),
  val sourceJars: List<String> = listOf(),
  val classJars: List<String> = listOf(),
) : WorkspaceModelEntity(), ResourceRootEntity {
  public companion object {
    public fun formatJarString(jar: String): String =
      if (jar.endsWith(".jar")) {
        "jar://${jar.safeCastToURI().path}!/"
      } else {
        // There can be other library roots except for jars, e.g., Android resources. Use the file:// scheme then.
        jar
      }
  }
}

public data class IntermediateModuleDependency(
  val moduleName: String,
) : WorkspaceModelEntity(), EntityDependency

public data class IntermediateLibraryDependency(
  val libraryName: String,
  val isProjectLevelLibrary: Boolean = false,
) : WorkspaceModelEntity(), EntityDependency

/**
This class holds basic module data that are not language-specific
 */
public data class GenericModuleInfo(
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
  val canDebug: Boolean = false,
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
  ModuleCapabilities(canRun == true, canTest == true, canCompile == true, canDebug == true)

public interface Module {
  public fun toState(): ModuleState
  public fun getModuleName(): String
}
