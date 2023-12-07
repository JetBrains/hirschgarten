package org.jetbrains.magicmetamodel.impl

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.platform.backend.workspace.BuilderSnapshot
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import org.jetbrains.bsp.DirectoryItem
import org.jetbrains.bsp.LibraryItem
import org.jetbrains.bsp.WorkspaceDirectoriesResult
import org.jetbrains.bsp.WorkspaceInvalidTargetsResult
import org.jetbrains.magicmetamodel.DocumentTargetsDetails
import org.jetbrains.magicmetamodel.MagicMetaModel
import org.jetbrains.magicmetamodel.MagicMetaModelDiff
import org.jetbrains.magicmetamodel.MagicMetaModelProjectConfig
import org.jetbrains.magicmetamodel.ProjectDetails
import org.jetbrains.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.ContentRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.GenericModuleInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.magicmetamodel.impl.workspacemodel.ModuleName
import org.jetbrains.magicmetamodel.impl.workspacemodel.PythonModule
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelToModulesMapTransformer
import org.jetbrains.magicmetamodel.impl.workspacemodel.WorkspaceModelUpdater
import org.jetbrains.magicmetamodel.impl.workspacemodel.impl.updaters.isRoot
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.toPair
import java.net.URI
import kotlin.io.path.name
import kotlin.io.path.toPath

internal class DefaultMagicMetaModelDiff(
  private val workspaceModel: WorkspaceModel,
  private val builderSnapshot: BuilderSnapshot,
  private val mmmStorageReplacement: LoadedTargetsStorage,
  private val mmmInstance: MagicMetaModelImpl,
  private val targetLoadListeners: Set<() -> Unit>,
) : MagicMetaModelDiff {
  override suspend fun applyOnWorkspaceModel() {
    val storageReplacement = builderSnapshot.getStorageReplacement()
    writeAction {
      if (workspaceModel.replaceProjectModel(storageReplacement)) {
        mmmInstance.loadStorage(mmmStorageReplacement)
        targetLoadListeners.forEach { it() }
      }
    }
  }
}

// TODO - get rid of *Impl - we should name it 'DefaultMagicMetaModel' or something like that
/**
 * Basic implementation of [MagicMetaModel] supporting shared sources
 * provided by the BSP and build on top of [WorkspaceModel].
 */
public class MagicMetaModelImpl : MagicMetaModel, ConvertableToState<DefaultMagicMetaModelState> {
  private val magicMetaModelProjectConfig: MagicMetaModelProjectConfig
  private val targets: Map<BuildTargetId, BuildTargetInfo>
  private val libraries: List<Library>?
  private val directories: WorkspaceDirectoriesResult?
  private val invalidTargets: WorkspaceInvalidTargetsResult

  private val targetsDetailsForDocumentProvider: TargetsDetailsForDocumentProvider
  private val overlappingTargetsGraph: Map<BuildTargetId, Set<BuildTargetId>>

  private val targetIdToModule: Map<BuildTargetId, Module>

  private var loadedTargetsStorage: LoadedTargetsStorage

  private val targetLoadListeners = mutableSetOf<() -> Unit>()

  // TODO ITS SUUPER UGLY I'LL MAKE IT NICER BUT WE NEED TO FIX THE ISSUE ASAP
  private val excludedPaths: List<String>

  internal constructor(
    magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    projectDetails: ProjectDetails,
  ) {
    log.debug { "Initializing MagicMetaModelImpl with: $magicMetaModelProjectConfig and $projectDetails..." }

    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig

    targets = projectDetails.targets.associate { it.toBuildTargetInfo().toPair() }

    this.targetsDetailsForDocumentProvider = logPerformance("create-target-details-for-document-provider") {
      TargetsDetailsForDocumentProvider(projectDetails.sources)
    }
    this.overlappingTargetsGraph = logPerformance("create-overlapping-targets-graph") {
      OverlappingTargetsGraph(targetsDetailsForDocumentProvider)
    }

    this.targetIdToModule = logPerformance("create-target-id-to-module-entities-map") {
      TargetIdToModuleEntitiesMap(
        projectDetails,
        magicMetaModelProjectConfig.projectBasePath,
        magicMetaModelProjectConfig.moduleNameProvider,
        magicMetaModelProjectConfig.hasDefaultPythonInterpreter,
      )
    }

    this.libraries = logPerformance("create-libraries") {
      createLibraries(projectDetails.libraries)
    }

    this.directories = projectDetails.directories
    this.invalidTargets = projectDetails.invalidTargets

    this.loadedTargetsStorage = logPerformance("create-loaded-targets-storage") {
      LoadedTargetsStorage(targetIdToModule.keys)
    }

    this.excludedPaths = projectDetails.outputPathUris

    log.debug { "Initializing MagicMetaModelImpl done!" }
  }

  private fun createLibraries(libraries: List<LibraryItem>?) = libraries?.map {
    Library(
      displayName = it.id.uri,
      classJars = it.jars,
      sourceJars = it.sourceJars,
    )
  }

  internal constructor(state: DefaultMagicMetaModelState, magicMetaModelProjectConfig: MagicMetaModelProjectConfig) {
    this.magicMetaModelProjectConfig = magicMetaModelProjectConfig
    this.loadedTargetsStorage =
      LoadedTargetsStorage(state.loadedTargetsStorageState)

    this.targetsDetailsForDocumentProvider =
      TargetsDetailsForDocumentProvider(state.targetsDetailsForDocumentProviderState)
    this.overlappingTargetsGraph = state.overlappingTargetsGraph

    this.targets = state.targets.associate { it.fromState().toPair() }

    this.libraries = state.libraries?.map { it.fromState() }
    this.directories = null // workspace model keeps info about them
    this.invalidTargets = WorkspaceInvalidTargetsResult(emptyList())

    val unloadedTargets = state.unloadedTargets.mapValues { it.value.fromState() }
    this.targetIdToModule = WorkspaceModelToModulesMapTransformer(
      magicMetaModelProjectConfig.workspaceModel,
      loadedTargetsStorage,
      magicMetaModelProjectConfig.moduleNameProvider,
    ) + unloadedTargets

    this.excludedPaths = state.excludedPaths
  }

  override fun loadDefaultTargets(): MagicMetaModelDiff {
    log.debug { "Calculating default targets to load..." }

    val nonOverlappingTargetsToLoad = logPerformance("compute-non-overlapping-targets") {
      NonOverlappingTargets(targets.values.toHashSet(), overlappingTargetsGraph)
    }

    log.debug { "Calculating default targets to load done! Targets to load: $nonOverlappingTargetsToLoad" }

    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.isPythonSupportEnabled
    )

    workspaceModelUpdater.clear()
    val newStorage = loadedTargetsStorage.copy()
    newStorage.clear()

    val modulesToLoad = getModulesForTargetsToLoad(nonOverlappingTargetsToLoad)
    val rootModule = rootModuleToLoadIfNeeded()

    logPerformance("load-modules") {
      workspaceModelUpdater.loadModules(modulesToLoad)
      workspaceModelUpdater.loadLibraries(libraries.orEmpty())
      workspaceModelUpdater.loadDirectories()

      if (rootModule != null) {
        workspaceModelUpdater.loadModule(rootModule)
      }
    }
    newStorage.addTargets(nonOverlappingTargetsToLoad)

    return DefaultMagicMetaModelDiff(
      workspaceModel = magicMetaModelProjectConfig.workspaceModel,
      builderSnapshot = builderSnapshot,
      mmmStorageReplacement = newStorage,
      mmmInstance = this,
      targetLoadListeners = targetLoadListeners,
    )
  }

  // TODO what if null?
  private fun getModulesForTargetsToLoad(targetsToLoad: Collection<BuildTargetId>): List<Module> =
    targetsToLoad.map { targetIdToModule[it]!! }

  private fun rootModuleToLoadIfNeeded(): Module? =
    if (!isRootModuleIncluded() && isProjectNonEmpty()) rootModuleToLoad() else null

  private fun isRootModuleIncluded(): Boolean =
    targetIdToModule.values.any { it.doesIncludeRootDir() }

  private fun Module.doesIncludeRootDir() =
    when (this) {
      is JavaModule -> isRoot(magicMetaModelProjectConfig.projectBasePath)
      is PythonModule -> sourceRoots.any { it.sourcePath == magicMetaModelProjectConfig.projectBasePath }
      else -> false
    }

  private fun isProjectNonEmpty(): Boolean =
    targets.isNotEmpty()

  private fun rootModuleToLoad(): Module {
    val genericModuleInfo = GenericModuleInfo(
      name = magicMetaModelProjectConfig.projectBasePath.name,
      type = "JAVA_MODULE",
      modulesDependencies = emptyList(),
      librariesDependencies = emptyList(),
    )

    return JavaModule(
      genericModuleInfo = genericModuleInfo,
      baseDirContentRoot = ContentRoot(
        path = magicMetaModelProjectConfig.projectBasePath,
        excludedPaths = excludedPaths.map { URI.create(it).toPath() },
      ),
      sourceRoots = emptyList(),
      resourceRoots = emptyList(),
      moduleLevelLibraries = null,
      compilerOutput = null,
      jvmJdkName = null,
    )
  }

  private fun WorkspaceModelUpdater.loadDirectories() {
    if (directories != null) {
      val includedDirectories = directories.includedDirectories.map { it.toVirtualFileUrl() }
      val excludedDirectories = directories.excludedDirectories.map { it.toVirtualFileUrl() }

      loadDirectories(includedDirectories, excludedDirectories)
    }
  }

  private fun DirectoryItem.toVirtualFileUrl(): VirtualFileUrl =
    magicMetaModelProjectConfig.virtualFileUrlManager.fromUrl(uri)

  override fun registerTargetLoadListener(function: () -> Unit) {
    targetLoadListeners.add(function)
  }

  public fun copyAllTargetLoadListenersTo(other: MagicMetaModelImpl) {
    targetLoadListeners.forEach { other.registerTargetLoadListener(it) }
  }

  override fun loadTarget(targetId: BuildTargetId): MagicMetaModelDiff? = when {
    loadedTargetsStorage.isTargetNotLoaded(targetId) -> doLoadTarget(targetId)
    else -> null
  }

  private fun BuildTargetId.isTargetLoaded() = loadedTargetsStorage.isTargetLoaded(this)

  private fun BuildTargetId.isTargetNotLoaded() = loadedTargetsStorage.isTargetNotLoaded(this)

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
        targets.contains(el) && !toProcess.contains(el) && !processed.contains(el) && !conflicts.contains(el)
      }

    do {
      processFirst()
        ?.let { targets[it] }
        ?.let {
          toProcess.addAll(nonConflictingDependencies(it))
        }
    } while (toProcess.isNotEmpty())
    return doLoadTargets(processed, conflicts)
  }

  private fun doLoadTargets(
    targets: Collection<BuildTargetId>,
    conflicts: HashSet<BuildTargetId>,
  ): DefaultMagicMetaModelDiff {
    val targetsToLoad = targets.filter { it.isTargetNotLoaded() }
    val loadedTargetsToRemove = conflicts.filter { it.isTargetLoaded() }

    val modulesToRemove = loadedTargetsToRemove.map {
      ModuleName(magicMetaModelProjectConfig.moduleNameProvider(it))
    }
    val modulesToAdd = targetsToLoad.mapNotNull { targetIdToModule[it] }

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

    val modulesToRemove = loadedTargetsToRemove.map {
      ModuleName(magicMetaModelProjectConfig.moduleNameProvider(it))
    }
    // TODO null!!!
    val moduleToAdd = targetIdToModule[targetId]!!

    return modifyModel { newStorage, workspaceModelUpdater ->
      workspaceModelUpdater.removeModules(modulesToRemove)
      newStorage.removeTargets(loadedTargetsToRemove)

      workspaceModelUpdater.loadModule(moduleToAdd)
      newStorage.addTarget(targetId)
    }
  }

  private fun modifyModel(
    action: (targetStorage: LoadedTargetsStorage, updater: WorkspaceModelUpdater) -> Unit,
  ): DefaultMagicMetaModelDiff {
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.isPythonSupportEnabled
    )
    val newStorage = loadedTargetsStorage.copy()
    action(newStorage, workspaceModelUpdater)
    return DefaultMagicMetaModelDiff(
      workspaceModel = magicMetaModelProjectConfig.workspaceModel,
      builderSnapshot = builderSnapshot,
      mmmStorageReplacement = newStorage,
      mmmInstance = this,
      targetLoadListeners = targetLoadListeners,
    )
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

  override fun getAllLoadedTargets(): List<BuildTargetInfo> =
    targets.values.filter { it.id.isTargetLoaded() }

  override fun getAllNotLoadedTargets(): List<BuildTargetInfo> =
    targets.values.filter { loadedTargetsStorage.isTargetNotLoaded(it.id) }

  override fun getAllInvalidTargets(): List<BuildTargetId> =
    invalidTargets.targets.map { it.uri }

  override fun clear() {
    val builderSnapshot = magicMetaModelProjectConfig.workspaceModel.getBuilderSnapshot()
    val workspaceModelUpdater = WorkspaceModelUpdater.create(
      builderSnapshot.builder,
      magicMetaModelProjectConfig.virtualFileUrlManager,
      magicMetaModelProjectConfig.projectBasePath,
      magicMetaModelProjectConfig.isPythonSupportEnabled
    )

    workspaceModelUpdater.clear()
    loadedTargetsStorage.clear()
  }

  override fun getLibraries(): List<Library> = libraries.orEmpty()

  // TODO - test
  override fun toState(): DefaultMagicMetaModelState =
    DefaultMagicMetaModelState(
      targets = targets.values.map { it.toState() },
      // TODO: add serialization for more languages
      libraries = libraries?.map { it.toState() },
      unloadedTargets = targetIdToModule.filterNot { it.key.isTargetLoaded() }
        .mapValues { it.value.toState() },
      targetsDetailsForDocumentProviderState = targetsDetailsForDocumentProvider.toState(),
      overlappingTargetsGraph = overlappingTargetsGraph,
      loadedTargetsStorageState = loadedTargetsStorage.toState(),
    )

  internal fun loadStorage(storage: LoadedTargetsStorage) {
    loadedTargetsStorage = storage
  }

  public fun isPythonSupportEnabled(): Boolean = magicMetaModelProjectConfig.isPythonSupportEnabled

  private companion object {
    private val log = logger<MagicMetaModelImpl>()
  }
}

public data class LoadedTargetsStorageState(
  public var allTargets: Collection<BuildTargetId> = emptyList(),
  public var loadedTargets: List<BuildTargetId> = emptyList(),
  public var notLoadedTargets: List<BuildTargetId> = emptyList(),
)

public class LoadedTargetsStorage private constructor(
  private val allTargets: Collection<BuildTargetId>,
  private val loadedTargets: MutableSet<BuildTargetId>,
  private val notLoadedTargets: MutableSet<BuildTargetId>,
) {
  public constructor(allTargets: Collection<BuildTargetId>) : this(
    allTargets = allTargets,
    loadedTargets = mutableSetOf(),
    notLoadedTargets = allTargets.toMutableSet(),
  )

  public constructor(state: LoadedTargetsStorageState) : this(
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

  public fun toState(): LoadedTargetsStorageState =
    LoadedTargetsStorageState(
      allTargets,
      loadedTargets.toMutableList(),
      notLoadedTargets.toMutableList(),
    )

  public fun copy(): LoadedTargetsStorage =
    LoadedTargetsStorage(
      allTargets = allTargets.toList(),
      loadedTargets = loadedTargets.toMutableSet(),
      notLoadedTargets = notLoadedTargets.toMutableSet(),
    )
}
