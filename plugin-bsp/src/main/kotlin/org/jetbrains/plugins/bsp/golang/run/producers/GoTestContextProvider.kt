/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.bsp.golang.run.producers

import com.google.idea.blaze.base.command.BlazeFlags

internal class GoTestContextProvider : TestContextProvider {
  public override fun getTestContext(context: com.intellij.execution.actions.ConfigurationContext): RunConfigurationContext? {
    val element: com.intellij.psi.PsiElement? = context.getPsiLocation()
    if (element == null) {
      return null
    }
    val file: com.intellij.psi.PsiFile? = element.getContainingFile()
    if (file !is GoFile || !GoTestFinder.isTestFile(file)) {
      return null
    }
    val target: com.google.common.util.concurrent.ListenableFuture<TargetInfo?>? =
      TestTargetHeuristic.targetFutureForPsiElement(element,  /* testSize= */null)
    if (target == null) {
      return null
    }
    val function: GoFunctionOrMethodDeclaration? = GoTestFinder.findTestFunctionInContext(element)
    if (function == null) {
      return TestContext.builder( /* sourceElement= */file, ExecutorType.DEBUG_SUPPORTED_TYPES)
        .addTestEnv(com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.GO_TEST_WRAP_TESTV)
        .setTarget(target)
        .setDescription(file.getName())
        .build()
    }

    val testFilterRegex: String =
      com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.regexifyTestFilter(
          com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.calculateRawTestFilterForElement(
              element,
              function,
          ),
      )
    val builder: TestContext.Builder = TestContext.builder(
        /* sourceElement= */function,
        ExecutorType.DEBUG_SUPPORTED_TYPES,
    )
      .addTestEnv(com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.GO_TEST_WRAP_TESTV)
      .setTarget(target)
      .setDescription(String.format("%s#%s", file.getName(), function.getName()))
    if (function is GoMethodDeclaration &&
      GoTestifySupport.getTestifySuiteTypeSpec(function) != null
    ) {
      builder.addBlazeFlagsModification(
          TestContext.BlazeFlagsModification.addFlagIfNotPresent(
              BlazeFlags.TEST_ARG + "-testify.m=" + testFilterRegex,
          ),
      )
    } else {
      builder.setTestFilter(testFilterRegex)
    }
    return builder.build()
  }

  companion object {
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
    private fun calculateRawTestFilterForElement(
      element: com.intellij.psi.PsiElement,
      enclosingFunction: GoFunctionOrMethodDeclaration
    ): String? {
      if (element.getParent().isEquivalentTo(enclosingFunction)) {
        return enclosingFunction.getName()
      } else {
        return GoTestRunConfigurationProducerBase.findSubTestInContext(element, enclosingFunction)
      }
    }

    @com.google.common.annotations.VisibleForTesting
    fun regexifyTestFilter(testFilter: String): String {
      return "^" + com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.escapeRegexChars(
          testFilter,
      ) + "$"
    }

    private fun escapeRegexChars(name: String): String {
      val output: java.lang.StringBuilder = java.lang.StringBuilder()
      for (c in name.toCharArray()) {
        if (com.google.idea.blaze.golang.run.producers.GoTestContextProvider.Companion.isRegexCharNeedingEscaping(
                c,
            )
        ) {
          output.append("\\")
        }
        output.append(c)
      }
      return output.toString()
    }

    private fun isRegexCharNeedingEscaping(c: Char): Boolean {
      // Taken from https://cs.opensource.google/go/go/+/refs/tags/go1.21.4:src/regexp/regexp.go;l=720
      return c == '\\' || c == '.' || c == '+' || c == '*' || c == '?' || c == '(' || c == ')' || c == '|' || c == '[' || c == ']' || c == '{' || c == '}' || c == '^' || c == '$'
    }


    const val GO_TEST_WRAP_TESTV: String = "GO_TEST_WRAP_TESTV=1"
  }
}
