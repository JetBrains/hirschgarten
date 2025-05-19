package org.jetbrains.bazel.python.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.jetbrains.python.debugger.PyPositionConverter
import com.jetbrains.python.debugger.PySignature
import com.jetbrains.python.debugger.PySourcePosition
import org.jetbrains.bazel.label.Label

class BazelPyDebugPositionConverter(private val project: Project, private val target: Label) : PyPositionConverter {
  @Deprecated("Deprecated in the interface")
  override fun create(filePath: String, line: Int): PySourcePosition = OurPySourcePosition(filePath, line)

  override fun convertToPython(position: XSourcePosition): PySourcePosition {
    val file = position.file.path
    val line = position.line + 1
    return OurPySourcePosition(file, line)
  }

  override fun convertFromPython(position: PySourcePosition, frameName: String?): XSourcePosition? = convertFromPython(position)

  override fun convertFromPython(position: PySourcePosition): XSourcePosition? {
    val file = PythonDebugUtils.findRealSourceFile(project, target, position.file)
    val line = position.line - 1
    return getXSourcePosition(file, line)
  }

  override fun convertSignature(signature: PySignature?): PySignature? = signature
}

private fun getXSourcePosition(file: String, line: Int): XSourcePosition? {
  val virtualFile = LocalFileSystem.getInstance().findFileByPath(file)
  return XDebuggerUtil.getInstance().createPosition(virtualFile, line)
}

private class OurPySourcePosition(file: String, line: Int) : PySourcePosition(file, line)
