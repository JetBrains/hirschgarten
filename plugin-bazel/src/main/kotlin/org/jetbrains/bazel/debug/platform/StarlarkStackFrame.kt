package org.jetbrains.bazel.debug.platform

import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.xdebugger.XDebuggerUtil
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueChildrenList
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import kotlin.io.path.Path
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP

class StarlarkStackFrame(
  private val frame: SDP.Frame,
  val threadId: Long,
  private val childrenComputer: ChildrenComputer,
  evaluatorProvider: StarlarkDebuggerEvaluator.Provider,
) : XStackFrame() {
  var isTopFrame: Boolean = false

  private val evaluator = evaluatorProvider.obtain(threadId)

  override fun computeChildren(node: XCompositeNode) {
    val children = XValueChildrenList()
    children.addToplevelValues(frame)
    node.addChildren(children, true)
  }

  private fun XValueChildrenList.addToplevelValues(frame: SDP.Frame) {
    frame.scopeList.forEach { scope ->
      scope.bindingList.forEach {
        add(StarlarkValue.fromProto(it, threadId, childrenComputer))
      }
    }
  }

  override fun getSourcePosition(): XSourcePosition? {
    val path = frame.location.path
    val vFile = VirtualFileManager.getInstance().findFileByNioPath(Path(path))
    val line = frame.location.lineNumber - 1 // convert from 1-indexed to 0-indexed
    return XDebuggerUtil.getInstance().createPosition(vFile, line)
  }

  override fun getEvaluator(): XDebuggerEvaluator =
    if (isTopFrame) evaluator else notTopFrameError

  private companion object {
    val notTopFrameError =
      StarlarkDebuggerEvaluator.ErrorEvaluator(StarlarkBundle.message("starlark.debug.not.top.frame"))
  }
}
