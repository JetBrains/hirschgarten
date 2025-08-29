package org.jetbrains.bazel.buildifier

import com.intellij.execution.Platform
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.OSAgnosticPathUtil
import org.jetbrains.bazel.config.BazelPluginBundle
import java.io.File
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isRegularFile

class BuildifierUtil {
  companion object {
    fun detectBuildifierExecutable(): Path? {
      val name =
        when {
          SystemInfo.isWindows -> "buildifier.exe"
          else -> "buildifier"
        }
      return PathEnvironmentVariableUtil.findInPath(name)?.toPath()
    }

    fun validateBuildifierExecutable(executablePath: String?): ValidationInfo? {
      val message =
        when {
          executablePath.isNullOrEmpty() -> BazelPluginBundle.message("path.validation.field.empty")
          !isAbsolutePath(executablePath) -> BazelPluginBundle.message("path.validation.must.be.absolute")
          executablePath.endsWith(" ") -> BazelPluginBundle.message("path.validation.ends.with.whitespace")
          else ->
            try {
              val nioPath = Path("").resolve(executablePath)
              nioPath.getErrorMessage()
            } catch (e: InvalidPathException) {
              BazelPluginBundle.message("path.validation.invalid", e.message.orEmpty())
            } catch (e: IOException) {
              BazelPluginBundle.message("path.validation.inaccessible", e.message.orEmpty())
            }
        }
      return message?.let { ValidationInfo(it, null) }
    }

    private fun Path.getErrorMessage(): String? =
      when {
        this.isRegularFile() -> if (this.isExecutable()) null else BazelPluginBundle.message("path.validation.cannot.execute", this)
        this.isDirectory() -> BazelPluginBundle.message("path.validation.cannot.execute", this)
        else -> BazelPluginBundle.message("path.validation.file.not.found", this)
      }

    private fun isAbsolutePath(path: String): Boolean =
      when (Platform.current()) {
        Platform.UNIX -> path.startsWith("/")
        // On Windows user may create project in \\wsl
        Platform.WINDOWS -> OSAgnosticPathUtil.isAbsoluteDosPath(path) || path.startsWith("\\\\wsl")
      }
  }
}
