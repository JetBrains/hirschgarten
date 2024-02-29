package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.StickyTargetAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentListener

public class SearchBarPanel : JPanel(BorderLayout()) {
  private val textField = prepareTextField()
  private var regexTEMP = false // todo - temporary
  private var treeTEMP = false // todo - temporary

  private val textFieldComponent = JBTextField()
  private val displayAsTree = BooleanWithButton(
    buttonText = BspPluginBundle.message("widget.target.search.display.as.tree"),
    buttonIcon = AllIcons.Actions.ShowAsTree,
  )

  private val queryChangeListeners = mutableListOf<DocumentListener>()

  init {
    add(textField)
    add(displayAsTree.button, BorderLayout.EAST)
  }

  private fun prepareTextField(): ExtendableTextField {
    val newField = ExtendableTextField()
    val searchLoadingExtension = TextComponentExtension.Indicator(
      trueIcon = AnimatedIcon.Default.INSTANCE,
      falseIcon = AllIcons.Actions.Find,
      predicate = ::isLoading,
      beforeText = true,
    )
    val regexExtension = TextComponentExtension.Switch(
      icon = AllIcons.Actions.Regex,
      valueGetter = { regexTEMP },
      valueSetter = { regexTEMP = it },
      parentComponent = newField,
    )
    val treeExtension = TextComponentExtension.Switch(
      icon = AllIcons.Actions.ShowAsTree,
      valueGetter = { treeTEMP },
      valueSetter = { treeTEMP = it },
      parentComponent = newField,
    )
    return newField.apply {
      addExtension(searchLoadingExtension)
      addExtension(treeExtension)
      addExtension(regexExtension)
    }
  }

  private fun isLoading(): Boolean {
    return try {
      textField.text.isNotEmpty()
    } catch (_: NullPointerException) {
      false
    }
  } // todo - implement real logic, this one is just for testing

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    textField.isEnabled = enabled
    textFieldComponent.isEnabled = enabled
    displayAsTree.button.isEnabled = enabled
  }

  public fun addTextFieldDocumentListener(listener: DocumentListener) {
    textField.document.addDocumentListener(listener)
    queryChangeListeners.add(listener)
  }

  public fun registerDisplayChangeListener(onChange: () -> Unit): Unit =
    displayAsTree.registerValueChangeListener(onChange)

  public fun clearAllListeners() {
    queryChangeListeners.forEach { textFieldComponent.document.removeDocumentListener(it) }
    queryChangeListeners.clear()
    displayAsTree.clearValueChangeListeners()
  }

  public fun getCurrentSearchQuery(): String = textField.text

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
