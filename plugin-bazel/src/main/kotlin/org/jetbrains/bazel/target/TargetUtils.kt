package org.jetbrains.bazel.target

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.bazel.magicmetamodel.findNameProvider
import org.jetbrains.bazel.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.bazel.workspacemodel.entities.JavaModule
import org.jetbrains.bazel.workspacemodel.entities.Module
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetTag
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.isExecutable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.to

private const val MAX_EXECUTABLE_TARGET_IDS = 10

@InternalApi
data class TargetUtilsState(
  var labelToTargetInfo: Map<String, BuildTargetState> = emptyMap(),
  var moduleIdToTarget: Map<String, String> = emptyMap(),
  var libraryIdToTarget: Map<String, String> = emptyMap(),
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
  @InternalApi
  var labelToTargetInfo: Map<Label, BuildTarget> = emptyMap()
    private set
  private var moduleIdToTarget: Map<String, Label> = emptyMap()

  private var libraryIdToTarget: Map<String, Label> = emptyMap()

  var fileToTarget: Map<Path, List<Label>> = hashMapOf()
    private set

  private var fileToExecutableTargets: Map<Path, List<Label>> = hashMapOf()

  // Everything below this comment is not persisted!
  var allTargetsAndLibrariesLabels: List<String> = emptyList()
    private set

    @InternalApi get

  private var libraryModulesLookupTable: HashSet<String> = hashSetOf()

  private var listeners: List<(Boolean) -> Unit> = emptyList()

  fun addFileToTargetIdEntry(path: Path, targets: List<Label>) {
    fileToTarget = fileToTarget + (path to targets)
  }

  fun removeFileToTargetIdEntry(path: Path) {
    fileToTarget = fileToTarget - path
  }

  @InternalApi
  suspend fun saveTargets(
    targetIdToTargetInfo: Map<Label, BuildTarget>,
    targetIdToModuleEntity: Map<Label, List<Module>>,
    fileToTarget: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>?,
    libraryModules: List<JavaModule>,
    nameProvider: TargetNameReformatProvider,
  ) {
    this.labelToTargetInfo = targetIdToTargetInfo.mapKeys { it.key }
    moduleIdToTarget =
      targetIdToModuleEntity.entries.associate { (targetId, modules) ->
        modules.first().getModuleName() to targetId
      }
    libraryIdToTarget =
      libraryItems
        ?.associate { library ->
          nameProvider.invoke(library.id) to library.id
        }.orEmpty()

    this.fileToTarget = fileToTarget
    fileToExecutableTargets = calculateFileToExecutableTargets(libraryItems)

    this.libraryModulesLookupTable = createLibraryModulesLookupTable(libraryModules)
    updateComputedFields()
  }

  @InternalApi
  fun addTargets(targetInfos: List<BuildTarget>) {
    labelToTargetInfo = labelToTargetInfo + targetInfos.associateBy { it.id }
    updateComputedFields()
  }

  private suspend fun calculateFileToExecutableTargets(libraryItems: List<LibraryItem>?): Map<Path, List<Label>> =
    withContext(Dispatchers.Default) {
      val targetDependentsGraph = TargetDependentsGraph(labelToTargetInfo, libraryItems)
      val targetToTransitiveRevertedDependenciesCache = ConcurrentHashMap<Label, Set<Label>>()
      fileToTarget
        .map { (path, targets) ->
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
            path to dependents
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
      if (targetInfo?.capabilities?.isExecutable == true) {
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

  private fun updateComputedFields() {
    allTargetsAndLibrariesLabels = (allTargets() + allLibraries()).map { it.toShortString(project) }
  }

  @InternalApi
  fun fireSyncListeners(targetListChanged: Boolean) {
    listeners.forEach { it(targetListChanged) }
  }

  @InternalApi
  fun registerSyncListener(listener: (targetListChanged: Boolean) -> Unit) {
    listeners += listener
  }

  @PublicApi
  fun allTargets(): List<Label> = labelToTargetInfo.keys.toList()

  @PublicApi
  fun allLibraries(): List<Label> = libraryIdToTarget.values.toList()

  @PublicApi
  fun getTargetsForPath(path: Path): List<Label> = fileToTarget[path] ?: emptyList()

  @PublicApi
  fun getTargetsForFile(file: VirtualFile): List<Label> =
    fileToTarget[file.toNioPath()]
      ?: getTargetsFromAncestorsForFile(file)

  @InternalApi
  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val executableDirectTargets =
      getTargetsForFile(file).filter { label -> labelToTargetInfo[label]?.capabilities?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return fileToExecutableTargets.getOrDefault(file.toNioPath(), emptySet()).toList()
    }
    return executableDirectTargets
  }

  private fun getTargetsFromAncestorsForFile(file: VirtualFile): List<Label> {
    return if (BazelFeatureFlags.isRetrieveTargetsForFileFromAncestorsEnabled) {
      val rootDir = project.rootDir
      var iter = file.parent
      while (iter != null && VfsUtil.isAncestor(rootDir, iter, false)) {
        val key = iter.toNioPath()
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
  fun isLibrary(target: Label): Boolean = BuildTargetTag.LIBRARY in getBuildTargetForLabel(target)?.tags.orEmpty()

  @PublicApi
  fun getTargetForModuleId(moduleId: String): Label? = moduleIdToTarget[moduleId]

  @PublicApi
  fun getTargetForLibraryId(libraryId: String): Label? = libraryIdToTarget[libraryId]

  @InternalApi
  fun getBuildTargetForLabel(label: Label): BuildTarget? = labelToTargetInfo[label]

  @InternalApi
  fun getBuildTargetForModule(module: com.intellij.openapi.module.Module): BuildTarget? =
    getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  /**
   * [libraryModulesLookupTable] is not persisted between IDE restarts, use this method with caution.
   */
  @InternalApi
  fun isLibraryModule(name: String): Boolean = name.addLibraryModulePrefix() in libraryModulesLookupTable

  @InternalApi
  override fun getState(): TargetUtilsState =
    TargetUtilsState(
      labelToTargetInfo = labelToTargetInfo.mapKeys { it.key.toString() }.mapValues { it.value.toState() },
      moduleIdToTarget = moduleIdToTarget.mapValues { it.value.toString() },
      libraryIdToTarget = libraryIdToTarget.mapValues { it.value.toString() },
      fileToTarget = fileToTarget.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
      fileToExecutableTargets = fileToExecutableTargets.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
    )

  @InternalApi
  override fun loadState(state: TargetUtilsState) {
    labelToTargetInfo =
      state.labelToTargetInfo
        .mapKeys { Label.parse(it.key) }
        .mapValues { it.value.fromState() }
    moduleIdToTarget = state.moduleIdToTarget.mapValues { Label.parse(it.value) }
    libraryIdToTarget = state.libraryIdToTarget.mapValues { Label.parse(it.value) }
    fileToTarget =
      state.fileToTarget.mapKeys { o -> o.key.toNioPathOrNull()!! }.mapValues { o -> o.value.map { Label.parse(it) } }
    fileToExecutableTargets =
      state.fileToExecutableTargets.mapKeys { o -> o.key.toNioPathOrNull()!! }.mapValues { o -> o.value.map { Label.parse(it) } }
    updateComputedFields()
  }
}

@InternalApi
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun Label.getModule(project: Project): com.intellij.openapi.module.Module? =
  project.service<TargetUtils>().getBuildTargetForLabel(this)?.getModule(project)

@PublicApi
fun Label.getModuleEntity(project: Project): ModuleEntity? = getModule(project)?.moduleEntity

val com.intellij.openapi.module.Module.moduleEntity: ModuleEntity?
  @InternalApi
  get() {
    val bridge = this as? ModuleBridge ?: return null
    return bridge.findModuleEntity(bridge.entityStorage.current)
  }

@InternalApi
fun BuildTarget.getModule(project: Project): com.intellij.openapi.module.Module? {
  val moduleNameProvider = project.findNameProvider()
  val moduleName = moduleNameProvider(this.id)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}

@InternalApi
fun calculateFileToTarget(targetIdToModuleDetails: Map<Label, ModuleDetails>): Map<Path, List<Label>> =
  targetIdToModuleDetails.values
    .flatMap { it.toPairsPathToId() }
    .groupBy { it.first }
    .mapValues { it.value.map { pair -> pair.second } }

private fun ModuleDetails.toPairsPathToId(): List<Pair<Path, Label>> = target.sources.map { it.path }.map { it to target.id }
