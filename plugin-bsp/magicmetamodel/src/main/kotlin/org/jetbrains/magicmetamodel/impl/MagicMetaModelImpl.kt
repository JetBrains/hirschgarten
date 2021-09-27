package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.workspaceModel.ide.WorkspaceModel
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.MagicMetaModel

/**
 * Basic implementation of [MagicMetaModel] supporting shared sources
 * provided by the BSP and build on top of [WorkspaceModel].
 */
internal class MagicMetaModelImpl internal constructor(
  private val workspaceModel: WorkspaceModel,
  private val targets: List<BuildTarget>,
  sources: List<SourcesItem>,
) : MagicMetaModel {

  init {
    LOGGER.debug { "Initializing MagicMetaModelImpl model..." }
  }

  private val targetsDetailsForDocumentProvider = TargetsDetailsForDocumentProvider(sources)
  private val overlappingTargetsGraph by OverlappingTargetsGraphDelegate(targetsDetailsForDocumentProvider)
  private val loadedTargetsStorage = LoadedTargetsStorage()

  init {
    LOGGER.debug { "Initializing MagicMetaModelImpl model done!" }
  }

  override fun loadDefaultTargets() {
    LOGGER.debug { "Calculating default targets to load..." }

    val nonOverlappingTargetsToLoad by NonOverlappingTargetsDelegate(overlappingTargetsGraph)

    LOGGER.debug { "Calculating default targets to load done! Targets to load: $nonOverlappingTargetsToLoad" }

    @Suppress("ForbiddenComment")
    // TODO: add mapping to the workspace model
    loadedTargetsStorage.clear()
    loadedTargetsStorage.addTargets(nonOverlappingTargetsToLoad)
  }

  override fun loadTarget(targetId: BuildTargetIdentifier) {
    throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId)

    if (loadedTargetsStorage.isTargetNotLoaded(targetId)) {
      @Suppress("ForbiddenComment")
      // TODO: add mapping to the workspace model
      loadTargetAndRemoveOverlappingLoadedTargets(targetId)
    }
  }

  private fun throwIllegalArgumentExceptionIfTargetIsNotIncludedInTheModel(targetId: BuildTargetIdentifier) {
    if (isTargetNotIncludedInTheModel(targetId)) {
      throw IllegalArgumentException("Target $targetId is not included in the model.")
    }
  }

  private fun isTargetNotIncludedInTheModel(targetId: BuildTargetIdentifier): Boolean =
    !targets.any { it.id == targetId }

  private fun loadTargetAndRemoveOverlappingLoadedTargets(targetIdToLoad: BuildTargetIdentifier) {
    val targetsToRemove = overlappingTargetsGraph[targetIdToLoad] ?: emptySet()

    loadedTargetsStorage.removeTargets(targetsToRemove)
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
    targets.filter(loadedTargetsStorage::isTargetLoaded)

  override fun getAllNotLoadedTargets(): List<BuildTarget> =
    targets.filterNot(loadedTargetsStorage::isTargetLoaded)

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
