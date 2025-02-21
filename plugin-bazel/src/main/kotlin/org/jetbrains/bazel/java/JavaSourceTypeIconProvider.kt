package org.jetbrains.bazel.java

import com.intellij.ui.IconManager
import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.SourceTypeIconProvider

class JavaSourceTypeIconProvider : SourceTypeIconProvider {
  override fun getSourceType(): SourceType = SourceType.JAVA

  override fun getIcon() = IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.JavaFileType)
}
