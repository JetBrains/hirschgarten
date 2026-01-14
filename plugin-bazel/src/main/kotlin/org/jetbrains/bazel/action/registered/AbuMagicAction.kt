package org.jetbrains.bazel.action.registered

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bsp.protocol.JoinedBuildServer

class AbuMagicAction :
  SuspendableAction(
    "Abu magic action",
    AllIcons.Providers.MongoDB,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    project.connection.runWithServer { bspServer ->
      doStuff(project, bspServer)
    }
  }

  /** If server-side magic is needed - [org.jetbrains.bazel.server.sync.AbuMagicQuery] */
  private suspend fun abuMagicQuery(server: JoinedBuildServer) = server.abuMagicQuery()

  private suspend fun doStuff(project: Project, server: JoinedBuildServer) {
    // stuff to do
  }
}
