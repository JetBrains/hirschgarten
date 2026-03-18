package org.jetbrains.bazel.run

import com.intellij.execution.impl.ProcessHandlerPidProvider
import com.intellij.execution.process.ProcessHandler
import kotlinx.coroutines.Deferred

internal class BazelProcessHandlerPidProvider : ProcessHandlerPidProvider {
  override fun getPid(processHandler: ProcessHandler): Deferred<Long?>? =
    (processHandler as? BazelProcessHandler)?.pid
}
