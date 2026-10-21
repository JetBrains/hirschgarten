package org.jetbrains.bazel.clion.run

import com.intellij.psi.PsiElement
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTarget

internal class BazelCcTestRunConfigurationProducer : BazelRunConfigurationProducer() {
  override fun getGutterAction(
    element: PsiElement,
    target: BuildTarget,
  ): GutterAction? {
    val (testElement, framework) = BazelCcTestFrameworkSupport.findTestAndFramework(element) ?: return null
    val testFilter = framework.createTestFilter(listOf(testElement)) ?: return null
    return GutterAction(testFilter = testFilter, additionalLocationString = testElement.testName)
  }
}
