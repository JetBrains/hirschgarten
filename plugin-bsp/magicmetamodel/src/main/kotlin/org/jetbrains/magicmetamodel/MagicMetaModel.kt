package org.jetbrains.magicmetamodel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.workspaceModel.ide.WorkspaceModel
import com.intellij.workspaceModel.storage.url.VirtualFileUrlManager
import org.jetbrains.magicmetamodel.impl.MagicMetaModelImpl
import java.nio.file.Path

public data class MagicMetaModelProjectConfig(
  val workspaceModel: WorkspaceModel,
  val virtualFileUrlManager: VirtualFileUrlManager,
  val projectBaseDir: Path,
)

public data class ProjectDetails(
  val targetsId: List<BuildTargetIdentifier>,
  val targets: Set<BuildTarget>,
  val sources: List<SourcesItem>,
  val resources: List<ResourcesItem>,
  val dependenciesSources: List<DependencySourcesItem>,
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
 * Provides operations on model entries.
 *
 * The main reason for its existence is the integration of the model obtained from
 * the BSP containing shared sources and [WorkspaceModel].
 */
public interface MagicMetaModel {

  /**
   * TODO
   */
  public fun save()

  /**
   * Loads default targets to the model - can be all targets, can be subset of them.
   *
   * If the project contains shared sources, the loaded targets should not share sources.
   * For example, if there are :target1 and :target2 that have the same sources, *only one of them* could be loaded.
   *
   * Requires write action if used with [WorkspaceModel].
   */
  public fun loadDefaultTargets()

  /**
   * Loads given target.
   *
   * If the project contains shared sources, all "overlapping" targets should be unloaded.
   * For example, if there are :target1 and :target2 that have the same sources,
   * and :target1 is currently loaded and :target2 is loaded then after this call *:target1 should no longer be loaded*.
   *
   * Requires write action if used with [WorkspaceModel].
   */
  public fun loadTarget(targetId: BuildTargetIdentifier)

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

  public companion object {
    private val LOGGER = logger<MagicMetaModel>()

    /**
     * Create instance of [MagicMetaModelImpl] which supports shared sources
     * provided by the BSP and works on top of [WorkspaceModel].
     */
    public fun create(
      magicMetaModelProjectConfig: MagicMetaModelProjectConfig,
      projectDetails: ProjectDetails,
    ): MagicMetaModel {
      LOGGER.debug { "Creating MagicMetaModelImpl for $magicMetaModelProjectConfig, $projectDetails..." }
      return MagicMetaModelImpl(magicMetaModelProjectConfig, projectDetails)
    }
  }
}
