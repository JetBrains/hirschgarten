package org.jetbrains.bazel.target

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.util.xmlb.annotations.OptionTag
import com.intellij.workspaceModel.ide.impl.legacyBridge.module.findModuleEntity
import com.intellij.workspaceModel.ide.legacyBridge.ModuleBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.TestOnly
import org.jetbrains.bazel.annotations.InternalApi
import org.jetbrains.bazel.annotations.PublicApi
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.gson.bazelGson
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

private const val MAX_EXECUTABLE_TARGET_IDS = 10

@InternalApi
data class TargetUtilsState(
  // The new tag is here to not break the sync with the old persisted data
  // The project that contains the old persisted data will be resynced and stored using the new tag
  // https://youtrack.jetbrains.com/issue/BAZEL-1967
  @OptionTag(tag = "labelToTargetInfoV2")
  var labelToTargetInfo: Map<String, String> = emptyMap(),
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
  /**
   * All labels in [TargetUtils] are canonical.
   * When querying [labelToTargetInfo] (e.g., via [getBuildTargetForLabel]) the label must be first canonicalized via [toCanonicalLabel].
   */
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

  fun addFileToTargetIdEntry(path: Path, targets: List<Label>) {
    fileToTarget = fileToTarget + (path to targets)
  }

  fun removeFileToTargetIdEntry(path: Path) {
    fileToTarget = fileToTarget - path
  }

  @InternalApi
  @TestOnly
  fun setTargets(labelToTargetInfo: Map<Label, BuildTarget>) {
    this.labelToTargetInfo = labelToTargetInfo
    updateComputedFields()
  }

  @InternalApi
  suspend fun saveTargets(
    targets: List<BuildTarget>,
    fileToTarget: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>?,
  ) {
    labelToTargetInfo = targets.associateBy { it.id }
    moduleIdToTarget = labelToTargetInfo.keys.associateBy { it.formatAsModuleName(project) }
    libraryIdToTarget =
      libraryItems
        ?.associate { library ->
          library.id.formatAsModuleName(project) to library.id
        }.orEmpty()

    this.fileToTarget = fileToTarget
    fileToExecutableTargets = calculateFileToExecutableTargets(libraryItems)

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
      if (targetInfo?.kind?.isExecutable == true) {
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

  private fun updateComputedFields() {
    allTargetsAndLibrariesLabels = (allTargets() + allLibraries()).map { it.toShortString(project) }
  }

  @PublicApi
  fun allTargets(): List<Label> = labelToTargetInfo.keys.toList()

  @PublicApi
  fun allLibraries(): List<Label> = libraryIdToTarget.values.toList()

  @PublicApi
  fun getTargetsForPath(path: Path): List<Label> = fileToTarget[path] ?: emptyList()

  @PublicApi
  fun getTargetsForFile(file: VirtualFile): List<Label> = file.toNioPathOrNull()?.let { getTargetsForPath(it) } ?: emptyList()

  @InternalApi
  fun getExecutableTargetsForFile(file: VirtualFile): List<Label> {
    val executableDirectTargets =
      getTargetsForFile(file).filter { label -> labelToTargetInfo[label]?.kind?.isExecutable == true }
    if (executableDirectTargets.isEmpty()) {
      return fileToExecutableTargets.getOrDefault(file.toNioPathOrNull(), emptySet()).toList()
    }
    return executableDirectTargets
  }

  @PublicApi
  fun isLibrary(target: Label): Boolean = getBuildTargetForLabel(target)?.kind?.ruleType == RuleType.LIBRARY

  @PublicApi
  fun getTargetForModuleId(moduleId: String): Label? = moduleIdToTarget[moduleId]

  @PublicApi
  fun getTargetForLibraryId(libraryId: String): Label? = libraryIdToTarget[libraryId]

  @InternalApi
  fun getBuildTargetForLabel(label: Label): BuildTarget? = label.toCanonicalLabel(project)?.let { labelToTargetInfo[it] }

  @InternalApi
  fun getBuildTargetForModule(module: com.intellij.openapi.module.Module): BuildTarget? =
    getTargetForModuleId(module.name)?.let { getBuildTargetForLabel(it) }

  @InternalApi
  fun allBuildTargets(): List<BuildTarget> = labelToTargetInfo.values.toList()

  @InternalApi
  override fun getState(): TargetUtilsState =
    TargetUtilsState(
      labelToTargetInfo =
        labelToTargetInfo
          .mapKeys {
            it.key.toString()
          }.mapValues {
            // don't store dependencies, sources, resources as they are saved in the workspace model already
            bazelGson.toJson(it.value.copy(dependencies = emptyList(), sources = emptyList(), resources = emptyList()))
          },
      moduleIdToTarget = moduleIdToTarget.mapValues { it.value.toString() },
      libraryIdToTarget = libraryIdToTarget.mapValues { it.value.toString() },
      fileToTarget = fileToTarget.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
      fileToExecutableTargets = fileToExecutableTargets.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.toString() } },
    )

  @InternalApi
  override fun loadState(state: TargetUtilsState) {
    try {
      labelToTargetInfo =
        state.labelToTargetInfo
          .mapKeys { Label.parse(it.key) }
          .mapValues { bazelGson.fromJson(it.value, BuildTarget::class.java) }
      moduleIdToTarget = state.moduleIdToTarget.mapValues { Label.parse(it.value) }
      libraryIdToTarget = state.libraryIdToTarget.mapValues { Label.parse(it.value) }
      fileToTarget =
        state.fileToTarget.mapKeys { o -> o.key.toNioPathOrNull()!! }.mapValues { o -> o.value.map { Label.parse(it) } }
      fileToExecutableTargets =
        state.fileToExecutableTargets.mapKeys { o -> o.key.toNioPathOrNull()!! }.mapValues { o -> o.value.map { Label.parse(it) } }
      updateComputedFields()
    } catch (e: Exception) {
      log.warn(e)
      labelToTargetInfo = emptyMap()
      moduleIdToTarget = emptyMap()
      libraryIdToTarget = emptyMap()
      fileToTarget = emptyMap()
      fileToExecutableTargets = emptyMap()
      allTargetsAndLibrariesLabels = emptyList()
      updateComputedFields()
    }
  }

  companion object {
    val log = logger<TargetUtils>()
  }
}

@InternalApi
fun String.addLibraryModulePrefix() = "_aux.libraries.$this"

@PublicApi
val Project.targetUtils: TargetUtils
  get() = service<TargetUtils>()

@PublicApi
fun Label.getModule(project: Project): com.intellij.openapi.module.Module? =
  project.targetUtils.getBuildTargetForLabel(this)?.getModule(project)

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
  val moduleName = this.id.formatAsModuleName(project)
  return ModuleManager.getInstance(project).findModuleByName(moduleName)
}
