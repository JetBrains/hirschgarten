package org.jetbrains.bazel.python.run

import com.intellij.psi.PsiElement
import org.jetbrains.bsp.protocol.BuildTarget

internal sealed interface PythonBazelRunContext {
  val target: BuildTarget
  val sourceElement: PsiElement
  val configurationName: String

  data class Binary(
    override val target: BuildTarget,
    override val sourceElement: PsiElement,
    override val configurationName: String,
  ) : PythonBazelRunContext

  data class Test(
    override val target: BuildTarget,
    override val sourceElement: PsiElement,
    override val configurationName: String,
    val testExecutableArguments: List<String>,
  ) : PythonBazelRunContext
}
