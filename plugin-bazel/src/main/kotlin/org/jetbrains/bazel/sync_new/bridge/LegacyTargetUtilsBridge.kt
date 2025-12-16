package org.jetbrains.bazel.sync_new.bridge

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.BazelSyncV2
import org.jetbrains.bazel.sync_new.flow.SyncStoreService
import org.jetbrains.bazel.sync_new.flow.index.SyncFileIndexService
import org.jetbrains.bazel.sync_new.flow.index.target_utils.TargetUtilsIndexService
import org.jetbrains.bazel.sync_new.languages_impl.jvm.index.JvmModuleSyncIndex
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path

class LegacyTargetUtilsBridge(private val project: Project) {
  private val targetUtilsIndexService = project.service<TargetUtilsIndexService>()
  private val syncFileIndexService = project.service<SyncFileIndexService>()
  private val jvmModuleSyncIndexService = project.service<JvmModuleSyncIndex>()
  private val syncStoreService = project.service<SyncStoreService>()

  val allTargetsLabels: List<String> = targetUtilsIndexService.getAllTargetsLabels()
  val allExecutableTargetLabels: List<String> = targetUtilsIndexService.getAllExecutableTargetsLabels()
  fun getAllTargets(): Sequence<Label> = targetUtilsIndexService.getAllTargets().asSequence()

  fun getTargetsForFile(file: Path): Sequence<Label> = syncFileIndexService.getTargetLabelsBySourceFile(file)
  fun getExecutableTargetsForFile(file: Path): Sequence<Label> = syncFileIndexService.getAllReverseExecutableTargetsBySourceFile(file)
  fun getTargetForModuleId(moduleId: String): Label? = jvmModuleSyncIndexService.getTargetForModuleId(moduleId)
  fun getTargetForLibraryId(libraryId: String): Label? = jvmModuleSyncIndexService.getTargetForLibraryId(libraryId)
  fun getBuildTargetForLabel(label: Label): BuildTarget? = targetUtilsIndexService.getBspBuildTarget(label)
  fun getAllBuildTargets(): Sequence<BuildTarget> {
    if (BazelSyncV2.disallowLegacyFullTargetGraphMaterialization) {
      throw IllegalStateException("full graph materialization is not allowed")
    }
    return syncStoreService.targetGraph
      .getAllVertexCompacts()
      .mapNotNull { targetUtilsIndexService.getBspBuildTarget(it.label) }
  }
}
