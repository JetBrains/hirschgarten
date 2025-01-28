package org.jetbrains.plugins.bsp.gdb

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.ArchitectureType
import com.jetbrains.cidr.cpp.execution.debugger.backend.CLionLLDBDriverConfiguration
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cidr.execution.debugger.backend.DebuggerDriver
import com.jetbrains.cidr.toolchains.OSType
import java.nio.file.Path

object ToolchainUtils {
  val toolchain: CPPToolchains.Toolchain
    get() {
      var toolchain = CPPToolchains.getInstance().defaultToolchain
      if (toolchain == null) {
        toolchain = CPPToolchains.Toolchain(OSType.getCurrent())
        toolchain.name = CPPToolchains.Toolchain.getDefault()
      }
      return toolchain
    }

  fun setDebuggerToDefault(toolchain: CPPToolchains.Toolchain) {
    val defaultToolchain = ToolchainUtils.toolchain
    toolchain.debugger = defaultToolchain.debugger
  }
}

class BazelLLDBDriverConfiguration(project: Project, private val workingDirectory: Path) :
  CLionLLDBDriverConfiguration(project, ToolchainUtils.toolchain) {
  @Throws(ExecutionException::class)
  override fun createDriverCommandLine(driver: DebuggerDriver, architectureType: ArchitectureType): GeneralCommandLine {
    val commandLine = super.createDriverCommandLine(driver, architectureType)
    commandLine.workDirectory = workingDirectory.toFile()
    return commandLine
  }
}
