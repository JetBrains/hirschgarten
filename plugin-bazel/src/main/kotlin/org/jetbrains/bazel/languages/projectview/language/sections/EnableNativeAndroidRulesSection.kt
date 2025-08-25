package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class EnableNativeAndroidRulesSection : ScalarSection<Boolean>() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Enable native (non-starlarkified) Android rules"
  override val completionProvider = SimpleCompletionProvider(VARIANTS)

  override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = variantsAnnotation(element, holder, VARIANTS)

  companion object {
    const val NAME = "enable_native_android_rules"
    val KEY = SectionKey<Boolean>(NAME)
    private val VARIANTS = listOf("true", "false")
  }
}
