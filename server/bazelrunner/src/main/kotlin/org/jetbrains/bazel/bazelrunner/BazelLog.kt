package org.jetbrains.bazel.bazelrunner

import com.intellij.openapi.application.PathManager
import com.intellij.util.io.outputStream
import org.jetbrains.bazel.config.BazelFeatureFlags
import java.io.PrintWriter
import java.nio.file.Path
import java.time.Instant

object BazelLog {
  val logPath: Path
    get() = PathManager.getLogDir().resolve("bazel-logs").resolve("bazel.log")

  fun write(body: PrintWriter.() -> Unit) {
    if (!BazelFeatureFlags.isLogEnabled) return

    logPath.outputStream(append = true).use { out ->
      PrintWriter(out).use { writer ->
        try {
          writer.appendLine("\n${Instant.now()}")
          body(writer)
        } catch (ex: Throwable) {
          ex.printStackTrace(writer)
        }
      }
    }
  }
}
