package org.jetbrains.bazel.run.coverage

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.runnerAction.BazelCoverageExecutorProvider

internal class DefaultBazelCoverageExecutorProvider : BazelCoverageExecutorProvider {
  override fun createCoverageExecutor(project: Project): Executor? {
    return ExecutorRegistry.getInstance().getExecutorById(CoverageExecutor.EXECUTOR_ID)
  }

  override fun isCoverageExecutor(executor: Executor): Boolean {
    return executor.id == CoverageExecutor.EXECUTOR_ID
  }
}
