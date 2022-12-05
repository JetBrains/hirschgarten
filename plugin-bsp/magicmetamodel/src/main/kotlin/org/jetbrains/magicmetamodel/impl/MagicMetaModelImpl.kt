package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.workspaceModel.ide.BuilderSnapshot
import com.intellij.workspaceModel.ide.StorageReplacement
import com.intellij.workspaceModel.ide.WorkspaceModel
import org.jetbrains.magicmetamodel.*
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater


internal class DefaultMagicMetaModelDiff(
  private val workspaceModel: WorkspaceModel,
  private val storageReplacement: StorageReplacement,
) : MagicMetaModelDiff {

  override fun applyOnWorkspaceModel(): Boolean =
    workspaceModel.replaceProjectModel(storageReplacement)
}

internal class EmptyMagicMetaModelDiff : MagicMetaModelDiff {

  override fun applyOnWorkspaceModel(): Boolean =
    true
}

// TODO - get rid of *Impl - we should name it 'DefaultMagicMetaModel' or something like that
/**
 * Basic implementation of [MagicMetaModel] supporting shared sources
 * provided by the BSP and build on top of [WorkspaceModel].
 */
public class MagicMetaModelImpl : MagicMetaModel, ConvertableToState<DefaultMagicMetaModelState> {

  private val magicMetaModelProjectConfig: MagicMetaModelProjectConfig
  private val projectDetails: ProjectDetails

  private val targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider
  private val overlappingTargetsGraph: Map<BuildTargetIdentifier, Set<BuildTargetIdentifier>>

  private val targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>

  private val loadedTargetsStorage: LoadedTargetsStorage

  private val targetLoadListeners = mutableSetOf<() -> Unit>()

  internal constructor(
    magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    projectDetails: ProjectDetails,
  ) {
    log.debug { "Initializing MagicMetaModelImpl with: $magicMetaModelProjectConfig and $projectDetails..." }

    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.projectDetails = projectDetails

    this.targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
    this.overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

    this.targetIdToModuleDetails = TargetIdToModuleDetails(projectDetails)

    this.loadedTargetsStorage = LoadedTargetsStorage(projectDetails.targetsId)

    log.debug { "Initializing MagicMetaModelImpl done!" }
  }

  internal constructor(state: DefaultMagicMetaModelState, magicMetaModelProjectConfig: MagicMetaModelProjectConfig) {
    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.projectDetails = state.projectDetailsState.fromState()

    this.targetsDetailsForDocumentProvider =
      TargetsDetailsForDocumentProvider(state.targetsDetailsForDocumentProviderState)
    this.overlappingTargetsGraph =
      state.overlappingTargetsGraph.mapKeys { it.key.fromState() }.mapValues { it.value.map { it.fromState() }.toSet() }

    this.targetIdToModuleDetails =
      state.targetIdToModuleDetails.mapKeys { it.key.fromState() }.mapValues { it.value.fromState() }
    this.loadedTargetsStorage =
      LoadedTargetsStorage(state.loadedTargetsStorageState)
  }



  override fun loadDefaultTargets(): MagicMetaModelDiff {
    log.debug { "Calculating default targets to load..." }

    val nonOverlappingTargetsToLoad = logPerformance("compute-non-overlapping-targets") {
      NonOverlappingTargets(projectDetails.targets, overlappingTargetsGraph)
    }

    log.debug { "Calculating default targets to load done! Targets to load: $nonOverlappingTargetsToLoad" }

    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
    )

    workspaceModelUpdater.clear()
    loadedTargetsStorage.clear()

    val modulesToLoad = getModulesDetailsForTargetsToLoad(nonOverlappingTargetsToLoad)

    // TODO TEST TESTS TEESTS RTEST11
    logPerformance("load-modules") { workspaceModelUpdater.loadModules(modulesToLoad) }
    loadedTargetsStorage.addTargets(nonOverlappingTargetsToLoad)


    return DefaultMagicMetaModelDiff(
      magicMetaModelProjectConfig.workspaceModel,
      builderSnapshot.getStorageReplacement()
    )
  }

  // TODO what if null?
  private fun getModulesDetailsForTargetsToLoad(targetsToLoad: Collection<BuildTargetIdentifier>): List<ModuleDetails> =
    targetsToLoad.map { targetIdToModuleDetails[it]!! }

  override fun loadTarget(targetId: BuildTargetIdentifier): MagicMetaModelDiff {
    throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId)

    return if (loadedTargetsStorage.isTargetNotLoaded(targetId)) {
      val builderSnapshot = loadTargetAndRemoveOverlappingLoadedTargets(targetId)
      triggerTargetLoadListeners()

      DefaultMagicMetaModelDiff(
        magicMetaModelProjectConfig.workspaceModel,
        builderSnapshot.getStorageReplacement()
      )
    } else {
      EmptyMagicMetaModelDiff()
    }
  }

  private fun triggerTargetLoadListeners(): Unit =
    targetLoadListeners.forEach { listener -> listener() }

  override fun registerTargetLoadListener(function: () -> Unit) {
    targetLoadListeners.add(function)
  }

  public fun copyAllTargetLoadListenersTo(other: MagicMetaModelImpl) {
    targetLoadListeners.forEach { other.registerTargetLoadListener(it) }
  }

  private fun throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId: BuildTargetIdentifier) {
    if (isTargetNotIncludedInTheModel(targetId)) {
      throw IllegalArgumentException("Target $targetId is not included in the model.")
    }
  }

  private fun isTargetNotIncludedInTheModel(targetId: BuildTargetIdentifier): Boolean =
    !projectDetails.targetsId.contains(targetId)

  private fun loadTargetAndRemoveOverlappingLoadedTargets(targetIdToLoad: BuildTargetIdentifier): BuilderSnapshot {
    val targetsToRemove = overlappingTargetsGraph[targetIdToLoad] ?: emptySet()
    // TODO test it!
    val loadedTargetsToRemove = targetsToRemove.filter(loadedTargetsStorage::isTargetLoaded)

    val modulesToRemove = loadedTargetsToRemove.map { ModuleName(it.uri) }
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
    )
    workspaceModelUpdater.removeModules(modulesToRemove)
    loadedTargetsStorage.removeTargets(loadedTargetsToRemove)

    // TODO null!!!
    val moduleToAdd = targetIdToModuleDetails[targetIdToLoad]!!
    workspaceModelUpdater.loadModule(moduleToAdd)
    loadedTargetsStorage.addTarget(targetIdToLoad)

    return builderSnapshot
  }

  override fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): DocumentTargetsDetails {
    val documentTargets = targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(documentId)

    // TODO maybe wo should check is there only 1 loaded? what if 2 are loaded - it means that we have a bug
    val loadedTarget = loadedTargetsStorage.getLoadedTargets().firstOrNull { documentTargets.contains(it) }
    val notLoadedTargets = loadedTargetsStorage.getNotLoadedTargets().filter { documentTargets.contains(it) }

    return DocumentTargetsDetails(
      loadedTargetId = loadedTarget,
      notLoadedTargetsIds = notLoadedTargets,
    )
  }

  override fun getAllLoadedTargets(): List<BuildTarget> =
    projectDetails.targets.filter { loadedTargetsStorage.isTargetLoaded(it.id) }

  override fun getAllNotLoadedTargets(): List<BuildTarget> =
    projectDetails.targets.filter { loadedTargetsStorage.isTargetNotLoaded(it.id) }

  override fun clear() {
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
    )

    workspaceModelUpdater.clear()
    loadedTargetsStorage.clear()
  }

  // TODO - test
  override fun toState(): DefaultMagicMetaModelState =
    DefaultMagicMetaModelState(
      projectDetailsState = projectDetails.toState(),
      targetsDetailsForDocumentProviderState = targetsDetailsForDocumentProvider.toState(),
      overlappingTargetsGraph = overlappingTargetsGraph.mapKeys { it.key.toState() }
        .mapValues { it.value.map { it.toState() } },
      targetIdToModuleDetails = targetIdToModuleDetails.mapKeys { it.key.toState() }
        .mapValues { it.value.toState() },
      loadedTargetsStorageState = loadedTargetsStorage.toState()
    )

  private companion object {
    private val log = logger<MagicMetaModelImpl>()
  }
}

public data class LoadedTargetsStorageState(
  public var allTargets: Collection<BuildTargetIdentifierState> = emptyList(),
  public var loadedTargets: List<BuildTargetIdentifierState> = emptyList(),
  public var notLoadedTargets: List<BuildTargetIdentifierState> = emptyList(),
)

private class LoadedTargetsStorage {

  private val allTargets: Collection<BuildTargetIdentifier>

  private val loadedTargets: MutableSet<BuildTargetIdentifier>
  private val notLoadedTargets: MutableSet<BuildTargetIdentifier>

  constructor(allTargets: Collection<BuildTargetIdentifier>) {
    this.allTargets = allTargets

    this.loadedTargets = mutableSetOf()
    this.notLoadedTargets = allTargets.toMutableSet()
  }

  constructor(state: LoadedTargetsStorageState) {
    this.allTargets = state.allTargets.map { it.fromState() }
    this.loadedTargets = state.loadedTargets.map { it.fromState() }.toMutableSet()
    this.notLoadedTargets = state.notLoadedTargets.map { it.fromState() }.toMutableSet()
  }

  fun clear() {
    loadedTargets.clear()
    notLoadedTargets.addAll(allTargets)
  }

  fun addTargets(targets: Collection<BuildTargetIdentifier>) {
    loadedTargets.addAll(targets)
    notLoadedTargets.removeAll(targets.toSet())
  }

  fun addTarget(target: BuildTargetIdentifier) {
    loadedTargets.add(target)
    notLoadedTargets.remove(target)
  }

  fun removeTargets(targets: Collection<BuildTargetIdentifier>) {
    loadedTargets.removeAll(targets.toSet())
    notLoadedTargets.addAll(targets)
  }

  fun isTargetNotLoaded(targetId: BuildTargetIdentifier): Boolean =
    notLoadedTargets.contains(targetId)

  fun isTargetLoaded(targetId: BuildTargetIdentifier): Boolean =
    loadedTargets.contains(targetId)

  fun getLoadedTargets(): List<BuildTargetIdentifier> =
    loadedTargets.toList()

  fun getNotLoadedTargets(): List<BuildTargetIdentifier> =
    notLoadedTargets.toList()

  fun toState(): LoadedTargetsStorageState =
    LoadedTargetsStorageState(
      allTargets.map { it.toState() },
      loadedTargets.map { it.toState() },
      notLoadedTargets.map { it.toState() })
}
