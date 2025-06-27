package org.jetbrains.bazel.golang.startup

import com.goide.project.GoModuleSettings
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.startup.BazelProjectActivity
import org.jetbrains.bazel.sync.projectStructure.legacy.WorkspaceModuleUtils

class GoPostStartupActivity : BazelProjectActivity() {
  override suspend fun executeForBazelProject(project: Project) {
    WorkspaceModuleUtils.findModule(project)?.let {
      writeAction {
        GoModuleSettings.getInstance(it).isGoSupportEnabled = true
      }
    }
  }
}
