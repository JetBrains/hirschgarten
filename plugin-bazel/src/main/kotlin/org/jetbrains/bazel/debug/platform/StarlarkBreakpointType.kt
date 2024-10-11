package org.jetbrains.bazel.debug.platform

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import org.jetbrains.bazel.languages.starlark.StarlarkFileType

class StarlarkBreakpointType :
  XLineBreakpointType<StarlarkBreakpointProperties>(
    "starlark-line",
    "Starlark Line Breakpoints",
  ) {
  override fun createBreakpointProperties(file: VirtualFile, line: Int): StarlarkBreakpointProperties = StarlarkBreakpointProperties()

  override fun canPutAt(
    file: VirtualFile,
    line: Int,
    project: Project,
  ): Boolean = file.supportsStarlarkDebugBreakpoint()

  private fun VirtualFile.supportsStarlarkDebugBreakpoint(): Boolean =
    this.extension == StarlarkFileType.defaultExtension || this.name == "BUILD" || this.name == "BUILD.bazel"
}
