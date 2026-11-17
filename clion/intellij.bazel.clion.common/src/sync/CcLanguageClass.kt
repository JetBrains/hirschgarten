package org.jetbrains.bazel.clion.sync

import com.jetbrains.cidr.lang.OCFileTypeHelpers
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider
import org.jetbrains.bazel.commons.TargetKind

val CC_LANGUAGE_CLASS: LanguageClass = LanguageClass(
  "C/C++",
  // allExtensions lowercases all entries, so we add uppercase additionally
  OCFileTypeHelpers.allExtensions().filter { it.isNotEmpty() }.toSet() +
  setOf("C", "H", "M"),
)

internal class CcLanguageClassProvider : LanguageClassProvider {

  override val languages: List<LanguageClass> get() = listOf(CC_LANGUAGE_CLASS)
}

@ApiStatus.Internal
fun TargetKind.includesCc(): Boolean = languageClasses.contains(CC_LANGUAGE_CLASS)
