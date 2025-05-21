package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred

class BazelTestProcessHandler(project: Project, runDeferred: Deferred<*>) : BazelProcessHandler(project, runDeferred) {
  override fun destroyProcessImpl() {
    // send a fake terminated test to the test console - otherwise it will show "No tests were found" instead of "Terminated"
    notifyTextAvailable(ServiceMessageBuilder.testSuiteStarted("Run cancelled").toString() + "\n", ProcessOutputType.STDOUT)
    super.destroyProcessImpl()
  }
}
