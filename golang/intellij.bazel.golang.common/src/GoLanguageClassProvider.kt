package org.jetbrains.bazel.golang

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

@ApiStatus.Internal
object GoLanguageClass {
  val GO: LanguageClass = LanguageClass("go", setOf("go"))
}

internal class GoLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = listOf(GoLanguageClass.GO)
}
