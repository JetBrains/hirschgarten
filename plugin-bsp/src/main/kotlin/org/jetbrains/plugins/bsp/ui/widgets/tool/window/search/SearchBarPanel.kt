package org.jetbrains.plugins.bsp.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public class SearchBarPanel : JPanel(BorderLayout()) {
  public var inProgress: Boolean by AtomicBoolean {
    repaintLoading()
  }

  private val searchLoadingExtension: TextComponentExtension.Indicator = TextComponentExtension.Indicator(
    trueIcon = AnimatedIcon.Default.INSTANCE,
    falseIcon = AllIcons.Actions.Find,
    predicate = { inProgress },
    beforeText = true,
  )

  private val textField = prepareTextField()
  private val displayChangeListeners = mutableListOf<() -> Unit>()
  private val queryChangeListeners = mutableListOf<(Regex) -> Unit>()

  private var displayAsTree: Boolean by AtomicBoolean(::displayAsTreeChanged)
  private var regexMode: Boolean by AtomicBoolean(::onQueryChange)

  private val textFieldComponent = JBTextField()

  init {
    add(textField)
  }

  private fun repaintLoading() {
    // without removing and adding the extension, the icon does not repaint for some reason
    textField.removeExtension(searchLoadingExtension)
    textField.addExtension(searchLoadingExtension)
    textField.repaint()
  }

  private fun prepareTextField(): ExtendableTextField {
    val newField = ExtendableTextField()
    val regexExtension = TextComponentExtension.Switch(
      // RegexHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
      icon = AllIcons.Actions.RegexHovered,
      valueGetter = { regexMode },
      valueSetter = { regexMode = it },
      parentComponent = newField,
      tooltip = BspPluginBundle.message("widget.target.search.regex"),
    )
    val treeExtension = TextComponentExtension.Switch(
      icon = AllIcons.Actions.ShowAsTree,
      valueGetter = { displayAsTree },
      valueSetter = { displayAsTree = it },
      parentComponent = newField,
      tooltip = BspPluginBundle.message("widget.target.search.display.as.tree")
    )
    val clearExtension = TextComponentExtension.Clear(
      isEmpty = { newField.text.isEmpty() },
      clearAction = ::clearQuery,
      tooltip = BspPluginBundle.message("widget.target.search.clear"),
    )
    return newField.apply {
      addExtension(searchLoadingExtension)
      addExtension(treeExtension)
      addExtension(regexExtension)
      addExtension(clearExtension)
      this.document.addDocumentListener(SimpleTextChangeListener(::onQueryChange))
    }
  }

  private fun displayAsTreeChanged() {
    displayChangeListeners.forEach { it() }
  }

  private fun onQueryChange() {
    queryChangeListeners.forEach { it(getCurrentSearchQuery()) }
    if (textField.text.isEmpty()) inProgress = false
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

  public fun registerShortcutsOn(component: JComponent) {
    val focusFindAction = SimpleAction { textField.requestFocus() }
    val findKeySet = CommonShortcuts.getFind()
    focusFindAction.registerCustomShortcutSet(findKeySet, component)

    val toggleRegexAction = SimpleAction { regexMode = !regexMode }
    // hardcoded - couldn't find this shortcut defined anywhere in the platform
    val regexKeySet = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK))
    toggleRegexAction.registerCustomShortcutSet(regexKeySet, component)
  }

  public fun getCurrentSearchQuery(): Regex =
    if (regexMode) {
      textField.text.toRegex()
    } else {
      textField.text.toRegex(RegexOption.LITERAL)
    }

  private fun clearQuery() {
    textField.text = ""
    onQueryChange()
  }

  public fun isDisplayAsTreeChosen(): Boolean = displayAsTree

  public fun isEmpty(): Boolean = textField.text.isEmpty()
}

private class AtomicBoolean(
  private val changeListener: (() -> Unit)?
) : ReadWriteProperty<SearchBarPanel, Boolean> {
  var theValue: Boolean by AtomicProperty(false)

  override fun getValue(thisRef: SearchBarPanel, property: KProperty<*>): Boolean = theValue

  override fun setValue(thisRef: SearchBarPanel, property: KProperty<*>, value: Boolean) {
    theValue = value
    changeListener?.invoke()
  }
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

private class SimpleAction(private val action: () -> Unit) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) { action() }
}
