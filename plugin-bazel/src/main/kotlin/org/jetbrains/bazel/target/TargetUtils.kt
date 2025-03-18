package org.jetbrains.bazel.target

import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.utils.removeTrailingSlash
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.LibraryItem
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import kotlin.to

private const val MAX_EXECUTABLE_TARGET_IDS = 10

@ApiStatus.Internal
@Serializable
data class TargetUtilsState(
  val labelToTargetInfo: Map<Label, BuildTargetInfo> = emptyMap(),
  val moduleIdToTarget: Map<String, Label> = emptyMap(),
  val libraryIdToTarget: Map<String, Label> = emptyMap(),
  // we must use URI as comparing URI path strings is susceptible to errors.
  // e.g., file:/test and file:///test should be similar in the URI world
  val fileToTarget: Map<URI, List<Label>> = emptyMap(),
  val fileToExecutableTargets: Map<URI, List<Label>> = emptyMap(),
)

@PublicApi
@Service(Service.Level.PROJECT)
@State(
  name = "TargetUtils",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class TargetUtils(private val project: Project) : SerializablePersistentStateComponent<TargetUtilsState>(TargetUtilsState()) {
  // Not persisted!
  @ApiStatus.Internal
  private var libraryModulesLookupTable: HashSet<String> = hashSetOf()

  private var listeners: List<(Boolean) -> Unit> = emptyList()

  fun addFileToTargetIdEntry(uri: URI, targets: List<Label>) {
    updateState { state ->
      val fileToTarget = state.fileToTarget + (uri to targets)
      state.copy(fileToTarget = fileToTarget)
    }
  }

  fun removeFileToTargetIdEntry(uri: URI) {
    updateState { state ->
      val fileToTarget = state.fileToTarget - uri
      state.copy(fileToTarget = fileToTarget)
    }
  }

  @ApiStatus.Internal
  suspend fun saveTargets(
    targetIdToTargetInfo: Map<Label, BuildTargetInfo>,
    targetIdToModuleEntity: Map<Label, List<Module>>,
    fileToTarget: Map<URI, List<Label>>,
    libraryItems: List<LibraryItem>?,
    libraryModules: List<JavaModule>,
    nameProvider: TargetNameReformatProvider,
  ) {
    updateState { state ->
      val labelToTargetInfo = targetIdToTargetInfo.mapKeys { it.key }
      val moduleIdToTarget =
        targetIdToModuleEntity.entries.associate { (targetId, modules) ->
          modules.first().getModuleName() to targetId
        }
      val libraryIdToTarget =
        libraryItems
          ?.associate { library ->
            nameProvider.invoke(BuildTargetInfo(id = library.id)) to library.id
          }.orEmpty()

      val fileToExecutableTargets = calculateFileToExecutableTargets(libraryItems)
      state.copy(
        labelToTargetInfo = labelToTargetInfo,
        moduleIdToTarget = moduleIdToTarget,
        libraryIdToTarget = libraryIdToTarget,
        fileToTarget = fileToTarget,
        fileToExecutableTargets = fileToExecutableTargets,
      )
    }
    this.libraryModulesLookupTable = createLibraryModulesLookupTable(libraryModules)
  }

  private suspend fun calculateFileToExecutableTargets(libraryItems: List<LibraryItem>?): Map<URI, List<Label>> =
    withContext(Dispatchers.Default) {
      state.let { state ->
        val targetDependentsGraph = TargetDependentsGraph(state.labelToTargetInfo, libraryItems)
        val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<Label, Set<Label>>()
        state.fileToTarget
          .map { (uri, targets) ->
            async {
              val dependents =
                targets
                  .flatMap { label ->
                    calculateTransitivelyExecutableTargets(
                      targetToTransitiveRevertedDependenciesCache,
                      targetDependentsGraph,
                      label,
                    )
                  }.distinct()
              uri to dependents
            }
          }.awaitAll()
          .filter { it.second.isNotEmpty() } // Avoid excessive memory consumption
          .toMap()
      }
    }

  private fun calculateTransitivelyExecutableTargets(
    resultCache: ConcurrentHashMap<Label, Set<Label>>,
    targetDependentsGraph: TargetDependentsGraph,
    target: Label,
  ): Set<Label> =
    resultCache.getOrPut(target) {
      val targetInfo = state.labelToTargetInfo[target]
      if (targetInfo?.capabilities?.isExecutable() == true) {
        return@getOrPut setOf(target)
      }

      val directDependentIds = targetDependentsGraph.directDependentIds(target)
      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargets(resultCache, targetDependentsGraph, dependency)
        }.distinct()
        .take(MAX_EXECUTABLE_TARGET_IDS)
        .toSet()
    }

  private fun createLibraryModulesLookupTable(libraryModules: List<JavaModule>) =
    libraryModules.map { it.genericModuleInfo.name }.toHashSet()

  @ApiStatus.Internal
  fun fireSyncListeners(targetListChanged: Boolean) {
    listeners.forEach { it(targetListChanged) }
  }

  @ApiStatus.Internal
  fun registerSyncListener(listener: (targetListChanged: Boolean) -> Unit) {
    listeners += listener
  }

  fun allTargets(): List<Label> = state.labelToTargetInfo.keys.toList()

  fun getTargetsForURI(uri: URI): List<Label> = state.fileToTarget[uri] ?: emptyList()

  fun getTargetsForFile(file: VirtualFile): List<Label> =
    state.fileToTarget[file.url.removeTrailingSlash().safeCastToURI()]
      ?: getTargetsFromAncestorsForFile(file)

  @ApiStatus.Internal
  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val executableDirectTargets =
      getTargetsForFile(file).filter { label -> state.labelToTargetInfo[label]?.capabilities?.isExecutable() == true }
    if (executableDirectTargets.isEmpty()) {
      return state.fileToExecutableTargets.getOrDefault(file.url.removeTrailingSlash().safeCastToURI(), emptySet()).toList()
    }
    return executableDirectTargets
  }

  private fun getTargetsFromAncestorsForFile(file: VirtualFile): List<Label> {
    return if (BazelFeatureFlags.isRetrieveTargetsForFileFromAncestorsEnabled) {
      val rootDir = project.rootDir
      var iter = file.parent
      while (iter != null && VfsUtil.isAncestor(rootDir, iter, false)) {
        val key = iter.url.removeTrailingSlash().safeCastToURI()
        state.fileToTarget[key]?.let { return it }
        iter = iter.parent
      }
      emptyList()
    } else {
      emptyList()
    }
  }

  @PublicApi // // https://youtrack.jetbrains.com/issue/BAZEL-1632
  @Suppress("UNUSED")
  fun isLibrary(target: Label): Boolean = BuildTargetTag.LIBRARY in getBuildTargetInfoForLabel(target)?.tags.orEmpty()

  @PublicApi
  fun getTargetForModuleId(moduleId: String): Label? = state.moduleIdToTarget[moduleId]

  @PublicApi
  fun getTargetForLibraryId(libraryId: String): Label? = state.libraryIdToTarget[libraryId]

  @ApiStatus.Internal
  fun getBuildTargetInfoForLabel(label: Label): BuildTargetInfo? = state.labelToTargetInfo[label]

  @ApiStatus.Internal
  fun getBuildTargetInfoForModule(module: com.intellij.openapi.module.Module): BuildTargetInfo? =
    getTargetForModuleId(module.name)?.let { getBuildTargetInfoForLabel(it) }

  /**
   * [libraryModulesLookupTable] is not persisted between IDE restarts, use this method with caution.
   */
  @ApiStatus.Internal
  fun isLibraryModule(name: String): Boolean = name.addLibraryModulePrefix() in libraryModulesLookupTable
}

@ApiStatus.Internal
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun Label.getModule(project: Project): com.intellij.openapi.module.Module? =
  project.service<TargetUtils>().getBuildTargetInfoForLabel(this)?.getModule(project)

@PublicApi
fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

val com.intellij.openapi.module.Module.moduleEntity: ModuleEntity?
  @ApiStatus.Internal
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

@ApiStatus.Internal
fun BuildTargetInfo.getModule(project: Project): com.intellij.openapi.module.Module? {
  val moduleNameProvider = project.findNameProvider().orDefault()
  val moduleName = moduleNameProvider(this)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}

@ApiStatus.Internal
fun calculateFileToTarget(targetIdToModuleDetails: Map<Label, ModuleDetails>): Map<URI, List<Label>> =
  targetIdToModuleDetails.values
    .flatMap { it.toPairsUrlToId() }
    .groupBy { it.first }
    .mapValues { it.value.map { pair -> pair.second } }

private fun ModuleDetails.toPairsUrlToId(): List<Pair<URI, Label>> =
  sources.flatMap { sources ->
    sources.sources.map { it.uri.removeTrailingSlash().safeCastToURI() }.map { it to target.id }
  }
