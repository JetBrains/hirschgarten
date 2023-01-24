package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.JavacOptionsItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.DefaultMagicMetaModelState
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl

public data class MagicMetaModelProjectConfig(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
)

public data class ProjectDetails(
  val targetsId: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
  val javacOptions: List<JavacOptionsItem>,
)

/**
 * Contains information about loaded target and not loaded targets for given document.
 *
 * @see [MagicMetaModel.getTargetsDetailsForDocument]
 */
public data class DocumentTargetsDetails(
  public val loadedTargetId: BuildTargetIdentifier?,
  public val notLoadedTargetsIds: List<BuildTargetIdentifier>,
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
   * Applies changes, should do it quickly - e.g. by using StorageReplacement
   */
  public fun applyOnWorkspaceModel(): Boolean
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
  public fun loadTarget(targetId: BuildTargetIdentifier): MagicMetaModelDiff?

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
  public fun getAllLoadedTargets(): List<BuildTarget>

  /**
   * Get all currently not loaded targets.
   */
  public fun getAllNotLoadedTargets(): List<BuildTarget>

  public fun clear()

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
