package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project

// TODO: remove this after execution refactoring
//  core module shouldn't be aware of things like coverage
//  coverage is simple an external tool which should be handle as everything else
interface BazelCoverageExecutorProvider {
  fun createCoverageExecutor(project: Project): Executor?
  fun isCoverageExecutor(executor: Executor): Boolean

  companion object {
    val ep: ExtensionPointName<BazelCoverageExecutorProvider> = ExtensionPointName.create("org.jetbrains.bazel.coverageExecutorProvider")
  }
}
