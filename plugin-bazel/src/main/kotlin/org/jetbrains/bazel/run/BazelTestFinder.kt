package org.jetbrains.bazel.run

import com.intellij.execution.Location
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.testing.BazelTestLocationHintProvider

interface BazelTestFinder {
  /**
   * Look for test suite locations.
   * The `classNameOrSuites` parameter will be:
   * - a single element with `classname` value from test XML (if it exists)
   * - a list of suite names, ordered from the top level to the suite we are looking for (otherwise)
   */
  fun findTestSuite(
    classNameOrSuites: List<String>,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>>

  /**
   * Look for test case locations.
   * The `classNameOrSuites` parameter will be:
   * - a single element with `classname` value from test XML (if it exists)
   * - a list of parent suite names, ordered from the top level downwards (otherwise)
   */
  fun findTestCase(
    classNameOrSuites: List<String>,
    methodName: String,
    project: Project,
    scope: GlobalSearchScope,
  ): List<Location<PsiElement>>

  companion object {
    val EP = ExtensionPointName.create<BazelTestFinder>("org.jetbrains.bazel.bazelTestFinder")
  }

  class Locator : SMTestLocator {
    override fun getLocation(
      protocol: String,
      path: String,
      project: Project,
      scope: GlobalSearchScope,
    ): List<Location<PsiElement>?> {
      val locationHintElements = BazelTestLocationHintProvider.parseLocationHint(path)
      val finders = EP.extensionList
      return when (protocol) {
        BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL ->
          locationHintElements.run { finders.flatMap { it.findTestSuite(classNameOrSuites, project, scope) } }
        BazelTestLocationHintProvider.TEST_CASE_PROTOCOL ->
          locationHintElements.run { finders.flatMap { it.findTestCase(classNameOrSuites, methodName, project, scope) } }
        else -> emptyList()
      }
    }
  }
}
