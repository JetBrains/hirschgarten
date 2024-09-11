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
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.SimpleDocumentListener
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

public class SearchBarPanel : JPanel(BorderLayout()) {
  public var inProgress: Boolean by AtomicBoolean {
    repaintLoading()
  }

  private val searchLoadingExtension: TextComponentExtension.Indicator =
    TextComponentExtension.Indicator(
      trueIcon = AnimatedIcon.Default.INSTANCE,
      falseIcon = AllIcons.Actions.Find,
      predicate = { inProgress },
      beforeText = true,
    )

  private val textField = prepareTextField()
  private val displayChangeListeners = mutableListOf<() -> Unit>()
  private val queryChangeListeners = mutableListOf<(Regex) -> Unit>()

  private var matchCase: Boolean by AtomicBoolean(::onQueryChange)
  private var displayAsTree: Boolean by AtomicBoolean(::displayAsTreeChanged)
  private var regexMode: Boolean by AtomicBoolean(::onQueryChange)

  private val textFieldComponent = JBTextField()

  init {
    add(textField)
  }

  private fun repaintLoading() {
    // without removing and adding the extension back, the icon does not repaint for some reason
    textField.removeExtension(searchLoadingExtension)
    textField.addExtension(searchLoadingExtension)
    textField.repaint()
  }

  private fun prepareTextField(): ExtendableTextField {
    val newField = ExtendableTextField()
    val matchCaseExtension =
      TextComponentExtension.Switch(
        // MatchCaseHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
        icon = AllIcons.Actions.MatchCaseHovered,
        valueGetter = { matchCase },
        valueSetter = { matchCase = it },
        parentComponent = newField,
        tooltip = BspPluginBundle.message("widget.target.search.match.case"),
      )
    val regexExtension =
      TextComponentExtension.Switch(
        // RegexHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
        icon = AllIcons.Actions.RegexHovered,
        valueGetter = { regexMode },
        valueSetter = { regexMode = it },
        parentComponent = newField,
        tooltip = BspPluginBundle.message("widget.target.search.regex"),
      )
    val treeExtension =
      TextComponentExtension.Switch(
        icon = AllIcons.Actions.ShowAsTree,
        valueGetter = { displayAsTree },
        valueSetter = { displayAsTree = it },
        parentComponent = newField,
        tooltip = BspPluginBundle.message("widget.target.search.display.as.tree"),
      )
    val clearExtension =
      TextComponentExtension.Clear(
        isEmpty = { newField.text.isEmpty() },
        clearAction = ::clearQuery,
        tooltip = BspPluginBundle.message("widget.target.search.clear"),
      )
    return newField.apply {
      addExtension(searchLoadingExtension)
      addExtension(treeExtension)
      addExtension(regexExtension)
      addExtension(matchCaseExtension)
      addExtension(clearExtension)
      this.document.addDocumentListener(SimpleDocumentListener(::onQueryChange))
    }
  }

  private fun clearQuery() {
    textField.text = ""
    onQueryChange()
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
    focusFindAction.registerCustomShortcutSet(FIND_SHORTCUT_SET, component)

    val toggleRegexAction = SimpleAction { regexMode = !regexMode }
    toggleRegexAction.registerCustomShortcutSet(REGEX_SHORTCUT_SET, component)
  }

  public fun getCurrentSearchQuery(): Regex {
    val options =
      setOfNotNull(
        if (!matchCase) RegexOption.IGNORE_CASE else null,
        if (!regexMode) RegexOption.LITERAL else null,
      )
    return textField.text.toRegex(options)
  }

  public fun isDisplayAsTreeChosen(): Boolean = displayAsTree

  public fun isEmpty(): Boolean = textField.text.isEmpty()
}

private class AtomicBoolean(private val changeListener: (() -> Unit)?) : ReadWriteProperty<SearchBarPanel, Boolean> {
  var theValue: Boolean by AtomicProperty(false)

  override fun getValue(thisRef: SearchBarPanel, property: KProperty<*>): Boolean = theValue

  override fun setValue(
    thisRef: SearchBarPanel,
    property: KProperty<*>,
    value: Boolean,
  ) {
    theValue = value
    changeListener?.invoke()
  }
}

private class SimpleAction(private val action: () -> Unit) : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    action()
  }
}

private val FIND_SHORTCUT_SET = CommonShortcuts.getFind()

// regex shortcut is hardcoded - couldn't find this shortcut defined anywhere in the platform
private val REGEX_SHORTCUT_SET = CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK))
