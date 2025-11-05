package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import org.jetbrains.bazel.run.test.useJetBrainsTestRunner

class BazelTestProcessHandler(
  private val project: Project,
  runDeferred: Deferred<*>,
  pidDeferred: Deferred<Long?>? = null,
) : BazelProcessHandler(project, runDeferred, pidDeferred) {
  override fun destroyProcessImpl() {
    // send a fake terminated test to the test console - otherwise it will show "No tests were found" instead of "Terminated"
    val message = ServiceMessageBuilder.testSuiteStarted("Run cancelled")
    if (project.useJetBrainsTestRunner()) {
      message.addAttribute("nodeId", "cancelled").addAttribute("parentNodeId", "0")
    }
    notifyTextAvailable(message.toString() + "\n", ProcessOutputType.STDOUT)
    super.destroyProcessImpl()
  }
}
