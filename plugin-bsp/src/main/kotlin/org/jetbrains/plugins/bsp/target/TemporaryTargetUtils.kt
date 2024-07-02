package org.jetbrains.plugins.bsp.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.plugins.bsp.config.BspFeatureFlags
import org.jetbrains.plugins.bsp.config.rootDir
import org.jetbrains.plugins.bsp.magicmetamodel.impl.BuildTargetInfoState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.LibraryState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.toState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.JavaModule
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Module
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.utils.safeCastToURI
import java.net.URI

public data class TemporaryTargetUtilsState(
  var idToTargetInfo: Map<String, BuildTargetInfoState> = emptyMap(),
  var moduleIdToBuildTargetId: Map<String, String> = emptyMap(),
  var fileToId: Map<String, List<String>> = emptyMap(),
  var targetsBaseDir: List<String> = emptyList(),
  var libraries: List<LibraryState> = emptyList(),
)

// This whole service is very temporary, it will be removed in the following PR
@Service(Service.Level.PROJECT)
@State(
  name = "TemporaryTargetUtils",
  storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
public class TemporaryTargetUtils : PersistentStateComponent<TemporaryTargetUtilsState> {
  private var targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo> = emptyMap()
  private var moduleIdToBuildTargetId: Map<String, BuildTargetIdentifier> = emptyMap()

  // we must use URI as comparing URI path strings is susceptible to errors.
  // e.g., file:/test and file:///test should be similar in the URI world
  private var fileToTargetId: Map<URI, List<BuildTargetIdentifier>> = hashMapOf()
  private var targetsBaseDir: Set<VirtualFile> = emptySet()
  private var libraries: List<Library> = emptyList()
  private var libraryModules: List<JavaModule> = emptyList()
  private var libraryModulesLookupTable: HashSet<String> = hashSetOf()

  private var listeners: List<() -> Unit> = emptyList()

  public fun saveTargets(
    targetIdToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo>,
    targetIdToModuleEntity: Map<BuildTargetIdentifier, Module>,
    targetIdToModuleDetails: Map<BuildTargetIdentifier, ModuleDetails>,
    libraries: List<Library>,
    libraryModules: List<JavaModule>,
  ) {
    this.targetIdToTargetInfo = targetIdToTargetInfo
    moduleIdToBuildTargetId = targetIdToModuleEntity.entries.associate { (targetId, module) ->
      module.getModuleName() to targetId
    }
    fileToTargetId = targetIdToModuleDetails.values.flatMap { it.toPairsUrlToId() }
      .groupBy { it.first }
      .mapValues { it.value.map { pair -> pair.second } }
    val virtualFileUrlManager = VirtualFileManager.getInstance()
    targetsBaseDir = this.targetIdToTargetInfo.values
      .mapNotNull { it.baseDirectory }
      .mapNotNull { virtualFileUrlManager.findFileByUrl(it) }
      .toSet()
    this.libraries = libraries
    this.libraryModules = libraryModules
    this.libraryModulesLookupTable = createLibraryModulesLookupTable()
  }

  private fun ModuleDetails.toPairsUrlToId(): List<Pair<URI, BuildTargetIdentifier>> =
    sources.flatMap { sources ->
      sources.sources.mapNotNull { it.uri.processUriString().safeCastToURI() }.map { it to target.id }
    }

  private fun String.processUriString() = this.trimEnd('/')

  private fun createLibraryModulesLookupTable() =
    libraryModules.map { it.genericModuleInfo.name }.toHashSet()

  public fun fireListeners() {
    listeners.forEach { it() }
  }

  public fun registerListener(listener: () -> Unit) {
    listeners += listener
  }

  public fun allTargetIds(): List<BuildTargetIdentifier> = targetIdToTargetInfo.keys.toList()

  public fun getTargetsForFile(file: VirtualFile, project: Project): List<BuildTargetIdentifier> =
    fileToTargetId[file.url.processUriString().safeCastToURI()]
      ?: getTargetsFromAncestorsForFile(file, project)

  private fun getTargetsFromAncestorsForFile(file: VirtualFile, project: Project): List<BuildTargetIdentifier> {
    return if (BspFeatureFlags.isRetrieveTargetsForFileFromAncestorsEnabled) {
      val rootDir = project.rootDir
      var iter = file.parent
      while (VfsUtil.isAncestor(rootDir, iter, false)) {
        val key = iter.url.processUriString().safeCastToURI()
        if (key in fileToTargetId) return fileToTargetId[key]!!
        iter = iter.parent
      }
      emptyList()
    } else emptyList()
  }

  public fun getTargetIdForModuleId(moduleId: String): BuildTargetIdentifier? = moduleIdToBuildTargetId[moduleId]

  public fun getBuildTargetInfoForId(buildTargetIdentifier: BuildTargetIdentifier): BuildTargetInfo? =
    targetIdToTargetInfo[buildTargetIdentifier]

  public fun isBaseDir(virtualFile: VirtualFile): Boolean =
    targetsBaseDir.contains(virtualFile)

  public fun getAllLibraries(): List<Library> = libraries

  public fun isLibraryModule(name: String): Boolean = name in libraryModulesLookupTable

  public fun getAllLibraryModules(): List<JavaModule> = libraryModules

  override fun getState(): TemporaryTargetUtilsState =
    TemporaryTargetUtilsState(
      idToTargetInfo = targetIdToTargetInfo.mapKeys { it.key.uri }.mapValues { it.value.toState() },
      moduleIdToBuildTargetId = moduleIdToBuildTargetId.mapValues { it.value.uri },
      fileToId = fileToTargetId.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.uri } },
      targetsBaseDir = targetsBaseDir.map { it.url }.toList(),
      libraries = libraries.map { it.toState() },
    )

  override fun loadState(state: TemporaryTargetUtilsState) {
    val virtualFileUrlManager = VirtualFileManager.getInstance()

    targetIdToTargetInfo = state.idToTargetInfo
      .mapKeys { BuildTargetIdentifier(it.key) }
      .mapValues { it.value.fromState() }
    moduleIdToBuildTargetId = state.moduleIdToBuildTargetId.mapValues { BuildTargetIdentifier(it.value) }
    fileToTargetId =
      state.fileToId.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { BuildTargetIdentifier(it) } }
    targetsBaseDir = state.targetsBaseDir.mapNotNull { virtualFileUrlManager.findFileByUrl(it) }.toSet()
    libraries = state.libraries.map { it.fromState() }
  }
}

public val Project.temporaryTargetUtils: TemporaryTargetUtils
  get() = service<TemporaryTargetUtils>()
