package org.jetbrains.bazel.ui.widgets.tool.window.search

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.fields.ExtendableTextField
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.ui.widgets.tool.window.model.BuildTargetsModel
import org.jetbrains.bazel.ui.widgets.tool.window.utils.BspShortcuts
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleAction
import org.jetbrains.bazel.ui.widgets.tool.window.utils.SimpleDocumentListener
import org.jetbrains.bazel.ui.widgets.tool.window.utils.TextComponentExtension
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class SearchBarPanel(private val model: BuildTargetsModel) : JBPanel<SearchBarPanel>(BorderLayout()) {
  private val searchLoadingExtension: TextComponentExtension.Indicator =
    TextComponentExtension.Indicator(
      trueIcon = AnimatedIcon.Default.INSTANCE,
      falseIcon = AllIcons.Actions.Find,
      predicate = { model.searchInProgress },
      beforeText = true,
    )

  private val textField = prepareTextField()

  private val focusFindAction = SimpleAction { textField.requestFocus() }
  private val toggleMatchCaseAction = SimpleAction { 
    model.matchCase = !model.matchCase
  }
  private val toggleRegexAction = SimpleAction { 
    model.regexMode = !model.regexMode
  }

  init {
    add(textField)
    toggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, this)
    toggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, this)

    // Listen for model changes
    model.addListener {
      repaintLoading()
    }
  }

  private fun repaintLoading() {
    // Avoid removing and adding the extension if it's not necessary
    // This prevents a potential infinite recursion
    textField.repaint()
  }

  private fun prepareTextField(): ExtendableTextField {
    val newField = ExtendableTextField()
    val matchCaseExtension =
      TextComponentExtension.Switch(
        // MatchCaseHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
        icon = AllIcons.Actions.MatchCaseHovered,
        valueGetter = { model.matchCase },
        valueSetter = { model.matchCase = it },
        parentComponent = newField,
        tooltip =
          BazelPluginBundle.message(
            "widget.target.search.match.case",
            KeymapUtil.getFirstKeyboardShortcutText(BspShortcuts.matchCase),
          ),
      )
    val regexExtension =
      TextComponentExtension.Switch(
        // RegexHovered icon matches the ShowAsTree icon better than the normal Regex; on new UI both icons look the same
        icon = AllIcons.Actions.RegexHovered,
        valueGetter = { model.regexMode },
        valueSetter = { model.regexMode = it },
        parentComponent = newField,
        tooltip = BazelPluginBundle.message("widget.target.search.regex", KeymapUtil.getFirstKeyboardShortcutText(BspShortcuts.regexMode)),
      )
    val treeExtension =
      TextComponentExtension.Switch(
        icon = AllIcons.Actions.ShowAsTree,
        valueGetter = { model.displayAsTree },
        valueSetter = { model.displayAsTree = it },
        parentComponent = newField,
        tooltip = BazelPluginBundle.message("widget.target.search.display.as.tree"),
      )
    val clearExtension =
      TextComponentExtension.Clear(
        isEmpty = { newField.text.isEmpty() },
        clearAction = ::clearQuery,
        tooltip = BazelPluginBundle.message("widget.target.search.clear"),
      )
    return newField.apply {
      this.emptyText.text =
        BazelPluginBundle.message("widget.target.search.hint", KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getFind()))
      addExtension(searchLoadingExtension)
      addExtension(treeExtension)
      addExtension(regexExtension)
      addExtension(matchCaseExtension)
      addExtension(clearExtension)
      this.document.addDocumentListener(SimpleDocumentListener {
        model.searchQuery = textField.text
      })
    }
  }

  private fun clearQuery() {
    textField.text = ""
  }


  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    textField.isEnabled = enabled
  }

  fun registerSearchShortcutsOn(component: JComponent) {
    focusFindAction.registerCustomShortcutSet(CommonShortcuts.getFind(), component)
    toggleMatchCaseAction.registerCustomShortcutSet(BspShortcuts.matchCase, component)
    toggleRegexAction.registerCustomShortcutSet(BspShortcuts.regexMode, component)
  }

  fun isEmpty(): Boolean = textField.text.isEmpty()
}
