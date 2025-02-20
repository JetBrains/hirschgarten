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
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
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
  var labelToTargetInfo: Map<String, BuildTargetInfoState> = emptyMap(),
  var moduleIdToTarget: Map<String, String> = emptyMap(),
  var fileToTarget: Map<String, List<String>> = emptyMap(),
  var fileToExecutableTargets: Map<String, List<String>> = emptyMap(),
)

@PublicApi
@Service(Service.Level.PROJECT)
@State(
  name = "TargetUtils",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
class TargetUtils(private val project: Project) : PersistentStateComponent<TargetUtilsState> {
  @ApiStatus.Internal
  var labelToTargetInfo: Map<Label, BuildTargetInfo> = emptyMap()
    private set
  private var moduleIdToTarget: Map<String, Label> = emptyMap()

  // we must use URI as comparing URI path strings is susceptible to errors.
  // e.g., file:/test and file:///test should be similar in the URI world
  var fileToTarget: Map<URI, List<Label>> = hashMapOf()
    private set

  private var fileToExecutableTargets: Map<URI, List<Label>> = hashMapOf()

  private var libraryModulesLookupTable: HashSet<String> = hashSetOf()

  private var listeners: List<(Boolean) -> Unit> = emptyList()

  fun addFileToTargetIdEntry(uri: URI, targets: List<Label>) {
    fileToTarget = fileToTarget + (uri to targets)
  }

  fun removeFileToTargetIdEntry(uri: URI) {
    fileToTarget = fileToTarget - uri
  }

  @ApiStatus.Internal
  suspend fun saveTargets(
    targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>,
    targetIdToModuleEntity: Map<BuildTargetIdentifier, Module>,
    targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
    libraryItems: List<LibraryItem>?,
    libraryModules: List<JavaModule>,
  ) {
    this.labelToTargetInfo = targetIdToTargetInfo.mapKeys { it.key.label() }
    moduleIdToTarget =
      targetIdToModuleEntity.entries.associate { (targetId, module) ->
        module.getModuleName() to targetId.label()
      }
    fileToTarget =
      targetIdToModuleDetails.values
        .flatMap { it.toPairsUrlToId() }
        .groupBy { it.first }
        .mapValues { it.value.map { pair -> pair.second } }
    fileToExecutableTargets = calculateFileToExecutableTargets(libraryItems)

    this.libraryModulesLookupTable = createLibraryModulesLookupTable(libraryModules)
  }

  private fun ModuleDetails.toPairsUrlToId(): List<Pair<URI, Label>> =
    sources.flatMap { sources ->
      sources.sources.mapNotNull { it.uri.processUriString().safeCastToURI() }.map { it to target.id.label() }
    }

  private fun String.processUriString() = this.trimEnd('/')

  private suspend fun calculateFileToExecutableTargets(libraryItems: List<LibraryItem>?): Map<URI, List<Label>> =
    withContext(Dispatchers.Default) {
      val targetDependentsGraph = TargetDependentsGraph(labelToTargetInfo, libraryItems)
      val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<Label, Set<Label>>()
      fileToTarget
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

  private fun calculateTransitivelyExecutableTargets(
    resultCache: ConcurrentHashMap<Label, Set<Label>>,
    targetDependentsGraph: TargetDependentsGraph,
    target: Label,
  ): Set<Label> =
    resultCache.getOrPut(target) {
      val targetInfo = labelToTargetInfo[target]
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

  fun allTargets(): List<Label> = labelToTargetInfo.keys.toList()

  fun getTargetsForURI(uri: URI): List<Label> = fileToTarget[uri] ?: emptyList()

  fun getTargetsForFile(file: VirtualFile): List<Label> =
    fileToTarget[file.url.processUriString().safeCastToURI()]
      ?: getTargetsFromAncestorsForFile(file)

  @ApiStatus.Internal
  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val executableDirectTargets =
      getTargetsForFile(file).filter { label -> labelToTargetInfo[label]?.capabilities?.isExecutable() == true }
    if (executableDirectTargets.isEmpty()) {
      return fileToExecutableTargets.getOrDefault(file.url.processUriString().safeCastToURI(), emptySet()).toList()
    }
    return executableDirectTargets
  }

  private fun getTargetsFromAncestorsForFile(file: VirtualFile): List<Label> {
    return if (BspFeatureFlags.isRetrieveTargetsForFileFromAncestorsEnabled) {
      val rootDir = project.rootDir
      var iter = file.parent
      while (iter != null && VfsUtil.isAncestor(rootDir, iter, false)) {
        val key = iter.url.processUriString().safeCastToURI()
        if (key in fileToTarget) return fileToTarget[key]!!
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
  fun getTargetForModuleId(moduleId: String): Label? = moduleIdToTarget[moduleId]

  @ApiStatus.Internal
  fun getBuildTargetInfoForLabel(label: Label): BuildTargetInfo? = labelToTargetInfo[label]

  @ApiStatus.Internal
  fun getBuildTargetInfoForModule(module: com.intellij.openapi.module.Module): BuildTargetInfo? =
    getTargetForModuleId(module.name)?.let { getBuildTargetInfoForLabel(it) }

  @ApiStatus.Internal
  fun isLibraryModule(name: String): Boolean = name.addLibraryModulePrefix() in libraryModulesLookupTable

  @ApiStatus.Internal
  override fun getState(): TargetUtilsState =
    TargetUtilsState(
      labelToTargetInfo = labelToTargetInfo.mapKeys { it.key.toString() }.mapValues { it.value.toState() },
      moduleIdToTarget = moduleIdToTarget.mapValues { it.value.toString() },
      fileToTarget = fileToTarget.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
      fileToExecutableTargets = fileToExecutableTargets.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
    )

  @ApiStatus.Internal
  override fun loadState(state: TargetUtilsState) {
    labelToTargetInfo =
      state.labelToTargetInfo
        .mapKeys { Label.parse(it.key) }
        .mapValues { it.value.fromState() }
    moduleIdToTarget = state.moduleIdToTarget.mapValues { Label.parse(it.value) }
    fileToTarget =
      state.fileToTarget.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { Label.parse(it) } }
    fileToExecutableTargets =
      state.fileToExecutableTargets.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { Label.parse(it) } }
  }
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
