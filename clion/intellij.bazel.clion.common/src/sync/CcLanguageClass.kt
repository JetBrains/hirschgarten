package org.jetbrains.bazel.clion.sync

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

val CC_LANGUAGE_CLASS = LanguageClass("cc", emptySet())

internal class CcLanguageClassProvider : LanguageClassProvider {

  override val languages: List<LanguageClass> get() = listOf(CC_LANGUAGE_CLASS)
}
