package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.workspaceModel.ide.WorkspaceModel
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater

/**
 * Basic implementation of [MagicMetaModel] supporting shared sources
 * provided by the BSP and build on top of [WorkspaceModel].
 */
internal class MagicMetaModelImpl internal constructor(
  private val magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
  private val projectDetails: ProjectDetails,
) : MagicMetaModel {

  init {
    LOGGER.debug { "Initializing MagicMetaModelImpl model..." }
  }

  private val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(projectDetails.sources)
  private val overlappingTargetsGraph = OverlappingTargetsGraph(targetsDetailsForDocumentProvider)

  private val targetIdToModuleDetails = TargetIdToModuleDetails(projectDetails)

  private val loadedTargetsStorage = LoadedTargetsStorage()

  private val workspaceModelSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()

  private val workspaceModelUpdater = WorkspaceModelUpdater.create(
    workspaceModelSnapshot.builder,
    magicMetaModelProjectConfig.virtualFileUrlManager,
    magicMetaModelProjectConfig.projectBaseDir
  )

  init {
    LOGGER.debug { "Initializing MagicMetaModelImpl model done!" }
  }

  override fun save() {
    val toSave = workspaceModelSnapshot.getStorageReplacement()
    magicMetaModelProjectConfig.workspaceModel.replaceProjectModel(toSave)
  }

  override fun loadDefaultTargets() {
    LOGGER.debug { "Calculating default targets to load..." }

    val nonOverlappingTargetsToLoad = NonOverlappingTargets(projectDetails.targets, overlappingTargetsGraph)

    LOGGER.debug { "Calculating default targets to load done! Targets to load: $nonOverlappingTargetsToLoad" }

    workspaceModelUpdater.clear()
    loadedTargetsStorage.clear()

    val modulesToLoad = getModulesDetailsForTargetsToLoad(nonOverlappingTargetsToLoad)

    // TODO TEST TESTS TEESTS RTEST11
    workspaceModelUpdater.loadModules(modulesToLoad)
    loadedTargetsStorage.addTargets(nonOverlappingTargetsToLoad)
  }

  // TODO what if null?
  private fun getModulesDetailsForTargetsToLoad(targetsToLoad: Collection<BuildTargetIdentifier>): List<ModuleDetails> =
    targetsToLoad.map { targetIdToModuleDetails[it]!! }

  override fun loadTarget(targetId: BuildTargetIdentifier) {
    throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId)

    if (loadedTargetsStorage.isTargetNotLoaded(targetId)) {
      loadTargetAndRemoveOverlappingLoadedTargets(targetId)
    }
  }

  private fun throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId: BuildTargetIdentifier) {
    if (isTargetNotIncludedInTheModel(targetId)) {
      throw IllegalArgumentException("Target $targetId is not included in the model.")
    }
  }

  private fun isTargetNotIncludedInTheModel(targetId: BuildTargetIdentifier): Boolean =
    !projectDetails.targetsId.contains(targetId)

  private fun loadTargetAndRemoveOverlappingLoadedTargets(targetIdToLoad: BuildTargetIdentifier) {
    val targetsToRemove = overlappingTargetsGraph[targetIdToLoad] ?: emptySet()
    // TODO test it!
    val loadedTargetsToRemove = targetsToRemove.filter(loadedTargetsStorage::isTargetLoaded)

    val modulesToRemove = loadedTargetsToRemove.map { ModuleName(it.uri) }
    workspaceModelUpdater.removeModules(modulesToRemove)
    loadedTargetsStorage.removeTargets(loadedTargetsToRemove)

    // TODO null!!!
    val moduleToAdd = targetIdToModuleDetails[targetIdToLoad]!!
    workspaceModelUpdater.loadModule(moduleToAdd)
    loadedTargetsStorage.addTarget(targetIdToLoad)
  }

  override fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): DocumentTargetsDetails {
    val allTargetsIds = targetsDetailsForDocumentProvider.getTargetsDetailsForDocument(documentId)

    val loadedTarget = loadedTargetsStorage.getLoadedTargetOrNull(allTargetsIds)
    val notLoadedTargets = loadedTargetsStorage.getNotLoadedTargets(allTargetsIds)

    return DocumentTargetsDetails(
      loadedTargetId = loadedTarget,
      notLoadedTargetsIds = notLoadedTargets,
    )
  }

  override fun getAllLoadedTargets(): List<BuildTarget> =
    projectDetails.targets.filter(loadedTargetsStorage::isTargetLoaded)

  override fun getAllNotLoadedTargets(): List<BuildTarget> =
    projectDetails.targets.filterNot(loadedTargetsStorage::isTargetLoaded)

  companion object {
    private val LOGGER = logger<MagicMetaModelImpl>()
  }
}

private class LoadedTargetsStorage {

  private val loadedTargets = mutableSetOf<BuildTargetIdentifier>()

  fun clear() =
    loadedTargets.clear()

  fun addTargets(targets: Collection<BuildTargetIdentifier>) =
    loadedTargets.addAll(targets)

  fun addTarget(target: BuildTargetIdentifier) =
    loadedTargets.add(target)

  fun removeTargets(targets: Collection<BuildTargetIdentifier>) =
    loadedTargets.removeAll(targets)

  fun getLoadedTargetOrNull(allTargets: List<BuildTargetIdentifier>): BuildTargetIdentifier? =
    allTargets.firstOrNull(this::isTargetLoaded)

  fun getNotLoadedTargets(allTargets: List<BuildTargetIdentifier>): List<BuildTargetIdentifier> =
    allTargets.filterNot(this::isTargetLoaded)

  fun isTargetNotLoaded(targetId: BuildTargetIdentifier): Boolean =
    !isTargetLoaded(targetId)

  fun isTargetLoaded(targetId: BuildTargetIdentifier): Boolean =
    loadedTargets.contains(targetId)

  fun isTargetLoaded(target: BuildTarget): Boolean =
    loadedTargets.contains(target.id)
}
