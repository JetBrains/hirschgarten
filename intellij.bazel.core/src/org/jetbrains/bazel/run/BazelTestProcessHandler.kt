package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred

internal class BazelTestProcessHandler(
  project: Project,
  runDeferred: Deferred<*>,
  pidDeferred: Deferred<Long?>? = null,
  private val isIdBasedTestTree: Boolean = false,
) : BazelProcessHandler(project, runDeferred, pidDeferred) {
  override fun destroyProcessImpl() {
    // send a fake terminated test to the test console - otherwise it will show "No tests were found" instead of "Terminated"
    val message = ServiceMessageBuilder.testSuiteStarted("Run cancelled")
    if (isIdBasedTestTree) {
      message.addAttribute("nodeId", "cancelled").addAttribute("parentNodeId", "0")
    }
    notifyTextAvailable(message.toString() + "\n", ProcessOutputType.STDOUT)
    super.destroyProcessImpl()
  }
}
