package org.jetbrains.bazel.sdkcompat

import com.intellij.execution.process.ProcessHandler
import com.jetbrains.rd.platform.codeWithMe.portForwarding.ProcessHandlerPidProvider
import kotlinx.coroutines.Deferred

class BazelProcessHandlerPidProvider : ProcessHandlerPidProvider {
  override fun getPid(processHandler: ProcessHandler): Deferred<Long?>? = (processHandler as? HasDeferredPid)?.pid
}
