package org.jetbrains.bazel.java.run

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.jetbrains.bazel.run.synthetic.MainClassSyntheticRunTargetTemplateGenerator
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class JavaSyntheticRunTargetTemplateGenerator : MainClassSyntheticRunTargetTemplateGenerator() {
  override fun isSupported(target: BuildTarget): Boolean = target.data is JvmBuildTarget

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
