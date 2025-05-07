package org.jetbrains.bazel.cpp.sync.compiler

import com.intellij.openapi.application.ApplicationManager
import java.io.File

// See com.google.idea.blaze.cpp.CompilerVersionChecker
interface CompilerVersionChecker {
  /** Indicates failure to check the compiler version  */
  class VersionCheckException(val kind: IssueKind, message: String) : Exception(message) {
    /** Describes the failure mode of the version check.  */
    enum class IssueKind {
      MISSING_EXEC_ROOT,
      MISSING_COMPILER,
      GENERIC_FAILURE,
    }
  }

  /** Returns the compiler's version string  */
  fun checkCompilerVersion(
    executionRoot: File,
    cppExecutable: File,
    checkerEnv: Map<String, String>,
  ): String?

  companion object {
    fun getInstance(): CompilerVersionChecker? =
      ApplicationManager.getApplication().getService<CompilerVersionChecker>(CompilerVersionChecker::class.java)
  }
}
