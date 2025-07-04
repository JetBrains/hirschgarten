package org.jetbrains.bazel.golang.ui.gutters

import com.goide.GoConstants
import com.goide.execution.GoRunUtil
import com.goide.execution.testing.GoTestFinder
import com.goide.execution.testing.GoTestRunConfigurationProducerBase
import com.goide.psi.GoFunctionDeclaration
import com.goide.psi.GoFunctionOrMethodDeclaration
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.ui.gutters.BazelRunLineMarkerContributor

class BazelGoRunLineMarkerContributor : BazelRunLineMarkerContributor() {
  override fun PsiElement.shouldAddMarker(): Boolean = isMainFunction() || GoTestFinder.isTestFile(this.containingFile)

  private fun PsiElement.isMainFunction(): Boolean =
    GoRunUtil.isMainGoFile(this.containingFile) &&
      GoConstants.MAIN == (this.parent as GoFunctionDeclaration).name

  override fun getSingleTestFilter(element: PsiElement): String {
    val function = GoTestFinder.findTestFunctionInContext(element)
    return if (function != null) {
      val rawTestFilter = calculateRawTestFilterForElement(element, function)
      return wrapText(rawTestFilter ?: element.text)
    } else {
      wrapText(element.text)
    }
  }

  private fun wrapText(text: String): String = "^${escapeRegexChars(text)}$"

  /**
   * Given a code element, calculate the test filter we'd need to run exactly that element.
   * It takes into account nested tests.
   * Here is an example of the expected results of clicking on each section:
   * ```
   * func Test(t *testing.T) {                    // returns "Test"
   * t.Run("with_nested", func(t *testing.T) {  // returns "Test/with_nested"
   * t.Run("subtest", func(t *testing.T) {}) // returns "Test/with_nested/subtest"
   * })
   * }
   * ```
   * When a user clicks in the ">" button, we get pointed to the "Run" part of "t.Run".
   * We need to walk the tree up to see the function call that has the actual argument.
   *
   * This function doesn't worry about turning this filter into a regex which can be passed to --test_filter.
   * @see .regexifyTestFilter for converting this to a --test_filter value.
   *
   *
   * @param element: Element the user has clicked on.
   * @param enclosingFunction: Go function encompassing this test.
   * @return String representation of the proper test filter.
   */
  private fun calculateRawTestFilterForElement(element: PsiElement, enclosingFunction: GoFunctionOrMethodDeclaration): String? =
    if (element.parent.isEquivalentTo(enclosingFunction)) {
      enclosingFunction.name
    } else {
      GoTestRunConfigurationProducerBase.findSubTestInContext(element, enclosingFunction)
    }

  private fun escapeRegexChars(name: String): String {
    val output = StringBuilder()
    for (c in name.toCharArray()) {
      if (isRegexCharNeedingEscaping(c)) {
        output.append("\\")
      }
      output.append(c)
    }
    return output.toString()
  }

  private fun isRegexCharNeedingEscaping(c: Char): Boolean {
    // Taken from https://cs.opensource.google/go/go/+/refs/tags/go1.21.4:src/regexp/regexp.go;l=720
    return c == '\\' ||
      c == '.' ||
      c == '+' ||
      c == '*' ||
      c == '?' ||
      c == '(' ||
      c == ')' ||
      c == '|' ||
      c == '[' ||
      c == ']' ||
      c == '{' ||
      c == '}' ||
      c == '^' ||
      c == '$'
  }
}
