package org.jetbrains.plugins.bsp.target

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.plugins.bsp.magicmetamodel.TargetNameReformatProvider
import org.jetbrains.plugins.bsp.magicmetamodel.impl.BuildTargetInfoState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.LibraryState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.PerformanceLogger.logPerformance
import org.jetbrains.plugins.bsp.magicmetamodel.impl.toState
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleDetails
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.impl.updaters.transformers.ProjectDetailsToModuleDetailsTransformer
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBuildTargetInfo
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
  private var idToTargetInfo: Map<BuildTargetIdentifier, BuildTargetInfo> = emptyMap()
  private var moduleIdToBuildTargetId: Map<String, BuildTargetIdentifier> = emptyMap()

  // we must use URI as comparing URI path strings is susceptible to errors.
  // e.g., file:/test and file:///test should be similar in the URI world
  private var fileToId: Map<URI, List<BuildTargetIdentifier>> = hashMapOf()
  private var targetsBaseDir: Set<VirtualFile> = emptySet()
  private var libraries: List<Library> = emptyList()

  private var listeners: List<() -> Unit> = emptyList()

  public fun saveTargets(
    targetIds: List<BuildTargetIdentifier>,
    transformer: ProjectDetailsToModuleDetailsTransformer,
    libraries: List<LibraryItem>?,
    moduleNameProvider: TargetNameReformatProvider,
    libraryNameProvider: TargetNameReformatProvider,
  ) {
    val modulesDetails = targetIds.map { transformer.moduleDetailsForTargetId(it) }
    val virtualFileUrlManager = VirtualFileManager.getInstance()

    idToTargetInfo = modulesDetails.associate { it.target.id to it.target.toBuildTargetInfo() }
    moduleIdToBuildTargetId = modulesDetails
      .associate { moduleNameProvider(it.target.toBuildTargetInfo()) to it.target.id }
    fileToId = modulesDetails.flatMap { it.toPairsUrlToId() }
      .groupBy { it.first }
      .mapValues { it.value.map { pair -> pair.second } }
    targetsBaseDir = idToTargetInfo.values
      .mapNotNull { it.baseDirectory }
      .mapNotNull { virtualFileUrlManager.findFileByUrl(it) }
      .toSet()
    this.libraries = logPerformance("create-libraries") {
      createLibraries(libraries, libraryNameProvider)
    }
  }

  private fun ModuleDetails.toPairsUrlToId(): List<Pair<URI, BuildTargetIdentifier>> =
    sources.flatMap { sources ->
      sources.sources.mapNotNull { it.uri.processUriString().safeCastToURI() }.map { it to target.id }
    }

  private fun String.processUriString() = this.trimEnd('/')

  private fun createLibraries(
    libraries: List<LibraryItem>?,
    libraryNameProvider: TargetNameReformatProvider,
  ): List<Library> =
    libraries?.map {
      Library(
        displayName = libraryNameProvider(BuildTargetInfo(id = it.id.uri)),
        iJars = it.ijars,
        classJars = it.jars,
        sourceJars = it.sourceJars,
      )
    }.orEmpty()

  public fun fireListeners() {
    listeners.forEach { it() }
  }

  public fun registerListener(listener: () -> Unit) {
    listeners += listener
  }

  public fun allTargetIds(): List<BuildTargetIdentifier> = idToTargetInfo.keys.toList()

  public fun getTargetsForFile(file: VirtualFile): List<BuildTargetIdentifier> =
    fileToId[file.url.processUriString().safeCastToURI()]
      ?: fileToId[file.parent.url.processUriString().safeCastToURI()].orEmpty()

  public fun getTargetIdForModuleId(moduleId: String): BuildTargetIdentifier? = moduleIdToBuildTargetId[moduleId]

  public fun getBuildTargetInfoForId(buildTargetIdentifier: BuildTargetIdentifier): BuildTargetInfo? =
    idToTargetInfo[buildTargetIdentifier]

  public fun isBaseDir(virtualFile: VirtualFile): Boolean =
    targetsBaseDir.contains(virtualFile)

  public fun getAllLibraries(): List<Library> = libraries

  override fun getState(): TemporaryTargetUtilsState =
    TemporaryTargetUtilsState(
      idToTargetInfo = idToTargetInfo.mapKeys { it.key.uri }.mapValues { it.value.toState() },
      moduleIdToBuildTargetId = moduleIdToBuildTargetId.mapValues { it.value.uri },
      fileToId = fileToId.mapKeys { o -> o.key.toString() }.mapValues { o -> o.value.map { it.uri } },
      targetsBaseDir = targetsBaseDir.map { it.url }.toList(),
      libraries = libraries.map { it.toState() },
    )

  override fun loadState(state: TemporaryTargetUtilsState) {
    val virtualFileUrlManager = VirtualFileManager.getInstance()

    idToTargetInfo = state.idToTargetInfo.mapKeys { BuildTargetIdentifier(it.key) }.mapValues { it.value.fromState() }
    moduleIdToBuildTargetId = state.moduleIdToBuildTargetId.mapValues { BuildTargetIdentifier(it.value) }
    fileToId =
      state.fileToId.mapKeys { o -> o.key.safeCastToURI() }.mapValues { o -> o.value.map { BuildTargetIdentifier(it) } }
    targetsBaseDir = state.targetsBaseDir.mapNotNull { virtualFileUrlManager.findFileByUrl(it) }.toSet()
    libraries = state.libraries.map { it.fromState() }
  }
}

public val Project.temporaryTargetUtils: TemporaryTargetUtils
  get() = service<TemporaryTargetUtils>()
