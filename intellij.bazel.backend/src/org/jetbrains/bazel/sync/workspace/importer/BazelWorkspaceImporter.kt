package org.jetbrains.bazel.sync.workspace.importer

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.workspace.storage.EntitySource
import com.intellij.platform.workspace.storage.ImmutableEntityStorage
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bsp.protocol.TaskId

/**
 * Extensible workspace importer, main purpose of [BazelWorkspaceImporter]
 * is to act as transform function from [WorkspaceSnapshot] to an actual IDE native
 * project model.
 *
 * Currently [BazelWorkspaceImporter] support pure transformation to [MutableEntityStorage] aka. Workspace Model.
 * Each importer can inspect full scope of [WorkspaceSnapshot], but shouldn't change its own behavior depending
 * on external factors like other language data that its own.
 *
 * **NOTE:** Each [BazelWorkspaceImporter] should aim to be a pure function,
 * importers should only realy on [WorkspaceSnapshot] as a source of truth for import input data
 *
 * **RECOMMENDATION:** Each [BazelWorkspaceImporter] should import exactly single language/technology,
 * in case of importer outputing model being shared among multiple languages/technologies, it's
 * importer responsiblity to provide extensions for extending its own behavior.
 */
@ApiStatus.Internal
interface BazelWorkspaceImporter {

  /**
   * Mark [BazelWorkspaceImporter] as named
   */
  interface Named : BazelWorkspaceImporter {

    /**
     * Internalized [BazelWorkspaceImporter] importer name
     */
    val importerName: @NlsContexts.ProgressTitle String
  }

  /**
   * Perform phased workspace import. Adhear to rules found in [WorkspaceImporterPhase] documentation.
   *
   * @param context Common workspace importer context
   * @param phase Current importer phase
   * @param snapshot Immutable workspace snapshot
   */
  // MAYBE RC: is Result<...> good fit here?
  suspend fun import(
    context: WorkspaceImporterContext, phase: WorkspaceImporterPhase,
    snapshot: WorkspaceSnapshot,
  ): Result<WorkspaceImporterResult>

  companion object {
    val EP_NAME: ExtensionPointName<BazelWorkspaceImporter> = ExtensionPointName("org.jetbrains.bazel.syncWorkspaceImporter")
  }
}

/**
 * Success result of [BazelWorkspaceImporter.import]
 */
@ApiStatus.Internal
sealed interface WorkspaceImporterResult {

  /**
   * Success [BazelWorkspaceImporter] execution shall be continued
   */
  data object Success : WorkspaceImporterResult

  /**
   * Abort [BazelWorkspaceImporter] execution, skip all next phases
   */
  data object Abort : WorkspaceImporterResult
}

/**
 * Common [BazelWorkspaceImporter] context
 *
 * @property project Current project, don't use [project] to collect data that can change [BazelWorkspaceImporter] behavior
 * @property taskConsole Current [TaskConsole]
 * @property progressReporter Current [SequentialProgressReporter]
 * @property taskId Importer specific [TaskId]
 * @property vfuManager [VirtualFileUrlManager] that shall be used for workspace model building
 */
@ApiStatus.Internal
data class WorkspaceImporterContext(
  val project: Project,
  val taskConsole: TaskConsole,
  val progressReporter: SequentialProgressReporter,
  val taskId: TaskId,
  val vfuManager: VirtualFileUrlManager,
  val currentSnapshot: ImmutableEntityStorage
)

/**
 * Phases of [BazelWorkspaceImporter]
 *
 * Phase call order
 * ```
 * Initialize -> WorkspaceApply -> Finalize
 * ```
 */
// RC: for now support only monolithic updates to shared `MutableEntityStorage`
@ApiStatus.Internal
sealed interface WorkspaceImporterPhase {

  /**
   * Called first once per [BazelWorkspaceImporter.import]
   */
  data object Initialize : WorkspaceImporterPhase

  /**
   * Apply changes to shared [MutableEntityStorage], remember that [builder] is shared among all [BazelWorkspaceImporter] instances,
   * so be careful with [MutableEntityStorage.replaceBySource].
   *
   * Called exactly once for every [BazelWorkspaceImporter].
   */
  data class WorkspaceApply(val builder: MutableEntityStorage, val entitySource: EntitySource) : WorkspaceImporterPhase

  /**
   * Called last once per [BazelWorkspaceImporter.import]
   */
  data object Finalize : WorkspaceImporterPhase

  /**
   * Optional phase called after [Finalize], depending on environment might not be invoked.
   * [WorkspaceImporterPhase.PostProcessing] phase will be applied after workspace model
   * application if concept of workspace model application is present in invocation environment.
   */
  data object PostProcessing : WorkspaceImporterPhase

}


