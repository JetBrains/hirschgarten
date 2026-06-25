package org.jetbrains.bazel.golang

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.LanguageClassProvider

internal object GoLanguageClass {
  val GO = LanguageClass("go", setOf("go"))
}

internal class GoLanguageClassProvider: LanguageClassProvider {
  override val languages: List<LanguageClass>
    get() = listOf(GoLanguageClass.GO)
}
