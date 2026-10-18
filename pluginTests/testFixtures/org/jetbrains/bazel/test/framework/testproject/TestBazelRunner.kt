package org.jetbrains.bazel.test.framework.testproject

import org.jetbrains.bazel.bazelrunner.BazelCommandExecutionDescriptor
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncher
import org.jetbrains.bazel.bazelrunner.BazelRunner
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path

internal object NoBazelProcessLauncher : BazelProcessLauncher {
  override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process =
    error("The test Bazel project does not start Bazel. Use `buildServer(...)` or a real-sync fixture.")
}

fun testBazelRunner(workspaceRoot: Path, bazelBinary: Path = Path("bazel")): BazelRunner =
  BazelRunner(null, workspaceRoot, NoBazelProcessLauncher, bazelBinary)

/**
 * Creates a runner that starts no Bazel. The runner gives the command line of each Bazel command to [respond],
 * and uses the process that [respond] returns. Use [finishedProcess] to make that process.
 */
fun testBazelRunner(workspaceRoot: Path, bazelBinary: Path = Path("bazel"), respond: (command: List<String>) -> Process): BazelRunner =
  BazelRunner(
    null,
    workspaceRoot,
    object : BazelProcessLauncher {
      override fun launchProcess(executionDescriptor: BazelCommandExecutionDescriptor): Process = respond(executionDescriptor.command)
    },
    bazelBinary,
  )

/** Creates a process that has already ended with [exitCode]. Its output is [stdout] and [stderr]. */
fun finishedProcess(stdout: String = "", stderr: String = "", exitCode: Int = 0): Process =
  object : Process() {
    private val stdoutStream = ByteArrayInputStream(stdout.toByteArray(Charsets.UTF_8))
    private val stderrStream = ByteArrayInputStream(stderr.toByteArray(Charsets.UTF_8))

    override fun getOutputStream(): OutputStream = OutputStream.nullOutputStream()

    override fun getInputStream(): InputStream = stdoutStream

    override fun getErrorStream(): InputStream = stderrStream

    override fun waitFor(): Int = exitCode

    override fun waitFor(timeout: Long, unit: TimeUnit): Boolean = true

    override fun exitValue(): Int = exitCode

    override fun isAlive(): Boolean = false

    override fun destroy() {}
  }
