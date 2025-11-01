package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import java.nio.file.Files

class SyntheticRunExecutionListener : ExecutionListener {
  override fun processTerminated(
    executorId: String,
    env: ExecutionEnvironment,
    handler: ProcessHandler,
    exitCode: Int,
  ) {
    val runConfig = env.runProfile as? BazelRunConfiguration ?: return
    val syntheticBuild = runConfig.getUserData(SYNTHETIC_BUILD_FILE_KEY)
    if (syntheticBuild != null) {
      Files.deleteIfExists(syntheticBuild)
    }
  }
}
