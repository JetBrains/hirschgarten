package org.jetbrains.bazel.build.events

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos.*
import com.google.protobuf.Timestamp
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Test utilities for creating and handling BEP (Build Event Protocol) events in tests.
 * Ported patterns from Google's Bazel plugin test infrastructure.
 */
object BepTestUtils {

  /**
   * Converts BuildEvent builders to an InputStream suitable for BEP parsing.
   * Events are written in delimited format (same as Bazel's binary BEP output).
   */
  fun asInputStream(vararg events: BuildEvent.Builder): InputStream {
    return asInputStream(events.toList())
  }

  fun asInputStream(events: Iterable<BuildEvent.Builder>): InputStream {
    val output = ByteArrayOutputStream()
    for (event in events) {
      event.build().writeDelimitedTo(output)
    }
    return ByteArrayInputStream(output.toByteArray())
  }

  /**
   * Creates a Configuration event.
   */
  fun configuration(id: String, mnemonic: String): BuildEvent.Builder {
    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setConfiguration(BuildEventId.ConfigurationId.newBuilder().setId(id))
      )
      .setConfiguration(Configuration.newBuilder().setMnemonic(mnemonic))
  }

  /**
   * Creates a NamedSetOfFiles event.
   */
  fun namedSetOfFiles(filePaths: List<String>, setId: String): BuildEvent.Builder {
    val files = filePaths.map { path ->
      File.newBuilder()
        .setUri("file:$path")
        .setName(path)
        .build()
    }

    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setNamedSet(BuildEventId.NamedSetOfFilesId.newBuilder().setId(setId))
      )
      .setNamedSetOfFiles(
        NamedSetOfFiles.newBuilder().addAllFiles(files)
      )
  }

  /**
   * Creates a TestResult event.
   */
  fun testResult(
    label: String,
    status: TestStatus,
    outputFiles: List<String> = emptyList(),
    statusDetails: String? = null,
    durationMillis: Long? = null
  ): BuildEvent.Builder {
    val builder = TestResult.newBuilder()
      .setStatus(status)

    statusDetails?.let { builder.statusDetails = it }

    durationMillis?.let {
      builder.setTestAttemptDuration(
        com.google.protobuf.Duration.newBuilder()
          .setSeconds(it / 1000)
          .setNanos(((it % 1000) * 1_000_000).toInt())
      )
    }

    outputFiles.forEach { path ->
      builder.addTestActionOutput(
        File.newBuilder()
          .setUri("file:$path")
          .setName(path)
      )
    }

    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setTestResult(BuildEventId.TestResultId.newBuilder().setLabel(label))
      )
      .setTestResult(builder)
  }

  /**
   * Creates a TestSummary event.
   */
  fun testSummary(
    label: String,
    overallStatus: TestStatus,
    totalRunCount: Int = 1,
    passed: List<String> = emptyList(),
    failed: List<String> = emptyList(),
    firstStartTimeMillis: Long? = null,
    totalDurationSec: Long? = null
  ): BuildEvent.Builder {
    val builder = TestSummary.newBuilder()
      .setOverallStatus(overallStatus)
      .setTotalRunCount(totalRunCount)

    passed.forEach {
      builder.addPassed(File.newBuilder().setUri("file:$it").setName(it))
    }

    failed.forEach {
      builder.addFailed(File.newBuilder().setUri("file:$it").setName(it))
    }

    firstStartTimeMillis?.let {
      builder.setFirstStartTime(
        Timestamp.newBuilder()
          .setSeconds(it / 1000)
          .setNanos(((it % 1000) * 1_000_000).toInt())
      )
    }

    totalDurationSec?.let {
      builder.setTotalRunDuration(
        com.google.protobuf.Duration.newBuilder().setSeconds(it)
      )
    }

    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setTestSummary(BuildEventId.TestSummaryId.newBuilder().setLabel(label))
      )
      .setTestSummary(builder)
  }

  /**
   * Creates an ActionCompleted event (typically for action failures).
   */
  fun actionCompleted(
    label: String,
    success: Boolean,
    stderr: String? = null,
    stdout: String? = null,
    primaryOutput: String? = null
  ): BuildEvent.Builder {
    val builder = ActionExecuted.newBuilder().setSuccess(success)

    stderr?.let {
      builder.stderr = File.newBuilder().setUri("file:$it").setName(it).build()
    }

    stdout?.let {
      builder.stdout = File.newBuilder().setUri("file:$it").setName(it).build()
    }

    primaryOutput?.let {
      builder.primaryOutput = File.newBuilder().setUri("file:$it").setName(it).build()
    }

    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setActionCompleted(
            BuildEventId.ActionCompletedId.newBuilder()
              .setLabel(label)
              .setPrimaryOutput(primaryOutput ?: "")
          )
      )
      .setAction(builder)
  }

  /**
   * Creates an Aborted event.
   */
  fun aborted(
    label: String,
    reason: Aborted.AbortReason,
    description: String = ""
  ): BuildEvent.Builder {
    return BuildEvent.newBuilder()
      .setId(
        BuildEventId.newBuilder()
          .setTargetCompleted(
            BuildEventId.TargetCompletedId.newBuilder().setLabel(label)
          )
      )
      .setAborted(
        Aborted.newBuilder()
          .setReason(reason)
          .setDescription(description)
      )
  }

  /**
   * Creates a BuildFinished event.
   */
  fun buildFinished(
    exitCode: Int,
    exitCodeName: String = "",
    anomalyReport: Boolean = false,
    finishTimeMillis: Long? = null
  ): BuildEvent.Builder {
    val exitCodeBuilder = BuildFinished.ExitCode.newBuilder()
      .setCode(exitCode)
      .setName(exitCodeName)

    val builder = BuildFinished.newBuilder()
      .setExitCode(exitCodeBuilder)

    if (anomalyReport) {
      builder.setAnomalyReport(
        BuildFinished.AnomalyReport.newBuilder().setWasSuspended(true)
      )
    }

    finishTimeMillis?.let {
      builder.setFinishTime(
        Timestamp.newBuilder()
          .setSeconds(it / 1000)
          .setNanos(((it % 1000) * 1_000_000).toInt())
      )
    }

    return BuildEvent.newBuilder()
      .setId(BuildEventId.newBuilder().setBuildFinished(BuildEventId.BuildFinishedId.getDefaultInstance()))
      .setFinished(builder)
  }
}
