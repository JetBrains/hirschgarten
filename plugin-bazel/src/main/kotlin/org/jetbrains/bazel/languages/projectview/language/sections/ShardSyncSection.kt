package org.jetbrains.bazel.languages.projectview.language.sections

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.projectview.completion.SimpleCompletionProvider
import org.jetbrains.bazel.languages.projectview.language.ScalarSection
import org.jetbrains.bazel.languages.projectview.language.SectionKey

class ShardSyncSection : ScalarSection<Boolean>() {
  override val name = NAME

  override val default = false

  override val doc =
    "Directs the plugin to shard bazel build invocations when syncing " +
      "and compiling your project. Bazel builds for sync may be sharded even if " +
      "this is set to false, to keep the build command under the maximum command length (ARG_MAX)."

  override val completionProvider = SimpleCompletionProvider(variants)

  override fun getSectionKey(): SectionKey<Boolean> = sectionKey

  override fun fromRawValue(rawValue: String): Boolean? = rawValue.toBooleanStrictOrNull()

  override fun annotateValue(element: PsiElement, holder: AnnotationHolder) = variantsAnnotation(element, holder, variants)

  companion object {
    const val NAME = "shard_sync"
    val sectionKey = SectionKey<Boolean>(NAME)
    private val variants = listOf("true", "false")
  }
}
