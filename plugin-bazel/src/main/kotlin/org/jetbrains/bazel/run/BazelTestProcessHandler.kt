package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred

class BazelTestProcessHandler(project: Project, runDeferred: Deferred<*>) : BazelProcessHandler(project, runDeferred) {
  override fun destroyProcessImpl() {
    // when there are no tests in the test console, it will wrongly show cancelled run as "No tests were found"
    notifyTextAvailable(ServiceMessageBuilder.testSuiteStarted("Run cancelled").toString() + "\n", ProcessOutputType.STDOUT)
    super.destroyProcessImpl()
  }
}
