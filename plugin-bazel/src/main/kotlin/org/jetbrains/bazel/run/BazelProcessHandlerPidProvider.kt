package org.jetbrains.bazel.run

import com.intellij.execution.process.ProcessHandler
import com.jetbrains.rd.platform.codeWithMe.portForwarding.ProcessHandlerPidProvider
import kotlinx.coroutines.Deferred

internal class BazelProcessHandlerPidProvider : ProcessHandlerPidProvider {
  override fun getPid(processHandler: ProcessHandler): Deferred<Long?>? =
    (processHandler as? BazelProcessHandler)?.pid
}
