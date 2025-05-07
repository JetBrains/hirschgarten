package org.jetbrains.bazel.cpp.sync.compiler

import com.intellij.openapi.application.ApplicationManager
import java.io.File

/** Wraps the provided compiler in a script that accepts Clion parameter files.
 * See com.google.idea.blaze.cpp.CompilerWrapperProvider
 * */
interface CompilerWrapperProvider {
  /**
   * Create a wrapper script that transforms the CLion compiler invocation into a safe invocation of
   * the compiler script that blaze uses.
   *
   *
   * CLion passes arguments to the compiler in an arguments file. The c toolchain compiler
   * wrapper script doesn't handle arguments files, so we need to move the compiler arguments from
   * the file to the command line.
   *
   *
   * The first argument provided to this script is the argument file. The second argument is the
   * output file.
   *
   * @param executionRoot the execution root for running the compiler
   * @param bazelCompilerExecutableFile the compiler
   * @param compilerWrapperEnvVars
   * @return The wrapper script that CLion can call.
   */
  fun createCompilerExecutableWrapper(
    executionRoot: File,
    bazelCompilerExecutableFile: File,
    compilerWrapperEnvVars: Map<String, String>,
  ): File?

  companion object {
    fun getInstance(): CompilerWrapperProvider? =
      ApplicationManager.getApplication().getService<CompilerWrapperProvider>(CompilerWrapperProvider::class.java)
  }
}
