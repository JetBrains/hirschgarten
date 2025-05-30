package org.jetbrains.bazel.kotlin.configuration

import org.jetbrains.bazel.utils.SourceType
import org.jetbrains.bazel.utils.SourceTypeIconProvider
import org.jetbrains.kotlin.idea.KotlinIconProviderService
import javax.swing.Icon

private class KotlinSourceTypeIconProvider : SourceTypeIconProvider {
  override fun getSourceType(): SourceType = SourceType.KOTLIN

  override fun getIcon(): Icon? = KotlinIconProviderService.getInstance().fileIcon
}
