package org.jetbrains.plugins.bsp.java

import com.intellij.ui.IconManager
import org.jetbrains.plugins.bsp.utils.SourceType
import org.jetbrains.plugins.bsp.utils.SourceTypeIconProvider

class JavaSourceTypeIconProvider : SourceTypeIconProvider {
  override fun getSourceType(): SourceType = SourceType.JAVA

  override fun getIcon() = IconManager.getInstance().getPlatformIcon(com.intellij.ui.PlatformIcons.JavaFileType)
}
