package org.jetbrains.bsp.bazel.server.sync.languages.cpp


import java.io.File
import java.io.PrintWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.function.Consumer

class CompilerWrapper {

  fun createCompilerExecutableWrapper(
    executionRoot: Path, blazeCompilerExecutableFile: Path,
    compilerWrapperEnvVars: Map<String, String>
  ): Path {
    // bazel only uses a wrapper script on unix, so we do not need this script when running on windows
    // todo: skip windows

      val blazeCompilerWrapper =
        File.createTempFile("blaze_compiler", ".sh" /* deleteOnExit */)
      blazeCompilerWrapper.setExecutable(true)

      val compilerWrapperScriptLines  = mutableListOf(
          "#!/bin/bash",
          "",
          "# The c toolchain compiler wrapper script doesn't handle arguments files, so we",
          "# need to move the compiler arguments from the file to the command line. We",
          "# preserve any existing commandline arguments, and remove the escaping from",
          "# arguments inside the args file.",
          "",
        )
    compilerWrapperScriptLines.addAll(
          exportedEnvVars(compilerWrapperEnvVars)
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
          String.format("EXE=%s", blazeCompilerExecutableFile),
          "# Read in the arguments file so we can pass the arguments on the command line.",
          String.format("(cd %s && \$EXE \"\${parsedargs[@]}\")", executionRoot),
        )
        )


      PrintWriter(blazeCompilerWrapper, StandardCharsets.UTF_8.name()).use { pw ->
        compilerWrapperScriptLines.forEach(Consumer { x: String? -> pw.println(x) })
      }
      return blazeCompilerWrapper.toPath().toAbsolutePath()

  }

  private fun exportedEnvVars(compilerWrapperEnvVars: Map<String, String>):List<String> {
    return compilerWrapperEnvVars.
      map {String.format("export %s=%s",it.key, it.value) }
  }
}
