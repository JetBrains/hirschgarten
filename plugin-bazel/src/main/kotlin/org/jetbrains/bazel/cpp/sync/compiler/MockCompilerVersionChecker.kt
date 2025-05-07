package org.jetbrains.bazel.cpp.sync.compiler

import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker.VersionCheckException
import org.jetbrains.bazel.cpp.sync.compiler.CompilerVersionChecker.VersionCheckException.IssueKind
import java.io.File

/** [CompilerVersionChecker] for tests.
 * See com.google.idea.blaze.cpp.MockCompilerVersionChecker
 * */
class MockCompilerVersionChecker(private var compilerVersion: String) : CompilerVersionChecker {
  private var injectFault = false

  @Throws(VersionCheckException::class)
  public override fun checkCompilerVersion(
    executionRoot: File,
    cppExecutable: File,
    checkerEnv: Map<String, String>,
  ): String? {
    if (injectFault) {
      throw VersionCheckException(IssueKind.GENERIC_FAILURE, "injected fault")
    }
    return compilerVersion
  }

  fun setCompilerVersion(compilerVersion: String) {
    this.compilerVersion = compilerVersion
  }

  fun setInjectFault(injectFault: Boolean) {
    this.injectFault = injectFault
  }
}
