package org.jetbrains.bazel.cpp.sync.compiler

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import sun.nio.cs.UTF_8
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.StandardCharsets

/**
 * Creates a wrapper script which reads the arguments file and writes the compiler outputs directly.
 */
class CompilerWrapperProviderImpl : CompilerWrapperProvider {
  override fun createCompilerExecutableWrapper(
    executionRoot: File,
    bazelCompilerExecutableFile: File,
    compilerWrapperEnvVars: Map<String, String>,
  ): File? {
    // bazel only uses a wrapper script on unix, so we do not need this script when running on windows
    if (SystemInfo.isWindows) {
      return bazelCompilerExecutableFile
    }

    try {
      val bazelCompilerWrapper: File =
        FileUtil.createTempFile("bazel_compiler", ".sh", true)
      if (!bazelCompilerWrapper.setExecutable(true)) {
        logger.warn("Unable to make compiler wrapper script executable: $bazelCompilerWrapper")
        return null
      }
      val compilerWrapperScriptLines = mutableListOf<String>()
      compilerWrapperScriptLines.addAll(
        listOf(
          "#!/bin/bash",
          "",
          "# The c toolchain compiler wrapper script doesn't handle arguments files, so we",
          "# need to move the compiler arguments from the file to the command line. We",
          "# preserve any existing commandline arguments, and remove the escaping from",
          "# arguments inside the args file.",
          "",
        ),
      )
      compilerWrapperScriptLines.addAll(
        exportedEnvVars(compilerWrapperEnvVars),
      )
      compilerWrapperScriptLines.addAll(
        listOf(
          "parsedargs=()",
          "for arg in \"\${@}\"; do ",
          "  case \"\$arg\" in",
          "    @*)",
          "      # Make sure the file ends with a newline - the read loop will not return",
          "      # the final line if it does not.",
          "      echo >> \${arg#@}",
          "      # Args file, the read will remove a layer of escaping",
          "      while read; do",
          "        parsedargs+=(\$REPLY)",
          "      done < \${arg#@}",
          "      ;;",
          "    *)",
          "      # Regular arg",
          "      parsedargs+=(\"\$arg\")",
          "      ;;",
          "  esac",
          "done",
          "",
          "# The actual compiler wrapper script we get from blaze",
          String.format("EXE=%s", bazelCompilerExecutableFile.path),
          "# Read in the arguments file so we can pass the arguments on the command line.",
          String.format("(cd %s && \$EXE \"\${parsedargs[@]}\")", executionRoot),
        ),
      )
      PrintWriter(bazelCompilerWrapper, StandardCharsets.UTF_8.name()).use { pw ->
        compilerWrapperScriptLines.forEach { pw.println(it) }
      }

      return bazelCompilerWrapper
    } catch (e: IOException) {
      logger.warn(
        "Unable to write compiler wrapper script executable: $bazelCompilerExecutableFile",
        e,
      )
      return null
    }
  }

  private fun exportedEnvVars(compilerWrapperEnvVars: Map<String, String>): List<String> =
    compilerWrapperEnvVars
      .map { envVar -> String.format("export %s=%s", envVar.key, envVar.value) }

  companion object {
    private val logger: Logger = Logger.getInstance(CompilerWrapperProviderImpl::class.java)
  }
}
