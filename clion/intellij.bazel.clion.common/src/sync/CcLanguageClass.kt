package org.jetbrains.bazel.clion.sync

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider
import org.jetbrains.bazel.commons.TargetKind

val CC_LANGUAGE_CLASS = LanguageClass("cc", emptySet())

internal class CcLanguageClassProvider : LanguageClassProvider {

  override val languages: List<LanguageClass> get() = listOf(CC_LANGUAGE_CLASS)
}

@ApiStatus.Internal
fun TargetKind.includesCc(): Boolean = languageClasses.contains(CC_LANGUAGE_CLASS)
