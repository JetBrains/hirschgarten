package org.jetbrains.bazel.ui.projectTree.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.runnerAction.TestTargetAction
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.Icon

//internal open class DebugAllTestsBaseAction(
//  text: () -> String,
//  icon: Icon,
//  val createAction: (project: Project, targets: List<BuildTarget>, directoryName: String) -> SuspendableAction,
//) : RunAllTestsBaseAction(text, icon, createAction) {
//  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
//    val actions =
//      readAction { getAllTestTargetInfos(project, e) }.map {
//        createAction(
//          project,
//          listOf(it),
//          e.getCurrentPath()?.name.orEmpty(),
//        )
//      }
//
//    actions.forEach {
//      it.actionPerformed(e)
//
//    }
//    ActionUtil.performActionDumbAwareWithCallbacks()
//  }
//}
internal class DebugAllTestsAction :
  RunAllTestsBaseAction(
    text = { BazelPluginBundle.message("action.debug.all.tests") },
    icon = AllIcons.Actions.StartDebugger,
    createAction = { project, targets, directoryName ->
      TestTargetAction(
        project,
        targets,
        text = {
          BazelPluginBundle.message("action.debug.all.tests.under", directoryName)
        },
        isDebugAction = true,
      )
    },
  )
