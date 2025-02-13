package org.jetbrains.plugins.bsp.kotlin

import org.jetbrains.kotlin.idea.KotlinIconProviderService
import org.jetbrains.plugins.bsp.utils.SourceType
import org.jetbrains.plugins.bsp.utils.SourceTypeIconProvider
import javax.swing.Icon

class KotlinSourceTypeIconProvider : SourceTypeIconProvider {
  override fun getSourceType(): SourceType = SourceType.KOTLIN

  override fun getIcon(): Icon? = KotlinIconProviderService.getInstance().fileIcon
}
