package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.components.JBTextField
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.StickyTargetAction
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentListener

public class SearchBarPanel : JPanel(BorderLayout()) {
  private val textFieldComponent = JBTextField()
  private val displayAsTree = BooleanWithButton(
    buttonText = BspPluginBundle.message("widget.target.search.display.as.tree"),
    buttonIcon = AllIcons.Actions.ShowAsTree,
  )

  private val queryChangeListeners = mutableListOf<DocumentListener>()

  init {
    add(textFieldComponent)
    add(displayAsTree.button, BorderLayout.EAST)
  }

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    textFieldComponent.isEnabled = enabled
    displayAsTree.button.isEnabled = enabled
  }

  public fun addTextFieldDocumentListener(listener: DocumentListener) {
    textFieldComponent.document.addDocumentListener(listener)
    queryChangeListeners.add(listener)
  }

  public fun registerDisplayChangeListener(onChange: () -> Unit): Unit =
    displayAsTree.registerValueChangeListener(onChange)

  public fun clearAllListeners() {
    queryChangeListeners.forEach { textFieldComponent.document.removeDocumentListener(it) }
    queryChangeListeners.clear()
    displayAsTree.clearValueChangeListeners()
  }

  public fun getCurrentSearchQuery(): String = textFieldComponent.text

  public fun isDisplayAsTreeChosen(): Boolean = displayAsTree.value
}

private class BooleanWithButton(
  buttonText: String,
  buttonIcon: Icon,
) {
  var value: Boolean = false
    private set
  val action = StickyTargetAction(buttonText, buttonIcon, { toggleValue() }) { value }
  val button: JComponent = ActionButton(
    action,
    action.templatePresentation.clone(),
    ActionPlaces.TOOLBAR,
    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE,
  )

  val displayChangeListeners: MutableList<() -> Unit> = mutableListOf()

  fun registerValueChangeListener(onChange: () -> Unit) {
    displayChangeListeners.add(onChange)
  }

  fun clearValueChangeListeners(): Unit = displayChangeListeners.clear()

  private fun toggleValue() {
    value = !value
    displayChangeListeners.forEach { it() }
  }
}
