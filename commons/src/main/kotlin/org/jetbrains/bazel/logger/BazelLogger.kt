package org.jetbrains.bazel.logger

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.IdeaLogRecordFormatter
import com.intellij.openapi.diagnostic.JulLogger
import com.intellij.openapi.diagnostic.RollingFileHandler
import kotlinx.datetime.Instant
import java.util.logging.Level
import java.util.logging.LogRecord

abstract class BazelLogger {
  abstract fun error(message: String)

  abstract fun warn(message: String)

  abstract fun info(message: String)

  abstract fun trace(message: String)
}

class BazelLoggerFactory {
  private val logDir = PathManager.getLogDir().resolve("bazel-logs").resolve("bazel.log")

  private val handler = RollingFileHandler(logDir, 20_000_000L, 10, true)

  private val baseLoggerLevel = Level.INFO

  init {
    handler.formatter = BazelLogsFormatter
  }

  fun getBazelLoggerInstance(name: String): BazelLogger {
    val logger =
      java.util.logging.Logger
        .getLogger(name)
    JulLogger.clearHandlers(logger)
    handler.setLevel(baseLoggerLevel)
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)
    logger.setLevel(baseLoggerLevel)
    return object : BazelLogger() {
      val julLogger = JulLogger(logger)

      override fun error(message: String) = julLogger.error(message)

      override fun warn(message: String) = julLogger.warn(message)

      override fun info(message: String) = julLogger.info(message)

      override fun trace(message: String) = julLogger.trace(message)
    }
  }

  private object BazelLogsFormatter : IdeaLogRecordFormatter() {
    private val creationTime = System.currentTimeMillis()

    private val separator = System.lineSeparator()

    override fun format(record: LogRecord): String {
      val time = Instant.fromEpochMilliseconds(record.millis)
      val relativeTime = timeSinceStartup(record.millis)
      val levelName = record.level.name
      val message = formatMessage(record)
      val exception = record.thrown?.let { formatThrowable(it) }.orEmpty()
      return "$time [$relativeTime] $levelName - $message$separator$exception"
    }

    private fun timeSinceStartup(current: Long) = (current - creationTime).takeUnless { it <= 0 }?.toString() ?: "-------"
  }
}

inline fun <reified T : Any> bazelLogger(): BazelLogger = BazelLoggerFactory().getBazelLoggerInstance("#${T::class.java}")
