package org.jetbrains.bazel.server.sync

import com.intellij.platform.util.progress.RawProgressReporter
import com.intellij.platform.util.progress.reportRawProgress
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface RawProgressReporterProvider {
  suspend operator fun <T> invoke(block: suspend (reporter: RawProgressReporter) -> T): T

  companion object {
    fun current(): RawProgressReporterProvider {
      return object : RawProgressReporterProvider {
        override suspend fun <T> invoke(block: suspend (reporter: RawProgressReporter) -> T): T {
          return reportRawProgress { block(it) }
        }
      }
    }
  }
}
