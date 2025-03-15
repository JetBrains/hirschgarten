/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.exporter

import com.google.common.base.Strings

import com.google.idea.blaze.base.io.FileOperationProvider
import com.intellij.execution.RunManager;
import java.io.FileOutputStream;

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.ui.table.JBTable
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.bazel.ogRun.BlazeRunConfiguration
import java.awt.Dimension
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
import java.util.stream.Collectors
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable

/** UI for exporting run configurations.  */
class ExportRunConfigurationDialog internal constructor(project: Project) : DialogWrapper(project, true) {
  private val configurations: List<RunConfiguration> =
    RunManager
      .getInstance(project)
      .allConfigurationsList
      .sortedWith(COMPARATOR)
      .toMutableList()
  private val tableModel: ExportRunConfigurationTableModel = ExportRunConfigurationTableModel(configurations)
  private val table: JBTable = JBTable(tableModel)
  private val outputDirectoryPanel: FieldPanel

  init {
    val booleanColumn = table.columnModel.getColumn(0)
    booleanColumn.setCellRenderer(BooleanTableCellRenderer())
    booleanColumn.setCellEditor(BooleanTableCellEditor())
    val width = table.getFontMetrics(table.font).stringWidth(table.getColumnName(0)) + 10
    booleanColumn.setPreferredWidth(width)
    booleanColumn.setMinWidth(width)
    booleanColumn.setMaxWidth(width)

    table
      .columnModel
      .getColumn(2)
      .setCellEditor(DefaultCellEditor(GuiUtils.createUndoableTextField()))

    val nameColumn = table.columnModel.getColumn(1)
    nameColumn.setCellRenderer(
      object : ColoredTableCellRenderer() {
        override fun customizeCellRenderer(
          table: JTable,
          value: Any?,
          isSelected: Boolean,
          hasFocus: Boolean,
          row: Int,
          col: Int,
        ) {
          val config = configurations[row]
          setIcon(config.getType().icon)
          append(config.name)
        }
      },
    )

    table.preferredSize = Dimension(700, 700)
    table.setShowColumns(true)

    val browseAction = ActionListener { chooseDirectory() }
    outputDirectoryPanel =
      FieldPanel("Export configurations to directory:", null, browseAction, null)
    val defaultExportDirectory: File? = defaultExportDirectory(project)
    if (defaultExportDirectory != null) {
      outputDirectoryPanel.text = defaultExportDirectory.path
    }

    title = "Export Bazel Run Configurations"
    init()
  }

  private val outputDirectoryPath: String
    get() = Strings.nullToEmpty(outputDirectoryPanel.text).trim { it <= ' ' }

  private fun chooseDirectory() {
    val descriptor =
      FileChooserDescriptorFactory
        .createSingleFolderDescriptor()
        .withTitle("Export Directory Location")
        .withDescription("Choose directory to export run configurations to")
        .withHideIgnored(false)
    val chooser =
      FileChooserFactory.getInstance().createFileChooser(descriptor, null, null)

    val files: Array<VirtualFile>
    val existingLocation = File(this.outputDirectoryPath)
    if (existingLocation.exists()) {
      val toSelect =
        LocalFileSystem.getInstance().refreshAndFindFileByPath(existingLocation.path)
      files = chooser.choose(null, toSelect)
    } else {
      files = chooser.choose(null)
    }
    if (files.isEmpty()) {
      return
    }
    val file = files[0]
    outputDirectoryPanel.text = file.path
  }

  override fun doValidate(): ValidationInfo? {
    val outputDir = this.outputDirectoryPath
    if (outputDir.isEmpty()) {
      return ValidationInfo("Choose an output directory")
    }
    if (!FileOperationProvider.getInstance().exists(File(outputDir))) {
      return ValidationInfo("Invalid output directory")
    }
    val names: MutableSet<String?> = HashSet<String?>()
    for (i in configurations.indices) {
      if (tableModel.enabled[i] != true) {
        continue
      }
      if (!names.add(tableModel.paths[i])) {
        return ValidationInfo("Duplicate output file name '" + tableModel.paths[i] + "'")
      }
    }
    return null
  }

  override fun doOKAction() {
    val outputDir = File(this.outputDirectoryPath)
    val outputFiles: MutableList<File?> = ArrayList<File?>()
    for (i in configurations.indices) {
      if (tableModel.enabled[i] != true) {
        continue
      }
      val outputFile = File(outputDir, tableModel.paths[i])
      writeConfiguration(configurations.get(i), outputFile)
      outputFiles.add(outputFile)
    }
    LocalFileSystem.getInstance().refreshIoFiles(outputFiles)
    super.doOKAction()
  }

  override fun createNorthPanel(): JComponent = outputDirectoryPanel

  override fun createCenterPanel(): JComponent {
    val panel: JPanel = JPanel(BorderLayout())
    panel.setBorder(IdeBorderFactory.createTitledBorder("Run Configurations", false))
    panel.add(
      ToolbarDecorator
        .createDecorator(table)
        .addExtraAction(ExportRunConfigurationDialog.SelectAllButton())
        .createPanel(),
      BorderLayout.CENTER,
    )
    return panel
  }

  private inner class SelectAllButton : AnActionButton("Select All", AllIcons.Actions.Selectall) {
    var allSelected: Boolean = false

    @Synchronized
    override fun actionPerformed(anActionEvent: AnActionEvent) {
      val newState = !allSelected
      for (i in tableModel.enabled.indices) {
        table.setValueAt(newState, i, 0)
      }
      allSelected = newState
      val presentation = anActionEvent.presentation
      if (allSelected) {
        presentation.setText("Deselect All")
        presentation.setIcon(AllIcons.Actions.Unselectall)
      } else {
        presentation.setText("Select All")
        presentation.setIcon(AllIcons.Actions.Selectall)
      }
      tableModel.fireTableDataChanged()
      table.revalidate()
      table.repaint()
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
  }

  companion object {
    // show blaze run configurations first, otherwise sort by name
    private val COMPARATOR =
      Comparator { o1: RunConfiguration?, o2: RunConfiguration? ->
        if (o1 is BlazeRunConfiguration != o2 is BlazeRunConfiguration) {
          return@Comparator if (o1 is BlazeRunConfiguration) -1 else 1
        }
        o1!!.name.compareTo(o2!!.name)
      }

    /** Try to find a checked-in project view file. Otherwise, fall back to the workspace root.  */
    private fun defaultExportDirectory(project: Project): File? {
      val workspaceRoot: WorkspaceRoot? = WorkspaceRoot.fromProjectSafe(project)
      if (workspaceRoot == null) {
        return null
      }
      val projectViewSet: ProjectViewSet? = ProjectViewManager.getInstance(project).getProjectViewSet()
      if (projectViewSet != null) {
        for (projectViewFile in projectViewSet.getProjectViewFiles()) {
          val file: File? = projectViewFile.projectViewFile
          if (file != null && FileUtil.isAncestor(workspaceRoot.directory(), file, false)) {
            return file.getParentFile()
          }
        }
      }
      return workspaceRoot.directory()
    }

    private fun writeConfiguration(configuration: RunConfiguration, outputFile: File) {
      try {
        FileOutputStream(outputFile, false).use { writer ->
          val xmlOutputter = XMLOutputter(Format.getCompactFormat())
          xmlOutputter.output(RunConfigurationSerializer.writeToXml(configuration), writer)
        }
      } catch (e: IOException) {
        throw RuntimeException("Error exporting run configuration to file: $outputFile")
      }
    }
  }
}
