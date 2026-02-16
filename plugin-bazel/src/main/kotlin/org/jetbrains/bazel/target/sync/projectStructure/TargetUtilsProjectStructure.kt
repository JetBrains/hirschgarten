package org.jetbrains.bazel.target.sync.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureProvider
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

class TargetUtilsProjectStructureDiff : ProjectStructureDiff {
  var bspTargets: List<RawBuildTarget> = emptyList()
  var fileToTarget: Map<Path, List<Label>> = emptyMap()
  var libraryItems: List<LibraryItem> = emptyList()

  override suspend fun apply(
    project: Project,
    syncScope: ProjectSyncScope,
    taskId: String,
  ) {
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "apply-changes-on-target-utils",
      message = BazelPluginBundle.message("console.task.apply.changes.on.target.utils.message"),
    ) {
      project.targetUtils.saveTargets(
        targets = bspTargets,
        fileToTarget = fileToTarget,
        libraryItems = libraryItems,
      )
      project.messageBus.syncPublisher(SyncStatusListener.TOPIC).targetUtilAvailable()
    }
  }
}

val AllProjectStructuresDiff.targetUtilsDiff: TargetUtilsProjectStructureDiff
  get() = diffOfType(TargetUtilsProjectStructureDiff::class.java)

class TargetUtilsProjectStructureProvider : ProjectStructureProvider<TargetUtilsProjectStructureDiff> {
  override fun newDiff(project: Project): TargetUtilsProjectStructureDiff = TargetUtilsProjectStructureDiff()
}
