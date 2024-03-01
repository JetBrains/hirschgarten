package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.properties.Delegates

public class SearchBarPanel : JPanel(BorderLayout()) {
  private val textField = prepareTextField()
  private val displayChangeListeners = mutableListOf<() -> Unit>()
  private val queryChangeListeners = mutableListOf<(Regex) -> Unit>()

  public var displayAsTree: Boolean by Delegates.observable(false) { _, _, _ ->
    displayAsTreeChanged()
  }
    private set

  private var regexMode: Boolean by Delegates.observable(false) { _, _, _ ->
    onQueryChange()
  }

  private val textFieldComponent = JBTextField()

  init {
    add(textField)
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
      valueGetter = { regexMode },
      valueSetter = { regexMode = it },
      parentComponent = newField,
    )
    val treeExtension = TextComponentExtension.Switch(
      icon = AllIcons.Actions.ShowAsTree,
      valueGetter = { displayAsTree },
      valueSetter = { displayAsTree = it },
      parentComponent = newField,
    )
    return newField.apply {
      addExtension(searchLoadingExtension)
      addExtension(treeExtension)
      addExtension(regexExtension)
      this.document.addDocumentListener(SimpleTextChangeListener(::onQueryChange))
    }
  }

  private fun isLoading(): Boolean {
    return try {
      textField.text.isNotEmpty()
    } catch (_: NullPointerException) {
      false
    }
  } // todo - implement real logic, this one is just for testing

  private fun displayAsTreeChanged() {
    displayChangeListeners.forEach { it() }
  }

  private fun onQueryChange() {
    queryChangeListeners.forEach { it(getCurrentSearchQuery()) }
  }

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    textField.isEnabled = enabled
    textFieldComponent.isEnabled = enabled
  }

  public fun registerDisplayChangeListener(onChange: () -> Unit) {
    displayChangeListeners.add(onChange)
  }

  public fun registerQueryChangeListener(onChange: (Regex) -> Unit) {
    queryChangeListeners.add(onChange)
  }

  public fun clearAllListeners() {
    displayChangeListeners.clear()
    queryChangeListeners.clear()
  }

  public fun getCurrentSearchQuery(): Regex =
    if (regexMode) {
      textField.text.toRegex()
    } else {
      textField.text.toRegex(RegexOption.LITERAL)
    }

  public fun isDisplayAsTreeChosen(): Boolean = displayAsTree

  public fun isEmpty(): Boolean = textField.text.isEmpty()
}

private class SimpleTextChangeListener(val onUpdate: () -> Unit) : DocumentListener {
  override fun insertUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun removeUpdate(e: DocumentEvent?) {
    onUpdate()
  }

  override fun changedUpdate(e: DocumentEvent?) {
    onUpdate()
  }
}
