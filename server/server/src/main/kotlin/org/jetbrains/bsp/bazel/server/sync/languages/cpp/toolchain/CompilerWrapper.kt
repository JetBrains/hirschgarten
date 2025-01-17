package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.jetbrains.bazel.commons.utils.OsFamily
import java.io.File
import java.nio.file.Path

class CompilerWrapper {
  fun createCompilerExecutableWrapper(
    executionRoot: Path,
    bazelCompilerExecutableFile: Path,
    compilerWrapperEnvVars: Map<String, String>,
  ): Path {
    // bazel itself only uses a wrapper script on unix, so we do not need this script when running on windows
    if (OsFamily.inferFromSystem() == OsFamily.WINDOWS) {
      return bazelCompilerExecutableFile
    }

    val bazelCompilerWrapper =
      File.createTempFile("bazel_compiler", ".sh")
    bazelCompilerWrapper.setExecutable(true)

    val compilerWrapperScriptLines =
      mutableListOf(
        "#!/bin/bash",
        "",
        "# The c toolchain compiler wrapper script doesn't handle arguments files, so we",
        "# need to move the compiler arguments from the file to the command line. We",
        "# preserve any existing commandline arguments, and remove the escaping from",
        "# arguments inside the args file.",
        "",
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
        "EXE=$bazelCompilerExecutableFile",
        "# Read in the arguments file so we can pass the arguments on the command line.",
        "(cd $executionRoot && \$EXE \"\${parsedargs[@]}\")",
      ),
    )
    bazelCompilerWrapper.writeText(compilerWrapperScriptLines.joinToString(System.lineSeparator()))
    return bazelCompilerWrapper.toPath().toAbsolutePath()
  }

  private fun exportedEnvVars(compilerWrapperEnvVars: Map<String, String>): List<String> =
    compilerWrapperEnvVars
      .map { String.format("export %s=%s", it.key, it.value) }
}
