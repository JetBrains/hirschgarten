package org.jetbrains.bazel.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.BuildTargetTag
import com.intellij.openapi.components.PersistentStateComponent
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
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.config.BspFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.magicmetamodel.orDefault
import org.jetbrains.bazel.utils.safeCastToURI
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.LibraryItem
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private const val MAX_EXECUTABLE_TARGET_IDS = 10

@ApiStatus.Internal
data class TargetUtilsState(
  var idToTargetInfo: Map<String, BuildTargetInfoState> = emptyMap(),
  var moduleIdToBuildTargetId: Map<String, String> = emptyMap(),
  var fileToId: Map<String, List<String>> = emptyMap(),
  var fileToExecutableTargetIds: Map<String, List<String>> = emptyMap(),
)

@PublicApi
@Service(Service.Level.PROJECT)
@State(
  name = "TargetUtils",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class TargetUtils(private val project: Project) : PersistentStateComponent<TargetUtilsState> {
  @ApiStatus.Internal
  var targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo> = emptyMap()
    private set
  private var moduleIdToBuildTargetId: Map<String, BuildTargetIdentifier> = emptyMap()

  // we must use URI as comparing URI path strings is susceptible to errors.
  // e.g., file:/test and file:///test should be similar in the URI world
  var fileToTargetId: Map<URI, List<BuildTargetIdentifier>> = hashMapOf()
    private set

  private var fileToExecutableTargetIds: Map<URI, List<BuildTargetIdentifier>> = hashMapOf()

  private var libraryModulesLookupTable: HashSet<String> = hashSetOf()

  private var listeners: List<(Boolean) -> Unit> = emptyList()

  @ApiStatus.Internal
  suspend fun saveTargets(
    targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>,
    targetIdToModuleEntity: Map<BuildTargetIdentifier, Module>,
    targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
    libraryItems: List<LibraryItem>?,
    libraryModules: List<JavaModule>,
  ) {
    this.targetIdToTargetInfo = targetIdToTargetInfo
    moduleIdToBuildTargetId =
      targetIdToModuleEntity.entries.associate { (targetId, module) ->
        module.getModuleName() to targetId
      }
    fileToTargetId =
      targetIdToModuleDetails.values
        .flatMap { it.toPairsUrlToId() }
        .groupBy { it.first }
        .mapValues { it.value.map { pair -> pair.second } }
    fileToExecutableTargetIds = calculateFileToExecutableTargetIds(libraryItems)

    this.libraryModulesLookupTable = createLibraryModulesLookupTable(libraryModules)
  }

  private fun ModuleDetails.toPairsUrlToId(): List<Pair<URI, BuildTargetIdentifier>> =
    sources.flatMap { sources ->
      sources.sources.mapNotNull { it.uri.processUriString().safeCastToURI() }.map { it to target.id }
    }

  private fun String.processUriString() = this.trimEnd('/')

  private suspend fun calculateFileToExecutableTargetIds(libraryItems: List<LibraryItem>?): Map<URI, List<BuildTargetIdentifier>> =
    withContext(Dispatchers.Default) {
      val targetDependentsGraph = TargetDependentsGraph(targetIdToTargetInfo, libraryItems)
      val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<BuildTargetIdentifier, Set<BuildTargetIdentifier>>()
      fileToTargetId
        .map { (uri, targetIds) ->
          async {
            val dependents =
              targetIds
                .flatMap { targetId ->
                  calculateTransitivelyExecutableTargetIds(
                    targetToTransitiveRevertedDependenciesCache,
                    targetDependentsGraph,
                    targetId,
                  )
                }.distinct()
            uri to dependents
          }
        }.awaitAll()
        .filter { it.second.isNotEmpty() } // Avoid excessive memory consumption
        .toMap()
    }

  private fun calculateTransitivelyExecutableTargetIds(
    resultCache: ConcurrentHashMap<BuildTargetIdentifier, Set<BuildTargetIdentifier>>,
    targetDependentsGraph: TargetDependentsGraph,
    targetId: BuildTargetIdentifier,
  ): Set<BuildTargetIdentifier> =
    resultCache.getOrPut(targetId) {
      val targetInfo = targetIdToTargetInfo[targetId]
      if (targetInfo?.capabilities?.isExecutable() == true) {
        return@getOrPut setOf(targetId)
      }

      val directDependentIds = targetDependentsGraph.directDependentIds(targetId)
      return@getOrPut directDependentIds
        .asSequence()
        .flatMap { dependency ->
          calculateTransitivelyExecutableTargetIds(resultCache, targetDependentsGraph, dependency)
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

  fun allTargetIds(): List<BuildTargetIdentifier> = targetIdToTargetInfo.keys.toList()

  fun getTargetsForFile(file: VirtualFile): List<BuildTargetIdentifier> =
    fileToTargetId[file.url.processUriString().safeCastToURI()]
      ?: getTargetsFromAncestorsForFile(file)

  @ApiStatus.Internal
  fun getExecutableTargetsForFile(file: VirtualFile): List<BuildTargetIdentifier> {
    val executableDirectTargets =
      getTargetsForFile(file).filter { targetId -> targetIdToTargetInfo[targetId]?.capabilities?.isExecutable() == true }
    if (executableDirectTargets.isEmpty()) {
      return fileToExecutableTargetIds.getOrDefault(file.url.processUriString().safeCastToURI(), emptySet()).toList()
    }
    return executableDirectTargets
  }

  private fun getTargetsFromAncestorsForFile(file: VirtualFile): List<BuildTargetIdentifier> {
    return if (BspFeatureFlags.isRetrieveTargetsForFileFromAncestorsEnabled) {
      val rootDir = project.rootDir
      var iter = file.parent
      while (iter != null && VfsUtil.isAncestor(rootDir, iter, false)) {
        val key = iter.url.processUriString().safeCastToURI()
        if (key in fileToTargetId) return fileToTargetId[key]!!
        iter = iter.parent
      }
      emptyList()
    } else {
      emptyList()
    }
  }

  @PublicApi // // https://youtrack.jetbrains.com/issue/BAZEL-1632
  @Suppress("UNUSED")
  fun isLibrary(id: BuildTargetIdentifier): Boolean = BuildTargetTag.LIBRARY in getBuildTargetInfoForId(id)?.tags.orEmpty()

  @ApiStatus.Internal
  fun getTargetIdForModuleId(moduleId: String): BuildTargetIdentifier? = moduleIdToBuildTargetId[moduleId]

  @ApiStatus.Internal
  fun getBuildTargetInfoForId(buildTargetIdentifier: BuildTargetIdentifier): BuildTargetInfo? = targetIdToTargetInfo[buildTargetIdentifier]

  @ApiStatus.Internal
  fun getBuildTargetInfoForModule(module: com.intellij.openapi.module.Module): BuildTargetInfo? =
    getTargetIdForModuleId(module.name)?.let { getBuildTargetInfoForId(it) }

  @ApiStatus.Internal
  fun isLibraryModule(name: String): Boolean = name.addLibraryModulePrefix() in libraryModulesLookupTable

  @ApiStatus.Internal
  override fun getState(): TargetUtilsState =
    TargetUtilsState(
      idToTargetInfo = targetIdToTargetInfo.mapKeys { it.key.uri }.mapValues { it.value.toState() },
      moduleIdToBuildTargetId = moduleIdToBuildTargetId.mapValues { it.value.uri },
      fileToId = fileToTargetId.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.uri } },
      fileToExecutableTargetIds = fileToExecutableTargetIds.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.uri } },
    )

  @ApiStatus.Internal
  override fun loadState(state: TargetUtilsState) {
    targetIdToTargetInfo =
      state.idToTargetInfo
        .mapKeys { BuildTargetIdentifier(it.key) }
        .mapValues { it.value.fromState() }
    moduleIdToBuildTargetId = state.moduleIdToBuildTargetId.mapValues { BuildTargetIdentifier(it.value) }
    fileToTargetId =
      state.fileToId.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { BuildTargetIdentifier(it) } }
    fileToExecutableTargetIds =
      state.fileToExecutableTargetIds.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { BuildTargetIdentifier(it) } }
  }
}

@ApiStatus.Internal
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun BuildTargetIdentifier.getModule(project: Project): com.intellij.openapi.module.Module? =
  project.service<TargetUtils>().getBuildTargetInfoForId(this)?.getModule(project)

@PublicApi
fun BuildTargetIdentifier.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

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
