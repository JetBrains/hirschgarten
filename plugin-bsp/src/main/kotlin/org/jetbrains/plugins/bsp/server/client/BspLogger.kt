package org.jetbrains.plugins.bsp.server.client

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.IdeaLogRecordFormatter
import com.intellij.openapi.diagnostic.JulLogger
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.RollingFileHandler
import kotlinx.datetime.Instant
import java.util.logging.Level
import java.util.logging.LogRecord

public class BspLogger {
  private val logDir = PathManager.getLogDir().resolve("bsp-logs").resolve("bsp.log")

  private val handler = RollingFileHandler(logDir, 20_000_000L, 10, true)

  private val baseLoggerLevel = Level.INFO

  init {
    handler.formatter = BspLogsFormatter
  }

  public fun getBspLoggerInstance(name: String): Logger {
    val logger = java.util.logging.Logger.getLogger(name)
    JulLogger.clearHandlers(logger)
    handler.setLevel(baseLoggerLevel)
    logger.addHandler(handler)
    logger.setUseParentHandlers(false)
    logger.setLevel(baseLoggerLevel)
    return JulLogger(logger)
  }

  private object BspLogsFormatter : IdeaLogRecordFormatter() {
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

    private fun timeSinceStartup(current: Long) =
      (current - creationTime).takeUnless { it <= 0 }?.toString() ?: "-------"
  }
}

public inline fun <reified T : Any> bspLogger(): Logger =
  BspLogger().getBspLoggerInstance("#${T::class.java}")
