package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ScalacOptionsItem
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bsp.LibraryItem
import org.jetbrains.bsp.WorkspaceDirectoriesResult
import org.jetbrains.bsp.WorkspaceInvalidTargetsResult
import org.jetbrains.magicmetamodel.impl.DefaultMagicMetaModelState
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.Library
import org.jetbrains.magicmetamodel.impl.workspacemodel.Module
import java.nio.file.Path

public data class MagicMetaModelProjectConfig(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBasePath: Path,
  val project: Project,
  val moduleNameProvider: ModuleNameProvider,
  val isPythonSupportEnabled: Boolean,
  val hasDefaultPythonInterpreter: Boolean,
  val isAndroidSupportEnabled: Boolean,
) {
  public constructor(
    workspaceModel: WorkspaceModel,
    virtualFileUrlManager: VirtualFileUrlManager,
    moduleNameProvider: ModuleNameProvider?,
    projectBasePath: Path,
    project: Project,
    isPythonSupportEnabled: Boolean = false,
    hasDefaultPythonInterpreter: Boolean = false,
    isAndroidSupportEnabled: Boolean = false,
  ) : this(
    workspaceModel,
    virtualFileUrlManager,
    projectBasePath,
    project,
    moduleNameProvider ?: DefaultModuleNameProvider,
    isPythonSupportEnabled,
    hasDefaultPythonInterpreter,
    isAndroidSupportEnabled
  )
}

public typealias ModuleNameProvider = (BuildTargetInfo) -> String

public object DefaultModuleNameProvider : ModuleNameProvider {
  override fun invoke(targetInfo: BuildTargetInfo): String = targetInfo.id
}

public data class ProjectDetails(
  val targetsId: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: List<JavacOptionsItem>,
  val scalacOptions: List<ScalacOptionsItem>,
  val pythonOptions: List<PythonOptionsItem>,
  val outputPathUris: List<String>,
  val libraries: List<LibraryItem>?,
  val directories: WorkspaceDirectoriesResult = WorkspaceDirectoriesResult(emptyList(), emptyList()),
  val invalidTargets: WorkspaceInvalidTargetsResult = WorkspaceInvalidTargetsResult(emptyList()),
  var defaultJdkName: String? = null,
)

/**
 * Contains information about loaded target and not loaded targets for given document.
 *
 * @see [MagicMetaModel.getTargetsDetailsForDocument]
 */
public data class DocumentTargetsDetails(
  public val loadedTargetId: BuildTargetId?,
  public val notLoadedTargetsIds: List<BuildTargetId>,
)

/**
 * Each operation that changes the state of the MagicMetaModel should
 * return the diff in order to apply the changes on the WorkspaceModel.
 * It's super useful for heavy operations - heavy calculations should be done in the background
 * task and the diff should be the outcome, and then the diff (it should be just a StorageReplacement)
 * can be applied on the WorkspaceModel quickly on the UI thread.
 */
public interface MagicMetaModelDiff {
  /**
   * Applies changes, should do it quickly - e.g. by using MutableEntityStorage.replaceBySource
   */
  public suspend fun applyOnWorkspaceModel()
}

/**
 * Provides operations on model entries.
 *
 * The main reason for its existence is the integration of the model obtained from
 * the BSP containing shared sources and [WorkspaceModel].
 */
public interface MagicMetaModel {
  /**
   * Loads default targets to the model - can be all targets, can be subset of them.
   *
   * If the project contains shared sources, the loaded targets should not share sources.
   * For example, if there are :target1 and :target2 that have the same sources, *only one of them* could be loaded.
   *
   * Requires write action if used with [WorkspaceModel].
   */
  public fun loadDefaultTargets(): MagicMetaModelDiff

  /**
   * Loads given target.
   *
   * If the project contains shared sources, all "overlapping" targets should be unloaded.
   * For example, if there are :target1 and :target2 that have the same sources,
   * and :target1 is currently loaded and :target2 is loaded then after this call *:target1 should no longer be loaded*.
   *
   * Requires write action if used with [WorkspaceModel].
   */
  public fun loadTarget(targetId: BuildTargetId): MagicMetaModelDiff?

  /**
   * Loads given target and all its dependencies.
   *
   * If the project contains shared sources, all "overlapping" targets should be unloaded.
   * For example, if there are :target1 and :target2 that have the same sources,
   * and :target1 is currently loaded and :target2 is loaded then after this call *:target1 should no longer be loaded*.
   *
   * Requires write action if used with [WorkspaceModel].
   */
  public fun loadTargetWithDependencies(targetId: BuildTargetId): MagicMetaModelDiff?

  /**
   * Register a function to be executed when a target has been loaded
   */
  public fun registerTargetLoadListener(function: () -> Unit)

  /**
   * Get targets details for given document.
   *
   * The response contains a loaded target that contains the document,
   * or null if no loaded target contains the document.
   * The response also contains unloaded targets that contain the document.
   */
  public fun getTargetsDetailsForDocument(documentId: TextDocumentIdentifier): DocumentTargetsDetails

  /**
   * Get all currently loaded targets.
   */
  public fun getAllLoadedTargets(): List<BuildTargetInfo>

  /**
   * Get all currently not loaded targets.
   */
  public fun getAllNotLoadedTargets(): List<BuildTargetInfo>

  /**
   * Get ids of all currently invalid targets.
   */
  public fun getAllInvalidTargets(): List<BuildTargetId>

  public fun clear()

  public fun getLibraries(): List<Library>

  public fun getDetailsForTargetId(targetId: BuildTargetId): Module?

  public companion object {
    private val log = logger<MagicMetaModel>()

    /**
     * Create instance of [MagicMetaModelImpl] which supports shared sources
     * provided by the BSP and works on top of [WorkspaceModel].
     */
    public fun create(
      magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
      projectDetails: ProjectDetails,
    ): MagicMetaModelImpl {
      log.debug { "Creating MagicMetaModelImpl for $magicMetaModelProjectConfig, $projectDetails..." }
      return MagicMetaModelImpl(magicMetaModelProjectConfig, projectDetails)
    }

    /**
     * Creates a new instance of MagicMetaModel from state.
     */
    public fun fromState(
      state: DefaultMagicMetaModelState,
      magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
    ): MagicMetaModelImpl =
      MagicMetaModelImpl(state, magicMetaModelProjectConfig)
  }
}
