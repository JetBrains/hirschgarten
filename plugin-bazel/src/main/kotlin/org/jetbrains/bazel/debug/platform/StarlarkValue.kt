package org.jetbrains.bazel.debug.platform

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos as SDP
import com.intellij.icons.AllIcons
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import org.jetbrains.bazel.debug.connector.StarlarkValueComputer
import javax.swing.Icon

class StarlarkValue private constructor(
  name: String,
  private val type: String,
  private val valueAsString: String,
  private val computeYourChildrenPack: ComputeYourChildrenPack?,
) : XNamedValue(name) {


  override fun computePresentation(node: XValueNode, place: XValuePlace) {
    val hasChildren = computeYourChildrenPack != null
    val icon = getValueIcon()
    val displayValue = getDisplayValue()
    node.setPresentation(icon, type, displayValue, hasChildren)
  }

  private fun getValueIcon(): Icon =
    when (type) {
      // types mentioned in Starlark documentation are given explicit branches here
      "NoneType" -> AllIcons.Debugger.Db_primitive
      "bool"     -> AllIcons.Debugger.Db_primitive
      "int"      -> AllIcons.Debugger.Db_primitive
      "string"   -> AllIcons.Debugger.Db_primitive
      "list"     -> AllIcons.Debugger.Db_array
      "tuple"    -> AllIcons.Debugger.Db_array
      "dict"     -> AllIcons.Debugger.Value
      "function" -> AllIcons.Nodes.Function
      else       -> AllIcons.Debugger.Value
    }

  private fun getDisplayValue(): String =
    when (type) {
      "string" -> "\"$valueAsString\"" // display strings as "abc" instead of just abc
      else -> valueAsString
    }

  override fun canNavigateToSource(): Boolean = false

  override fun computeChildren(node: XCompositeNode) {
    computeYourChildrenPack?.let {
      it.valueComputer.computeValueChildren(it.threadId, it.valueId) {
        children -> node.addComputedChildren(children, it)
      }
    } ?: node.addChildren(XValueChildrenList(), true)
  }

  private fun XCompositeNode.addComputedChildren(
    children: List<SDP.Value>,
    computeYourChildrenPack: ComputeYourChildrenPack
  ) {
    val xChildren = XValueChildrenList()
    children
      .map { it.toStarlarkValue(computeYourChildrenPack) }
      .forEach { xChildren.add(it) }
    this.addChildren(xChildren, true)
  }

  private fun SDP.Value.toStarlarkValue(parentPack: ComputeYourChildrenPack): StarlarkValue =
    fromProto(this, parentPack.threadId, parentPack.valueComputer)


  companion object {
    fun fromProto(value: SDP.Value, threadId: Long, valueComputer: StarlarkValueComputer): StarlarkValue {
      val pack = if (value.hasChildren) {
        ComputeYourChildrenPack(threadId, value.id, valueComputer)
      } else null
      return StarlarkValue(value.label, value.type, value.description, pack)
    }
  }


  // everything a value needs to find its children
  private data class ComputeYourChildrenPack(
    val threadId: Long,
    val valueId: Long,
    val valueComputer: StarlarkValueComputer,
  )
}
