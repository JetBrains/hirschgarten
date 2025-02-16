package org.jetbrains.bazel.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleDocumentListener
import org.jetbrains.bazel.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class SearchBarPanel : JPanel(BorderLayout()) {
  var inProgress: Boolean by AtomicBoolean {
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

  private val focusFindAction = SimpleAction { textField.requestFocus() }
  private val toggleMatchCaseAction = SimpleAction { matchCase = !matchCase }
  private val toggleRegexAction = SimpleAction { regexMode = !regexMode }

  private val textFieldComponent = JBTextField()

  init {
    add(textField)
    toggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, this)
    toggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, this)
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
        tooltip =
          BspPluginBundle.message(
            "widget.target.search.match.case",
            KeymapUtil.getFirstKeyboardShortcutText(BspShortcuts.matchCase),
          ),
      )
    val regexExtension =
      TextComponentExtension.Switch(
        // RegexHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
        icon = AllIcons.Actions.RegexHovered,
        valueGetter = { regexMode },
        valueSetter = { regexMode = it },
        parentComponent = newField,
        tooltip = BspPluginBundle.message("widget.target.search.regex", KeymapUtil.getFirstKeyboardShortcutText(BspShortcuts.regexMode)),
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
      this.emptyText.text =
        BspPluginBundle.message("widget.target.search.hint", KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getFind()))
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

  fun registerDisplayChangeListener(onChange: () -> Unit) {
    displayChangeListeners.add(onChange)
  }

  fun registerQueryChangeListener(onChange: (Regex) -> Unit) {
    queryChangeListeners.add(onChange)
  }

  fun clearAllListeners() {
    displayChangeListeners.clear()
    queryChangeListeners.clear()
  }

  fun registerSearchShortcutsOn(component: JComponent) {
    focusFindAction.registerCustomShortcutSet(CommonShortcuts.getFind(), component)
    toggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, component)
    toggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, component)
  }

  fun getCurrentSearchQuery(): Regex {
    val options =
      setOfNotNull(
        if (!matchCase) RegexOption.IGNORE_CASE else null,
        if (!regexMode) RegexOption.LITERAL else null,
      )
    return textField.text.toRegex(options)
  }

  fun isDisplayAsTreeChosen(): Boolean = displayAsTree

  fun isEmpty(): Boolean = textField.text.isEmpty()
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
