/*
 * Copyright 2020 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.ogRun.ui

import com.google.common.collect.ImmutableList
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.AbstractAction
import javax.swing.AbstractCellEditor
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.table.TableCellEditor
import kotlin.concurrent.Volatile

// TODO: use JBPanel instead of JPanel

/** UI component for a list of [Label]s with autocomplete.  */
class TargetExpressionListUi(private val project: Project) : JPanel() {
  private val listModel: ListTableModel<TargetItem?>
  private val tableView: TableView<TargetItem?>

  init {
    listModel = ListTableModel<TargetItem?>(TargetColumn())
    tableView = TableView<TargetItem?>(listModel)
    tableView.emptyText.text = "Choose some targets"
    tableView.preferredScrollableViewportSize = Dimension(200, tableView.getRowHeight() * 4)

    setLayout(BorderLayout())
    add(
      ToolbarDecorator
        .createDecorator(tableView)
        .setAddAction { addTarget() }
        .setRemoveAction { removeTarget() }
        .disableUpDownActions()
        .createPanel(),
      BorderLayout.CENTER,
    )
  }

  var targetExpressions: List<String>
    /** Returns the non-empty target patterns presented in the UI component.  */
    get() =
      listModel
        .items
        .mapNotNull { it?.expression?.trim { it <= ' ' } }
        .filter { it.isNotEmpty() }
    set(targets) {
      listModel.setItems(
        targets
          .filter { it.isNotBlank() }
          .map { TargetItem(it) },
      )
    }

  override fun setEnabled(enabled: Boolean) {
    super.setEnabled(enabled)
    tableView.setEnabled(enabled)
    tableView.setRowSelectionAllowed(enabled)
  }

  private fun addTarget() {
    TableUtil.stopEditing(tableView) // save any partially-filled state

    listModel.addRow(TargetItem(""))
    val index = listModel.rowCount - 1
    tableView.getSelectionModel().setSelectionInterval(index, index)
    tableView.scrollRectToVisible(
      tableView.getCellRect(index, /* column= */0, /* includeSpacing= */true),
    )
    TableUtil.editCellAt(tableView, index, /* column= */0)
  }

  private fun removeTarget() {
    TableUtil.removeSelectedItems(tableView)
  }

  private inner class TargetColumn : ColumnInfo<TargetItem, String>( /* name= */"") {
    override fun valueOf(targetItem: TargetItem): String = targetItem.expression

    override fun setValue(targetItem: TargetItem, value: String) {
      targetItem.expression = value
    }

    override fun getEditor(targetItem: TargetItem?): TableCellEditor = TargetListCellEditor(project)

    override fun isCellEditable(targetItem: TargetItem?): Boolean = true
  }

  private class TargetItem(internal var expression: String)

  private class TargetListCellEditor(private val project: Project) :
    AbstractCellEditor(),
    TableCellEditor {
    @Volatile
    private var textField: TextFieldWithAutoCompletion<String>? = null

    override fun getTableCellEditorComponent(
      table: JTable?,
      value: Any?,
      isSelected: Boolean,
      row: Int,
      column: Int,
    ): Component? {
      val textField =
        TextFieldWithAutoCompletion(
          project,
          TargetCompletionProvider(project), // showCompletionHint=
          true, // text=
          value as String?,
        )
      textField.addSettingsProvider { editorEx: EditorEx ->
        // base class ignores 'enter' keypress events, causing entire dialog to close without
        // committing changes... fix copied from upstream PsiClassTableCellEditor
        val c: JComponent = editorEx.contentComponent
        c.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "ENTER")
        c
          .actionMap
          .put(
            "ENTER",
            object : AbstractAction() {
              override fun actionPerformed(e: ActionEvent) {
                stopCellEditing()
              }
            },
          )
      }
      textField.setBorder(BorderFactory.createLineBorder(JBColor.BLACK))
      this.textField = textField
      return textField
    }

    override fun isCellEditable(e: EventObject?): Boolean {
      if (e !is MouseEvent) {
        return true
      }
      return e.getClickCount() >= 2
    }

    override fun getCellEditorValue(): String = textField?.getText() ?: ""
  }

  private class TargetCompletionProvider(project: Project?) :
    TextFieldWithAutoCompletion.StringsCompletionProvider(
      getTargets(project),
      null,
    ) {
    companion object {
      private fun getTargets(project: Project?): MutableCollection<String?>? {
        val projectData: BlazeProjectData? =
          BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
        val importSettings: BlazeImportSettings? =
          BlazeImportSettingsManager.getInstance(project).getImportSettings()
        val projectViewSet: ProjectViewSet? = ProjectViewManager.getInstance(project).getProjectViewSet()
        if (projectData == null || importSettings == null || projectViewSet == null) {
          return ImmutableList.of<String?>()
        }
        val importRoots: ImportRoots =
          ImportRoots
            .builder(
              WorkspaceRoot.fromImportSettings(importSettings),
              importSettings.getBuildSystem(),
            ).add(projectViewSet)
            .build()

        return projectData
          .getTargetMap()
          .targets()
          .stream()
          .filter(TargetIdeInfo::isPlainTarget)
          .map(TargetIdeInfo::getKey)
          .map(TargetKey::getLabel)
          .filter(importRoots::importAsSource)
          .map(Label::toString)
          .collect(ImmutableList.toImmutableList<E?>())
      }
    }
  }
}
