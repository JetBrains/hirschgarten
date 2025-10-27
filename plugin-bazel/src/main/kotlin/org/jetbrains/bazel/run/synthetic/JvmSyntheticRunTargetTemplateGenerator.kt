package org.jetbrains.bazel.run.synthetic

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class JvmSyntheticRunTargetTemplateGenerator : MainClassSyntheticRunTargetTemplateGenerator() {
  override val id: String = "jvm"

  override fun getMainClass(element: PsiElement): String? {
    val psiClass = element.getNonStrictParentOfType<PsiClass>() ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  override fun getBuildContent(target: BuildTarget, syntheticTarget: String, mainClass: String): String {
    @Language("starlark") val build = """
      load("@rules_java//java:defs.bzl", "java_binary")

      java_binary(
        name = "$syntheticTarget",
        main_class = "${mainClass}",
        runtime_deps = ["${target.id}"],
      )
    """.trimIndent()
    return build
  }

}
