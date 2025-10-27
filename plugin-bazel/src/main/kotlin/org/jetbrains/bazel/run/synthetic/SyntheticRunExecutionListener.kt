package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.writeAction
import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.run.config.BazelRunConfiguration

class SyntheticRunExecutionListener : ExecutionListener {
  override fun processTerminated(
    executorId: String,
    env: ExecutionEnvironment,
    handler: ProcessHandler,
    exitCode: Int,
  ) {
    super.processTerminated(executorId, env, handler, exitCode)

    val bazelRunConfig = env.runProfile as? BazelRunConfiguration
    if (bazelRunConfig == null) {
      return
    }
    val syntheticBuild = bazelRunConfig.getUserData(SYNTHETIC_BUILD_FILE_KEY)
    if (syntheticBuild != null) {
      runBlocking {
        writeAction {
          syntheticBuild.delete(this)
        }
      }
    }
  }
}
