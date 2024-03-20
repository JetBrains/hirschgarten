package org.jetbrains.plugins.bsp.magicmetamodel.impl

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.internal
import com.intellij.platform.workspace.jps.JpsFileDependentEntitySource
import com.intellij.platform.workspace.jps.JpsFileEntitySource
import com.intellij.platform.workspace.jps.JpsGlobalFileEntitySource
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bsp.protocol.DirectoryItem
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.WorkspaceDirectoriesResult
import org.jetbrains.plugins.bsp.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.plugins.bsp.magicmetamodel.MagicMetaModel
import org.jetbrains.plugins.bsp.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.plugins.bsp.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.plugins.bsp.magicmetamodel.MagicMetaModelTemporaryFacade
import org.jetbrains.plugins.bsp.magicmetamodel.ProjectDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.workspacemodel.entities.BspDummyEntitySource
import org.jetbrains.workspacemodel.entities.BspEntitySource

internal class DefaultMagicMetaModelDiff(
  private val workspaceModel: WorkspaceModel,
  private val builder: MutableEntityStorage,
  private val mmmStorageReplacement: TargetsStatusStorage,
  private val mmmInstance: MagicMetaModelImpl,
  private val targetLoadListeners: Set<() -> Unit>,
) : MagicMetaModelDiff {
  override suspend fun applyOnWorkspaceModel() {
    val snapshot = workspaceModel.internal.getBuilderSnapshot()
    snapshot.builder.replaceBySource({ it.isBspRelevant() }, builder)
    val storageReplacement = snapshot.getStorageReplacement()
    writeAction {
      if (workspaceModel.internal.replaceProjectModel(storageReplacement)) {
        mmmInstance.loadStorage(mmmStorageReplacement)
        // all the listeners do UI things so they must be invoked under EDT
        targetLoadListeners.forEach { it() }
      } else {
        error("Project model is not updated successfully. Try `reload` action to recalculate the project model.")
      }
    }
  }

  private fun EntitySource.isBspRelevant() =
    when (this) {
      // avoid touching global sources
      is JpsGlobalFileEntitySource -> false

      is JpsFileEntitySource,
      is JpsFileDependentEntitySource,
      is BspEntitySource,
      is BspDummyEntitySource,
      -> true

      else -> false
    }
}

// TODO - get rid of *Impl - we should name it 'DefaultMagicMetaModel' or something like that
/**
 * Basic implementation of [MagicMetaModel] supporting shared sources
 * provided by the BSP and build on top of [WorkspaceModel].
 */
public class MagicMetaModelImpl : MagicMetaModel, ConvertableToState<DefaultMagicMetaModelState> {
  private val magicMetaModelProjectConfig: MagicMetaModelProjectConfig
  public val facade: MagicMetaModelTemporaryFacade

  private val overlappingTargetsGraph: Map<BuildTargetId, Set<BuildTargetId>>
  private var targetsStatusStorage: TargetsStatusStorage

  private val targetLoadListeners = mutableSetOf<() -> Unit>()

  // TODO (BAZEL-831): prob all the following fields should be removed from MMM
  private val libraries: List<Library>?

  // out of mmm
  private val directories: WorkspaceDirectoriesResult?

  private val outputPathUris: List<String>

  internal constructor(
    magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    projectDetails: ProjectDetails,
  ) {
    log.debug { "Initializing MagicMetaModelImpl with: $magicMetaModelProjectConfig and $projectDetails..." }

    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig

    this.targetsStatusStorage = logPerformance("create-loaded-targets-storage") {
      TargetsStatusStorage(projectDetails.targetsId.map { it.uri })
    }

    this.facade = MagicMetaModelTemporaryFacade(projectDetails, magicMetaModelProjectConfig, targetsStatusStorage)

    this.overlappingTargetsGraph = logPerformance("create-overlapping-targets-graph") {
      OverlappingTargetsGraph(facade)
    }

    this.libraries = logPerformance("create-libraries") {
      createLibraries(projectDetails.libraries)
    }

    this.directories = projectDetails.directories

    this.outputPathUris = projectDetails.outputPathUris

    log.debug { "Initializing MagicMetaModelImpl done!" }
  }

  private fun createLibraries(libraries: List<LibraryItem>?) = libraries?.map {
    Library(
      displayName = it.id.uri,
      iJars = it.ijars,
      classJars = it.jars,
      sourceJars = it.sourceJars,
    )
  }

  internal constructor(state: DefaultMagicMetaModelState, magicMetaModelProjectConfig: MagicMetaModelProjectConfig) {
    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.targetsStatusStorage =
      TargetsStatusStorage(state.targetsStatusStorageState)

    this.facade = MagicMetaModelTemporaryFacade(state.facadeState, magicMetaModelProjectConfig, targetsStatusStorage)
    this.overlappingTargetsGraph = state.overlappingTargetsGraph

    this.libraries = state.libraries?.map { it.fromState() }
    this.directories = null // workspace model keeps info about them

    this.outputPathUris = state.outputPathUris
  }

  override fun loadDefaultTargets(): MagicMetaModelDiff {
    log.debug { "Calculating default targets to load..." }

    val nonOverlappingTargetsToLoad = logPerformance("compute-non-overlapping-targets") {
      NonOverlappingTargets(facade.getAllTargets().toHashSet(), overlappingTargetsGraph)
    }

    log.debug { "Calculating default targets to load done! Targets to load: $nonOverlappingTargetsToLoad" }

    val builder = MutableEntityStorage.create()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.project,
      magicMetaModelProjectConfig.isPythonSupportEnabled,
      magicMetaModelProjectConfig.isAndroidSupportEnabled,
    )

    val newStorage = targetsStatusStorage.copy()
    newStorage.clear()

    val modulesToLoad = getModulesForTargetsToLoad(nonOverlappingTargetsToLoad)

    logPerformance("load-modules") {
      workspaceModelUpdater.loadModules(modulesToLoad)
      workspaceModelUpdater.loadLibraries(libraries.orEmpty())
      workspaceModelUpdater.loadDirectories()
    }
    newStorage.addTargets(nonOverlappingTargetsToLoad)

    return DefaultMagicMetaModelDiff(
      workspaceModel = magicMetaModelProjectConfig.workspaceModel,
      builder = builder,
      mmmStorageReplacement = newStorage,
      mmmInstance = this,
      targetLoadListeners = targetLoadListeners,
    )
  }

  // TODO what if null?
  private fun getModulesForTargetsToLoad(targetsToLoad: Collection<BuildTargetId>): List<Module> =
    targetsToLoad.mapNotNull { facade.getModuleForTargetId(it) }

  private fun WorkspaceModelUpdater.loadDirectories() {
    if (directories != null) {
      val includedDirectories = directories.includedDirectories.map { it.toVirtualFileUrl() }
      val excludedDirectories = directories.excludedDirectories.map { it.toVirtualFileUrl() }
      val outputPaths = outputPathUris.map { magicMetaModelProjectConfig.virtualFileUrlManager.getOrCreateFromUri(it) }

      loadDirectories(includedDirectories, excludedDirectories + outputPaths)
    }
  }

  private fun DirectoryItem.toVirtualFileUrl(): VirtualFileUrl =
    magicMetaModelProjectConfig.virtualFileUrlManager.getOrCreateFromUri(uri)

  override fun registerTargetLoadListener(function: () -> Unit) {
    targetLoadListeners.add(function)
  }

  public fun copyAllTargetLoadListenersTo(other: MagicMetaModelImpl) {
    targetLoadListeners.forEach { other.registerTargetLoadListener(it) }
  }

  override fun loadTarget(targetId: BuildTargetId): MagicMetaModelDiff? = when {
    targetsStatusStorage.isTargetNotLoaded(targetId) -> doLoadTarget(targetId)
    else -> null
  }

  private fun BuildTargetId.isTargetLoaded() = targetsStatusStorage.isTargetLoaded(this)

  private fun BuildTargetId.isTargetNotLoaded() = targetsStatusStorage.isTargetNotLoaded(this)

  private fun BuildTargetId.getConflicts() = overlappingTargetsGraph[this] ?: emptySet()

  override fun loadTargetWithDependencies(targetId: BuildTargetId): MagicMetaModelDiff? {
    if (targetId.isTargetLoaded()) return null

    val toProcess = ArrayDeque(listOf(targetId))
    val processed = hashSetOf<BuildTargetId>()
    val conflicts = hashSetOf<BuildTargetId>()

    // If one of the dependencies we try to load would conflict with any other already processed,
    // we are skipping it. We are processing dependencies FIFO, so we can assume that those already processed
    // are not less imminent than skipped one
    val processFirst = fun(): BuildTargetId? {
      val element = toProcess.removeFirst()
      if (conflicts.contains(element)) return null
      processed.add(element)
      conflicts.addAll(element.getConflicts())
      return element
    }

    val nonConflictingDependencies = fun(target: BuildTargetInfo) =
      target.dependencies.filter { el ->
        facade.isTargetRegistered(el) && !toProcess.contains(el) && !processed.contains(el) && !conflicts.contains(el)
      }

    do {
      processFirst()
        ?.let { facade.getTargetInfoForTargetId(it) }
        ?.let { toProcess.addAll(nonConflictingDependencies(it)) }
    } while (toProcess.isNotEmpty())
    return doLoadTargets(processed, conflicts)
  }

  private fun doLoadTargets(
    targets: Collection<BuildTargetId>,
    conflicts: HashSet<BuildTargetId>,
  ): DefaultMagicMetaModelDiff {
    val targetsToLoad = targets.filter { it.isTargetNotLoaded() }
    val loadedTargetsToRemove = conflicts.filter { it.isTargetLoaded() }

    val modulesToRemove = loadedTargetsToRemove.mapNotNull { loadTargetToRemove ->
      facade.getTargetInfoForTargetId(loadTargetToRemove)
        ?.let { ModuleName(magicMetaModelProjectConfig.moduleNameProvider(it)) }
    }
    val modulesToAdd = targetsToLoad.mapNotNull { facade.getModuleForTargetId(it) }

    return modifyModel { newStorage, workspaceModelUpdater ->
      workspaceModelUpdater.removeModules(modulesToRemove)
      newStorage.removeTargets(loadedTargetsToRemove)

      modulesToAdd.forEach { workspaceModelUpdater.loadModule(it) }
      targetsToLoad.forEach { newStorage.addTarget(it) }
    }
  }

  // TODO ughh so ugly
  private fun doLoadTarget(targetId: BuildTargetId): DefaultMagicMetaModelDiff {
    val targetsToRemove = targetId.getConflicts()
    // TODO test it!
    val loadedTargetsToRemove = targetsToRemove.filter { it.isTargetLoaded() }

    val modulesToRemove = loadedTargetsToRemove.mapNotNull { loadedTargetToRemove ->
      facade.getTargetInfoForTargetId(loadedTargetToRemove)
        ?.let { ModuleName(magicMetaModelProjectConfig.moduleNameProvider(it)) }
    }
    // TODO null!!!
    val moduleToAdd = facade.getModuleForTargetId(targetId)!!

    return modifyModel { newStorage, workspaceModelUpdater ->
      workspaceModelUpdater.removeModules(modulesToRemove)
      newStorage.removeTargets(loadedTargetsToRemove)

      workspaceModelUpdater.loadModule(moduleToAdd)
      newStorage.addTarget(targetId)
    }
  }

  private fun modifyModel(
    action: (targetStorage: TargetsStatusStorage, updater: WorkspaceModelUpdater) -> Unit,
  ): DefaultMagicMetaModelDiff {
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.internal.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.project,
      magicMetaModelProjectConfig.isPythonSupportEnabled,
      magicMetaModelProjectConfig.isAndroidSupportEnabled,
    )
    val newStorage = targetsStatusStorage.copy()
    action(newStorage, workspaceModelUpdater)
    return DefaultMagicMetaModelDiff(
      workspaceModel = magicMetaModelProjectConfig.workspaceModel,
      builder = builderSnapshot.builder,
      mmmStorageReplacement = newStorage,
      mmmInstance = this,
      targetLoadListeners = targetLoadListeners,
    )
  }

  override fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): DocumentTargetsDetails {
    val documentTargets = facade.getTargetsForFile(documentId)

    // TODO maybe wo should check is there only 1 loaded? what if 2 are loaded - it means that we have a bug
    val loadedTarget = targetsStatusStorage.getLoadedTargets().firstOrNull { documentTargets.contains(it) }
    val notLoadedTargets = targetsStatusStorage.getNotLoadedTargets().filter { documentTargets.contains(it) }

    return DocumentTargetsDetails(
      loadedTargetId = loadedTarget,
      notLoadedTargetsIds = notLoadedTargets,
    )
  }

  override fun getAllLoadedTargets(): List<BuildTargetInfo> =
    targetsStatusStorage.getLoadedTargets().mapNotNull { facade.getTargetInfoForTargetId(it) }

  override fun getAllNotLoadedTargets(): List<BuildTargetInfo> =
    targetsStatusStorage.getNotLoadedTargets().mapNotNull { facade.getTargetInfoForTargetId(it) }

  override fun clear() {
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.internal.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.project,
      magicMetaModelProjectConfig.isPythonSupportEnabled,
      magicMetaModelProjectConfig.isAndroidSupportEnabled,
    )

    workspaceModelUpdater.clear()
    targetsStatusStorage.clear()
  }

  override fun getLibraries(): List<Library> = libraries.orEmpty()

  override fun getDetailsForTargetId(targetId: BuildTargetId): Module? = facade.getModuleForTargetId(targetId)

  // TODO - test
  override fun toState(): DefaultMagicMetaModelState =
    DefaultMagicMetaModelState(
      facadeState = facade.toState(),
      // TODO: add serialization for more languages
      libraries = libraries?.map { it.toState() },
      overlappingTargetsGraph = overlappingTargetsGraph,
      targetsStatusStorageState = targetsStatusStorage.toState(),
    )

  internal fun loadStorage(storage: TargetsStatusStorage) {
    targetsStatusStorage = storage
  }

  public fun isPythonSupportEnabled(): Boolean = magicMetaModelProjectConfig.isPythonSupportEnabled

  private companion object {
    private val log = logger<MagicMetaModelImpl>()
  }
}

public data class TargetsStatusStorageState(
  public var allTargets: Collection<BuildTargetId> = emptyList(),
  public var loadedTargets: List<BuildTargetId> = emptyList(),
  public var notLoadedTargets: List<BuildTargetId> = emptyList(),
)

public class TargetsStatusStorage private constructor(
  private val allTargets: Collection<BuildTargetId>,
  private val loadedTargets: MutableSet<BuildTargetId>,
  private val notLoadedTargets: MutableSet<BuildTargetId>,
) {
  public constructor(allTargets: Collection<BuildTargetId>) : this(
    allTargets = allTargets,
    loadedTargets = mutableSetOf(),
    notLoadedTargets = allTargets.toMutableSet(),
  )

  public constructor(state: TargetsStatusStorageState) : this(
    allTargets = state.allTargets,
    loadedTargets = state.loadedTargets.toMutableSet(),
    notLoadedTargets = state.notLoadedTargets.toMutableSet(),
  )

  public fun clear() {
    loadedTargets.clear()
    notLoadedTargets.clear()
    notLoadedTargets.addAll(allTargets)
  }

  public fun addTargets(targets: Collection<BuildTargetId>) {
    loadedTargets.addAll(targets)
    notLoadedTargets.removeAll(targets.toSet())
  }

  public fun addTarget(target: BuildTargetId) {
    loadedTargets.add(target)
    notLoadedTargets.remove(target)
  }

  public fun removeTargets(targets: Collection<BuildTargetId>) {
    loadedTargets.removeAll(targets.toSet())
    notLoadedTargets.addAll(targets)
  }

  public fun isTargetNotLoaded(targetId: BuildTargetId): Boolean =
    notLoadedTargets.contains(targetId)

  public fun isTargetLoaded(targetId: BuildTargetId): Boolean =
    loadedTargets.contains(targetId)

  public fun getLoadedTargets(): List<BuildTargetId> =
    loadedTargets.toList()

  public fun getNotLoadedTargets(): List<BuildTargetId> =
    notLoadedTargets.toList()

  public fun toState(): TargetsStatusStorageState =
    TargetsStatusStorageState(
      allTargets,
      loadedTargets.toMutableList(),
      notLoadedTargets.toMutableList(),
    )

  public fun copy(): TargetsStatusStorage =
    TargetsStatusStorage(
      allTargets = allTargets.toList(),
      loadedTargets = loadedTargets.toMutableSet(),
      notLoadedTargets = notLoadedTargets.toMutableSet(),
    )
}

@TestOnly
public fun Collection<String>.toDefaultTargetsMap(): Map<BuildTargetId, BuildTargetInfo> =
  associateBy(
    keySelector = { it },
    valueTransform = { BuildTargetInfo(id = it) }
  )
