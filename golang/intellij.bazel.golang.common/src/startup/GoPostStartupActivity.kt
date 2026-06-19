package org.jetbrains.bazel.golang.startup

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.golang.workspace.GoWorkspaceModuleUtil
import org.jetbrains.bazel.startup.utils.BazelProjectActivity

internal class GoPostStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    GoWorkspaceModuleUtil.findModule(project)?.let {
      edtWriteAction {
        GoModuleSettings.getInstance(it).isGoSupportEnabled = true
      }
    }
  }
}
