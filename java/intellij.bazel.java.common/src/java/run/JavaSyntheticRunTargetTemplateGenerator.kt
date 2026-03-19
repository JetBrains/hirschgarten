package org.jetbrains.bazel.java.run

import com.intellij.lang.jvm.util.JvmClassUtil
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.synthetic.MainClassSyntheticRunTargetTemplateGenerator
import org.jetbrains.bsp.protocol.ExecutableTarget

@ApiStatus.Internal
class JavaSyntheticRunTargetTemplateGenerator : MainClassSyntheticRunTargetTemplateGenerator() {
  override fun isSupported(target: ExecutableTarget): Boolean = target.kind.isJvmTarget()

  override fun getMainClass(element: PsiElement): String? {
    val psiClass = PsiTreeUtil.getNonStrictParentOfType(element, PsiClass::class.java) ?: return null
    return JvmClassUtil.getJvmClassName(psiClass)
  }

  override fun getBuildContent(target: ExecutableTarget, syntheticTarget: String, mainClass: String): String {
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
