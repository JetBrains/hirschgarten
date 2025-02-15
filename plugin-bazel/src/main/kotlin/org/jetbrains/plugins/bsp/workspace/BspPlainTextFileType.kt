package org.jetbrains.plugins.bsp.workspace

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.fileTypes.PlainTextLikeFileType
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons
import org.jetbrains.plugins.bsp.utils.SourceType
import org.jetbrains.plugins.bsp.utils.SourceTypeIconProvider
import javax.swing.Icon

class BspPlainTextFileType(private val fileExtension: String = "txt") :
  LanguageFileType(PlainTextLanguage.INSTANCE),
  PlainTextLikeFileType {
  override fun getName(): String = "BSP_PLAIN_TEXT"

  override fun getDescription(): String = "BSP plain text"

  override fun getDefaultExtension(): String = "txt"

  override fun getIcon(): Icon {
    val sourceType = SourceType.fromExtension(fileExtension)
    return SourceTypeIconProvider.getIconBySourceType(sourceType) ?: IconManager.getInstance().getPlatformIcon(PlatformIcons.TextFileType)
  }

  companion object {
    /**
     * it is used in XML file
     */
    @Suppress("UNUSED")
    val INSTANCE = BspPlainTextFileType()
  }
}
