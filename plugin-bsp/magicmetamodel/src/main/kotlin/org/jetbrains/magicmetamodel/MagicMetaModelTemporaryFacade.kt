package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import org.jetbrains.magicmetamodel.impl.BuildTargetInfoState
import org.jetbrains.magicmetamodel.impl.ModuleState
import org.jetbrains.magicmetamodel.impl.PerformanceLogger
import org.jetbrains.magicmetamodel.impl.TargetIdToModuleEntitiesMap
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProvider
import org.jetbrains.magicmetamodel.impl.TargetsDetailsForDocumentProviderState
import org.jetbrains.magicmetamodel.impl.TargetsStatusStorage
import org.jetbrains.magicmetamodel.impl.toState
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelToModulesMapTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.toPair

public data class MagicMetaModelTemporaryFacadeState(
  var targetsDetailsForDocumentProviderState: TargetsDetailsForDocumentProviderState =
    TargetsDetailsForDocumentProviderState(),
  var targets: List<BuildTargetInfoState> = emptyList(),
  var unloadedTargets: Map<BuildTargetId, ModuleState> = emptyMap(),
)

public class MagicMetaModelTemporaryFacade {
  private val targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider
  private val magicMetaModelProjectConfig: MagicMetaModelProjectConfig

  // it's here only temporarily, will be removed in the following PRs
  private val targetsStatusStorage: TargetsStatusStorage

  public val targets: Map<BuildTargetId, BuildTargetInfo>
  private val targetIdToModule: Map<BuildTargetId, Module>

  public constructor(
    projectDetails: ProjectDetails,
    magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    targetsStatusStorage: TargetsStatusStorage,
  ) {
    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.targetsStatusStorage = targetsStatusStorage

    this.targetsDetailsForDocumentProvider =
      PerformanceLogger.logPerformance("create-target-details-for-document-provider") {
        TargetsDetailsForDocumentProvider(projectDetails.sources)
      }
    this.targets = projectDetails.targets.associate { it.toBuildTargetInfo().toPair() }
    this.targetIdToModule = PerformanceLogger.logPerformance("create-target-id-to-module-entities-map") {
      TargetIdToModuleEntitiesMap(
        projectDetails = projectDetails,
        projectBasePath = magicMetaModelProjectConfig.projectBasePath,
        targetsMap = targets,
        moduleNameProvider = magicMetaModelProjectConfig.moduleNameProvider,
        hasDefaultPythonInterpreter = magicMetaModelProjectConfig.hasDefaultPythonInterpreter,
        isAndroidSupportEnabled = magicMetaModelProjectConfig.isAndroidSupportEnabled,
      )
    }
  }

  public constructor(
    state: MagicMetaModelTemporaryFacadeState,
    magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    targetsStatusStorage: TargetsStatusStorage,
  ) {
    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.targetsStatusStorage = targetsStatusStorage

    this.targetsDetailsForDocumentProvider =
      TargetsDetailsForDocumentProvider(state.targetsDetailsForDocumentProviderState)

    this.targets = state.targets.associate { it.fromState().toPair() }
    val unloadedTargets = state.unloadedTargets.mapValues { it.value.fromState() }
    this.targetIdToModule = WorkspaceModelToModulesMapTransformer(
      workspaceModel = magicMetaModelProjectConfig.workspaceModel,
      targetsStatusStorage = targetsStatusStorage,
      targetsMap = targets,
      moduleNameProvider = magicMetaModelProjectConfig.moduleNameProvider,
    ) + unloadedTargets
  }

  // will be removed in the following PRs
  public fun getAllTargets(): Collection<BuildTargetInfo> = targets.values

  // will be removed in the following PRs
  public fun isTargetRegistered(targetId: BuildTargetId): Boolean = targets.contains(targetId)

  // will be removed in the following PRs
  public fun getModuleForTargetId(targetId: BuildTargetId): Module? = targetIdToModule[targetId]

  // will be removed in the following PRs
  public fun getTargetInfoForTargetId(targetId: BuildTargetId): BuildTargetInfo? = targets[targetId]

  // will be removed in the following PRs
  public fun allDocuments(): List<TextDocumentIdentifier> =
    targetsDetailsForDocumentProvider.getAllDocuments()

  // in the following PRs: TextDocumentIdentifier -> VirtualFile
  public fun getTargetsForFile(textDocument: TextDocumentIdentifier): Set<BuildTargetId> =
    targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(textDocument)

  public fun toState(): MagicMetaModelTemporaryFacadeState =
    MagicMetaModelTemporaryFacadeState(
      targetsDetailsForDocumentProviderState = targetsDetailsForDocumentProvider.toState(),
      targets = targets.values.map { it.toState() },
      unloadedTargets = targetIdToModule.filterNot { targetsStatusStorage.isTargetLoaded(it.key) }
        .mapValues { it.value.toState() },
    )
}
