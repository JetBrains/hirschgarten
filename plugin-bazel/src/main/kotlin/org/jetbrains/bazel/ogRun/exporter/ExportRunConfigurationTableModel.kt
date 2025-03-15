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


import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.text.UniqueNameGenerator
import javax.swing.table.AbstractTableModel

/** Table model used by the 'export run configurations' UI.  */
internal class ExportRunConfigurationTableModel(configurations: List<RunConfiguration>) : AbstractTableModel() {
  val enabled: Array<Boolean?> = arrayOfNulls<Boolean>(configurations.size)
  val names: Array<String?> = arrayOfNulls<String>(configurations.size)
  val paths: Array<String?> = arrayOfNulls<String>(configurations.size)

  init {

    val nameGenerator = UniqueNameGenerator()
    for (i in configurations.indices) {
      val config = configurations[i]
      enabled[i] = false
      names[i] = config.name
      paths[i] =
        nameGenerator.generateUniqueName(FileUtil.sanitizeFileName(config.name), "", ".xml")
    }
  }

  override fun getColumnCount(): Int = 3

  override fun getColumnClass(columnIndex: Int): Class<*>? = COLUMN_CLASSES.get(columnIndex)

  override fun getColumnName(column: Int): String? = COLUMN_NAMES.get(column)

  override fun getRowCount(): Int = enabled.size

  override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex != 1

  override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? {
    return when (columnIndex) {
      0 -> enabled[rowIndex]
      1 -> names[rowIndex]
      2 -> paths[rowIndex]
      else -> throw RuntimeException("Invalid column index: $columnIndex")
    }
  }

  override fun setValueAt(
    aValue: Any?,
    rowIndex: Int,
    columnIndex: Int,
  ) {
    when (columnIndex) {
      0 -> {
        enabled[rowIndex] = aValue as Boolean?
        return
      }

      1 -> {
        names[rowIndex] = aValue as String?
        return
      }

      2 -> {
        paths[rowIndex] = aValue as String?
        return
      }

      else -> throw RuntimeException("Invalid column index: $columnIndex")
    }
  }

  companion object {
    private val COLUMN_NAMES: List<String?> =
      listOf<String?>("Export", "Name", "Output filename")
    private val COLUMN_CLASSES: List<Class<*>?> =
      listOf<Class<*>?>(Boolean::class.java, String::class.java, String::class.java)
  }
}
