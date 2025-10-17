package org.jetbrains.bazel.run.task

import com.intellij.execution.process.ProcessOutputType
import org.jetbrains.bazel.run.BazelProcessHandler
import java.nio.file.Path
import kotlin.io.path.useLines

private const val TEAMCITY_PREFIX = "##teamcity[test"
private const val TEST_NAME_TAG = " name='"
private const val JAVA_TEST_SCHEMA = "java:test://"

class JetBrainsTestRunnerTaskListener(handler: BazelProcessHandler) : BazelRunTaskListener(handler) {
  override fun onCachedTestLog(testLog: Path) {
    testLog.useLines { lines ->
      lines.map { line ->
        markTestNameAsCached(line)
      }.forEach { line ->
        handler.notifyTextAvailable(line + "\n", ProcessOutputType.STDOUT)
      }
    }
  }

  private fun markTestNameAsCached(line: String): String {
    if (!line.startsWith(TEAMCITY_PREFIX)) return line
    if (JAVA_TEST_SCHEMA !in line) return line
    val nameStart = line.indexOf(TEST_NAME_TAG)
    if (nameStart == -1) return line
    val nameEnd = line.indexOf('\'', startIndex = nameStart + TEST_NAME_TAG.length)
    return line.substring(0 until nameEnd) + " (cached)" + line.substring(nameEnd)
  }
}
