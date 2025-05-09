package org.jetbrains.bazel.target.sync.projectStructure

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.projectStructure.AllProjectStructuresDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureDiff
import org.jetbrains.bazel.sync.projectStructure.ProjectStructureProvider
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.ui.console.syncConsole
import org.jetbrains.bazel.ui.console.withSubtask
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path

class TargetUtilsProjectStructureDiff : ProjectStructureDiff {
  var bspTargets: List<BuildTarget> = emptyList()
  var fileToTarget: Map<Path, List<Label>> = emptyMap()
  var libraryItems: List<LibraryItem>? = null

  override suspend fun apply(
    project: Project,
    syncScope: ProjectSyncScope,
    taskId: String,
  ) {
    project.syncConsole.withSubtask(
      taskId = taskId,
      subtaskId = "apply-changes-on-target-utils",
      message = "XD",
    ) {
      project.targetUtils.saveTargets(
        targets = bspTargets,
        fileToTarget = fileToTarget,
        libraryItems = libraryItems,
      )
    }
  }
}

val AllProjectStructuresDiff.targetUtilsDiff: TargetUtilsProjectStructureDiff
  get() = diffOfType(TargetUtilsProjectStructureDiff::class.java)

class TargetUtilsProjectStructureProvider : ProjectStructureProvider<TargetUtilsProjectStructureDiff> {
  override fun newDiff(project: Project): TargetUtilsProjectStructureDiff = TargetUtilsProjectStructureDiff()
}
