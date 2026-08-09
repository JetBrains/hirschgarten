package org.jetbrains.bazel.ui.gutters

import com.intellij.execution.Location
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.LineMarkerActionWrapper
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DataSink
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.BuildTarget

internal fun getExecutorActions(project: Project, target: BuildTarget): List<AnAction> =
  getExecutorActions(BazelRunLocation(project, target))

@ApiStatus.Internal
fun getExecutorActions(location: Location<*>): List<AnAction> {
  val executorActions = ExecutorAction.getActions()
  return executorActions.map { executorAction ->
    object : LineMarkerActionWrapper(location.psiElement, executorAction) {
      override fun dataSnapshot(sink: DataSink) {
        sink[Location.DATA_KEY] = location
      }
    }
  }
}
