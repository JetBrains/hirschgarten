package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkParsingTestCase
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingRecoveryTest : StarlarkParsingTestCase("recovery") {
  @Test
  fun testArgumentsWithoutComma() = doTest(true)

  @Test
  fun testArgumentsWithoutCommaBeforeNamedArgument() = doTest(true)

  @Test
  fun testAssignmentWithoutValue() = doTest(true)

  @Test
  fun testAugmentedAssignmentWithoutValue() = doTest(true)

  @Test
  fun testBinaryWithoutRightOperand() = doTest(true)

  @Test
  fun testBinaryWithoutRightOperandBeforeNextLine() = doTest(true)

  @Test
  fun testChainedAssignment() = doTest(true)

  @Test
  fun testChainedAssignmentWithoutValue() = doTest(true)

  @Test
  fun testConditionalWithoutElse() = doTest(true)

  @Test
  fun testDictWithoutColon() = doTest(true)

  @Test
  fun testDictWithoutCommaBetweenEntries() = doTest(true)

  @Test
  fun testDictWithoutValue() = doTest(true)

  @Test
  fun testForWithoutIn() = doTest(true)

  @Test
  fun testForWithoutTarget() = doTest(true)

  @Test
  fun testFunctionWithoutBody() = doTest(true)

  @Test
  fun testFunctionWithoutClosingParenthesis() = doTest(true)

  @Test
  fun testFunctionWithoutIndent() = doTest(true)

  @Test
  fun testFunctionWithoutName() = doTest(true)

  @Test
  fun testFunctionWithoutParameters() = doTest(true)

  @Test
  fun testIfWithoutColon() = doTest(true)

  @Test
  fun testIfWithoutCondition() = doTest(true)

  @Test
  fun testLambdaWithoutColon() = doTest(true)

  @Test
  fun testListWithoutComma() = doTest(true)

  @Test
  fun testListWithoutCommaBetweenStrings() = doTest(true)

  @Test
  fun testLoadValueNotString() = doTest(true)

  @Test
  fun testLoadWithoutComma() = doTest(true)

  @Test
  fun testLoadWithoutSymbols() = doTest(true)

  @Test
  fun testNamedArgumentAtEndOfFile() = doTest(true)

  @Test
  fun testNamedArgumentWithoutValue() = doTest(true)

  @Test
  fun testNotWithoutOperand() = doTest(true)

  @Test
  fun testParametersWithoutComma() = doTest(true)

  @Test
  fun testPythonStatements() = doTest(true)

  @Test
  fun testReferenceWithoutName() = doTest(true)

  @Test
  fun testReservedKeywordAsName() = doTest(true)

  @Test
  fun testReturnWithUnexpectedToken() = doTest(true)

  @Test
  fun testStarArgumentWithoutValue() = doTest(true)

  @Test
  fun testUnclosedArgumentList() = doTest(true)

  @Test
  fun testUnclosedDict() = doTest(true)

  @Test
  fun testUnclosedList() = doTest(true)

  @Test
  fun testUnclosedListInCall() = doTest(true)

  @Test
  fun testUnclosedLoad() = doTest(true)

  @Test
  fun testUnclosedParameters() = doTest(true)

  @Test
  fun testUnclosedSlice() = doTest(true)

  @Test
  fun testUnclosedTuple() = doTest(true)

  @Test
  fun testUnexpectedIndent() = doTest(true)
}
