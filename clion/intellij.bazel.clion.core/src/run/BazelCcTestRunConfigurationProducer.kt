package org.jetbrains.bazel.clion.run

import com.intellij.execution.PsiLocation
import com.jetbrains.cidr.execution.testing.CidrTestLocation
import org.jetbrains.bazel.ui.gutters.BazelRunConfigurationProducer
import org.jetbrains.bsp.protocol.BuildTargetInfo

internal class BazelCcTestRunConfigurationProducer : BazelRunConfigurationProducer() {
  override fun getGutterAction(
    location: PsiLocation<*>,
    target: BuildTargetInfo,
  ): GutterAction? {
    val (testElement, framework) = BazelCcTestFrameworkSupport.findTestAndFramework(location.psiElement) ?: return null
    // A node of the test tree knows the instantiation and the parameter of a test, but its PSI element does not
    val treeTestElement = (location as? CidrTestLocation<*>)?.testScopeElement
    val testFilter = framework.createTestFilter(listOf(treeTestElement ?: testElement)) ?: return null
    return GutterAction(testFilter = testFilter, additionalLocationString = treeTestElement?.configurationName ?: testElement.testName)
  }
}
