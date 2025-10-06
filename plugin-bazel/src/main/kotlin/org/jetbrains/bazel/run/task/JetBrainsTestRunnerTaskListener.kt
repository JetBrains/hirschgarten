package org.jetbrains.bazel.run.task

import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.run.BazelProcessHandler
import java.nio.file.Path
import kotlin.io.path.readText

class JetBrainsTestRunnerTaskListener(handler: BazelProcessHandler) : BazelRunTaskListener(handler) {
  override fun onCachedTestLog(testLog: Path) {
    ansiEscapeDecoder.escapeText(testLog.readText(), ProcessOutputType.STDOUT) { s: String, key: Key<Any> ->
      handler.notifyTextAvailable(s, key)
    }
  }
}
