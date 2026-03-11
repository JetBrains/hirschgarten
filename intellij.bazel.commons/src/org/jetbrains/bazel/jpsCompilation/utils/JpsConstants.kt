package org.jetbrains.bazel.jpsCompilation.utils

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass

@ApiStatus.Internal
object JpsConstants {
  val SUPPORTED_LANGUAGES: List<LanguageClass> =
    listOf(
      LanguageClass.JAVA,
      LanguageClass.KOTLIN,
      LanguageClass.SCALA,
    )
}
