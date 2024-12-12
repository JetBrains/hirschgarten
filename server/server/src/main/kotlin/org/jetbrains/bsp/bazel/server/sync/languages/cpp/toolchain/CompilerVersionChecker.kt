package org.jetbrains.bsp.bazel.server.sync.languages.cpp.toolchain

import org.apache.logging.log4j.LogManager
import org.jetbrains.bsp.bazel.projectview.parser.DefaultProjectViewParser
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

interface CompilerVersionChecker {
  fun getCompilerVersion(execRoot: String, executable: Path, xcode: XCodeCompilerSettings?): String?
}


class CompilerVersionCheckerImpl : CompilerVersionChecker {
  private val log = LogManager.getLogger(javaClass)
  override fun getCompilerVersion(execRoot: String, executable: Path, xcode: XCodeCompilerSettings?): String? {
    if (!Paths.get(execRoot).exists()) {
      log.warn("Cannot find executable '$execRoot' in $executable")
      return null
    }
    if (!executable.exists()) {
      log.warn("Cannot find executable '$execRoot' in $executable")
      return null
    }


    val compilerEnvFlags = xcode?.asEnvironmentVariables() ?: emptyMap()
    val processBuilder = ProcessBuilder(executable.toAbsolutePath().toString(), "--version")
    processBuilder.environment().putAll(compilerEnvFlags)
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

    log.error("stderr: $err \n stdout: $out")
    return null

  }

  private fun getMSVCVersion(err: String): String {
    val endOfLine = err.indexOf('\r')
    if (endOfLine < 0) {
      return err
    }
    return err.substring(0, endOfLine)
  }
}

