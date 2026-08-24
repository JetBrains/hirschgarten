package org.jetbrains.bazel.golang.sync

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.gazelleTarget
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.server.connection
import org.jetbrains.bazel.sync.ProjectPreSyncHook
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bsp.protocol.RunParams

internal val GAZELLE_TARGET_OVERRIDE_KEY: Key<Label> = Key.create("GAZELLE_TARGET_OVERRIDE_KEY")

internal class RunGazelleTargetPreSyncHook : ProjectPreSyncHook {
  override fun isEnabled(project: Project): Boolean = BazelFeatureFlags.isGoSupportEnabled

  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val project = environment.project
    val gazelleTarget = project.getUserData(GAZELLE_TARGET_OVERRIDE_KEY)
                        ?: environment.project.projectView().gazelleTarget
                        ?: return
    environment.withSubtask("Run Gazelle target") { taskId ->
      project.connection.runWithServer(taskId) { server ->
        val runParams =
          RunParams(
            target = gazelleTarget,
            taskId = taskId,
            checkVisibility = true,
          )
        val buildTargetRun = server.buildTargetRun(runParams)
        if (buildTargetRun.statusCode == BazelStatus.BUILD_ERROR) {
          project.syncConsole.addDiagnosticMessage(
            taskId = taskId,
            message = "Specify the correct gazelle_target: in your selected .bazelproject",
            severity = MessageEvent.Kind.WARNING,
          )
        }
      }
    }
  }
}
