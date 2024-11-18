package org.jetbrains.plugins.bsp.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.plugins.bsp.workspacemodel.entities.ModuleCapabilities

interface ConvertableFromState<out T> {
  fun fromState(): T
}

data class BuildTargetInfoState(
  var id: String = "",
  var displayName: String? = null,
  var dependencies: List<String> = emptyList(),
  var capabilities: ModuleCapabilitiesState = ModuleCapabilitiesState(),
  var tags: List<String> = emptyList(),
  var languageIds: List<String> = emptyList(),
  var baseDirectory: String? = null,
) : ConvertableFromState<BuildTargetInfo> {
  override fun fromState(): BuildTargetInfo =
    BuildTargetInfo(
      id = BuildTargetIdentifier(id),
      displayName = displayName,
      dependencies = dependencies.map { BuildTargetIdentifier(it) },
      capabilities = capabilities.fromState(),
      tags = tags,
      languageIds = languageIds,
      baseDirectory = baseDirectory,
    )
}

fun BuildTargetInfo.toState(): BuildTargetInfoState =
  BuildTargetInfoState(
    id = id.uri,
    displayName = displayName,
    dependencies = dependencies.map { it.uri },
    capabilities = capabilities.toState(),
    tags = tags,
    languageIds = languageIds,
    baseDirectory = baseDirectory,
  )

data class ModuleCapabilitiesState(
  var canRun: Boolean = false,
  var canTest: Boolean = false,
  var canCompile: Boolean = false,
  var canDebug: Boolean = false,
) : ConvertableFromState<ModuleCapabilities> {
  override fun fromState(): ModuleCapabilities =
    ModuleCapabilities(
      canRun = canRun,
      canTest = canTest,
      canCompile = canCompile,
      canDebug = canDebug,
    )
}

fun ModuleCapabilities.toState(): ModuleCapabilitiesState =
  ModuleCapabilitiesState(
    canRun = canRun,
    canTest = canTest,
    canCompile = canCompile,
    canDebug = canDebug,
  )
