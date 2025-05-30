package org.jetbrains.bazel.python.sync

import com.jetbrains.python.parser.icons.PythonParserIcons
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.SourceTypeIconProvider
import javax.swing.Icon

private class PythonSourceIconProvider : SourceTypeIconProvider {
  override fun getIcon(): Icon = PythonParserIcons.PythonFile

  override fun getSourceType(): SourceType = SourceType.PYTHON
}
