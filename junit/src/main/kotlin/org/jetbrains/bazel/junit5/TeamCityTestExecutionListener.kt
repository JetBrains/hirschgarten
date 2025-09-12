package org.jetbrains.bazel.junit5

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import java.time.Duration
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap

/**
 * JUnit 5 TestExecutionListener that emits TeamCity service messages to stdout so IntelliJ's SM Runner
 * can render real-time test progress when running tests under Bazel.
 *
 * It reports:
 * - testSuiteStarted/Finished for containers (classes, nested containers), excluding engine roots
 * - testStarted/Finished (and testFailed/testIgnored) for tests
 * - locationHint using uniqueId: so the IDE can navigate to sources
 * - flowId using the current thread id to support parallel runs
 */
class TeamCityTestExecutionListener : TestExecutionListener {
  private val startedAtNanos = ConcurrentHashMap<String, Long>()
  private val startedSuites = ConcurrentHashMap.newKeySet<String>()

  override fun executionStarted(testIdentifier: TestIdentifier) {
    val id = testIdentifier.uniqueId
    if (testIdentifier.isContainer && hasParent(testIdentifier)) {
      if (startedSuites.add(id)) {
        teamcity(
          "testSuiteStarted",
          mapOf(
            "name" to testIdentifier.displayName,
            "locationHint" to "uniqueId:${escape(id)}",
            "flowId" to flowId(),
          ),
        )
      }
    }
    if (testIdentifier.isTest) {
      startedAtNanos[id] = System.nanoTime()
      teamcity(
        "testStarted",
        mapOf(
          "name" to testIdentifier.displayName,
          "locationHint" to "uniqueId:${escape(id)}",
          "flowId" to flowId(),
          // captureStandardOutput=true makes SM Runner attach stdout/stderr to the current test when possible
          "captureStandardOutput" to "true",
        ),
      )
    }
  }

  override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
    val id = testIdentifier.uniqueId
    if (testIdentifier.isTest) {
      when (testExecutionResult.status) {
        TestExecutionResult.Status.SUCCESSFUL -> {
          // no-op, will just send finished with duration
        }
        TestExecutionResult.Status.ABORTED -> {
          teamcity(
            "testIgnored",
            mapOf(
              "name" to testIdentifier.displayName,
              "message" to (testExecutionResult.throwable.map { it.message ?: it::class.java.name }.orElse("aborted")),
              "flowId" to flowId(),
            ),
          )
        }
        TestExecutionResult.Status.FAILED -> {
          val (message, details) = testExecutionResult.throwable.map { t ->
            (t.message ?: t::class.java.name) to t.stackTraceToString()
          }.orElse("failed" to "")
          teamcity(
            "testFailed",
            mapOf(
              "name" to testIdentifier.displayName,
              "message" to message,
              "details" to details,
              "flowId" to flowId(),
            ),
          )
        }
      }
      val durationMillis = startedAtNanos.remove(id)?.let { nanos -> Duration.ofNanos(System.nanoTime() - nanos).toMillis() } ?: 0L
      teamcity(
        "testFinished",
        mapOf(
          "name" to testIdentifier.displayName,
          "duration" to durationMillis.toString(),
          "flowId" to flowId(),
        ),
      )
    } else if (testIdentifier.isContainer && hasParent(testIdentifier)) {
      if (startedSuites.remove(id)) {
        teamcity(
          "testSuiteFinished",
          mapOf(
            "name" to testIdentifier.displayName,
            "flowId" to flowId(),
          ),
        )
      }
    }
  }

  private fun hasParent(testIdentifier: TestIdentifier): Boolean = testIdentifier.parentId.isPresent

  private fun flowId(): String = Thread.currentThread().id.toString()

  private fun teamcity(messageName: String, attrs: Map<String, String>) {
    val rendered = buildString(64) {
      append("##teamcity[")
      append(messageName)
      for ((k, v) in attrs) {
        append(' ')
        append(k)
        append('=')
        append('\'')
        append(escape(v))
        append('\'')
      }
      append(']')
    }
    println(rendered)
    System.out.flush()
  }

  private fun escape(v: String): String = v
    .replace("|", "||")
    .replace("'", "|'")
    .replace("\n", "|n")
    .replace("\r", "|r")
    .replace("[", "|[")
    .replace("]", "|]")
}
