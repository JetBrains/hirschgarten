package org.jetbrains.bazel.run.synthetic

import com.intellij.execution.ExecutionListener
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import java.nio.file.Files

class SyntheticRunExecutionListener : ExecutionListener {
  override fun processStarting(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
    val runConfig = env.runProfile as? BazelRunConfiguration ?: return
    val syntheticTemplate = runConfig.getUserData(SYNTHETIC_BUILD_SESSION)
    if (syntheticTemplate == null) {
      return
    }
    val buildFilePath = syntheticTemplate.buildFilePath
    Files.createDirectories(buildFilePath.parent)
    Files.writeString(buildFilePath, syntheticTemplate.buildFileContent)
  }

  override fun processTerminated(
    executorId: String,
    env: ExecutionEnvironment,
    handler: ProcessHandler,
    exitCode: Int,
  ) {
    val runConfig = env.runProfile as? BazelRunConfiguration ?: return
    val syntheticTemplate = runConfig.getUserData(SYNTHETIC_BUILD_SESSION)
    if (syntheticTemplate != null) {
      Files.deleteIfExists(syntheticTemplate.buildFilePath)
    }
  }
}
