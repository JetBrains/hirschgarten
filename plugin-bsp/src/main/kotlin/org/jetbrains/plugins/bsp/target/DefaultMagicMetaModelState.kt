package org.jetbrains.plugins.bsp.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.Library
import org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities
import kotlin.collections.map

public interface ConvertableFromState<out T> {
  public fun fromState(): T
}

public data class BuildTargetInfoState(
  var id: String = "",
  var displayName: String? = null,
  var dependencies: List<String> = kotlin.collections.emptyList(),
  var capabilities: ModuleCapabilitiesState = ModuleCapabilitiesState(),
  var tags: List<String> = kotlin.collections.emptyList(),
  var languageIds: List<String> = kotlin.collections.emptyList(),
  var baseDirectory: String? = null,
) : ConvertableFromState<org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo> {
  override fun fromState(): org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo =
    org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo(
      id =
        ch.epfl.scala.bsp4j
          .BuildTargetIdentifier(id),
      displayName = displayName,
      dependencies =
        dependencies.map {
          ch.epfl.scala.bsp4j
            .BuildTargetIdentifier(it)
        },
      capabilities = capabilities.fromState(),
      tags = tags,
      languageIds = languageIds,
      baseDirectory = baseDirectory,
    )
}

public fun org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo.toState(): BuildTargetInfoState =
  BuildTargetInfoState(
    id = id.uri,
    displayName = displayName,
    dependencies = dependencies.map { it.uri },
    capabilities = capabilities.toState(),
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory,
  )

public data class LibraryState(
  var displayName: String = "",
  var sourceJars: List<String> = kotlin.collections.emptyList(),
  var classJars: List<String> = kotlin.collections.emptyList(),
) : ConvertableFromState<org.jetbrains.plugins.bsp.workspacemodel.entities.Library> {
  override fun fromState(): org.jetbrains.plugins.bsp.workspacemodel.entities.Library =
    org.jetbrains.plugins.bsp.workspacemodel.entities.Library(
      displayName = displayName,
      sourceJars = sourceJars,
      classJars = classJars,
    )
}

public fun org.jetbrains.plugins.bsp.workspacemodel.entities.Library.toState(): LibraryState =
  LibraryState(
    displayName = displayName,
    sourceJars = sourceJars,
    classJars = classJars,
  )

public data class ModuleCapabilitiesState(
  var canRun: Boolean = false,
  var canTest: Boolean = false,
  var canCompile: Boolean = false,
  var canDebug: Boolean = false,
) : ConvertableFromState<org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities> {
  override fun fromState(): org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities =
    org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities(
      canRun = canRun,
      canTest = canTest,
      canCompile = canCompile,
      canDebug = canDebug,
    )
}

public fun org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities.toState(): ModuleCapabilitiesState =
  ModuleCapabilitiesState(
    canRun = canRun,
    canTest = canTest,
    canCompile = canCompile,
    canDebug = canDebug,
  )
