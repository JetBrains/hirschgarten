package org.jetbrains.bazel.cpp.sync.compiler

import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker.VersionCheckException
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker.VersionCheckException.IssueKind
import java.io.File

/** Runs a compiler to check its version.
 * Inspired by com.google.idea.blaze.cpp.CompilerVersionCheckerImpl
 * */
class CompilerVersionCheckerImpl : CompilerVersionChecker {
  @Throws(VersionCheckException::class)
  public override fun checkCompilerVersion(
    executionRoot: File,
    cppExecutable: File,
    checkerEnv: Map<String, String>,
  ): String {
    if (!executionRoot.exists()) {
      throw VersionCheckException(IssueKind.MISSING_EXEC_ROOT, "")
    }
    if (!cppExecutable.exists()) {
      throw VersionCheckException(IssueKind.MISSING_COMPILER, "")
    }

    val processBuilder = ProcessBuilder(cppExecutable.toPath().toAbsolutePath().toString(), "--version")
    processBuilder.environment().putAll(checkerEnv)
    val process = processBuilder.start()
    val result = process.waitFor()
    val out = process.inputStream.bufferedReader().use { it.readText() }
    val err = process.errorStream.bufferedReader().use { it.readText() }

    if (result == 0) {
      return out
    }

    // MSVC does not know the --version flag and will fail. However, the error message does contain
    // the compiler version.
    if (err.contains("Microsoft")) {
      return getMSVCVersion(err)
    }

    throw VersionCheckException(
      IssueKind.GENERIC_FAILURE,
      String.format("stderr: \"%s\"\nstdout: \"%s\"", err, out),
    )
  }

  private fun getMSVCVersion(err: String): String {
    val endOfLine = err.indexOf('\r')
    if (endOfLine < 0) {
      return err
    }

    return err.substring(0, endOfLine)
  }
}
