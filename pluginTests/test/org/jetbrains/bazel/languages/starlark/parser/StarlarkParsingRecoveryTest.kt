package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.projectFixture
import org.jetbrains.bazel.languages.starlark.fixtures.starlarkParsingFixture
import org.junit.jupiter.api.Test

@TestApplication
class StarlarkParsingRecoveryTest {
  companion object {
    private val project = projectFixture()
  }

  private val parsing by starlarkParsingFixture(project, "recovery")

  @Test
  fun testArgumentsWithWrongCloser() = parsing.doTest(true)

  @Test
  fun testArgumentsWithoutComma() = parsing.doTest(true)

  @Test
  fun testArgumentsWithoutCommaAtLineEnd() = parsing.doTest(true)

  @Test
  fun testArgumentsWithoutCommaBeforeNamedArgument() = parsing.doTest(true)

  @Test
  fun testAssignmentWithoutValue() = parsing.doTest(true)

  @Test
  fun testAugmentedAssignmentWithoutValue() = parsing.doTest(true)

  @Test
  fun testBinaryWithoutRightOperand() = parsing.doTest(true)

  @Test
  fun testBinaryWithoutRightOperandBeforeNextLine() = parsing.doTest(true)

  @Test
  fun testChainedAssignment() = parsing.doTest(true)

  @Test
  fun testChainedAssignmentWithoutValue() = parsing.doTest(true)

  @Test
  fun testColonsAndSemicolonsAfterErrors() = parsing.doTest(true)

  @Test
  fun testCompoundKeywordInsideLine() = parsing.doTest(true)

  @Test
  fun testConditionalWithoutElse() = parsing.doTest(true)

  @Test
  fun testDictWithoutColon() = parsing.doTest(true)

  @Test
  fun testDictWithoutCommaBetweenEntries() = parsing.doTest(true)

  @Test
  fun testDictWithoutValue() = parsing.doTest(true)

  @Test
  fun testExtraClosingBracket() = parsing.doTest(true)

  @Test
  fun testForWithoutIn() = parsing.doTest(true)

  @Test
  fun testForWithoutTarget() = parsing.doTest(true)

  @Test
  fun testFunctionWithoutBody() = parsing.doTest(true)

  @Test
  fun testFunctionWithoutClosingParenthesis() = parsing.doTest(true)

  @Test
  fun testFunctionWithoutIndent() = parsing.doTest(true)

  @Test
  fun testFunctionWithoutName() = parsing.doTest(true)

  @Test
  fun testFunctionWithoutParameters() = parsing.doTest(true)

  @Test
  fun testHeaderColonAfterBrokenBracketContent() = parsing.doTest(true)

  @Test
  fun testIfWithoutColon() = parsing.doTest(true)

  @Test
  fun testIfWithoutCondition() = parsing.doTest(true)

  @Test
  fun testInconsistentDedentWithSameIndentLines() = parsing.doTest(true)

  @Test
  fun testInlineSuiteWithCompoundStatement() = parsing.doTest(true)

  @Test
  fun testLambdaWithoutColon() = parsing.doTest(true)

  @Test
  fun testListClosedByParenthesis() = parsing.doTest(true)

  @Test
  fun testListWithoutComma() = parsing.doTest(true)

  @Test
  fun testListWithoutCommaBetweenStrings() = parsing.doTest(true)

  @Test
  fun testLoadValueBytes() = parsing.doTest(true)

  @Test
  fun testLoadValueNotString() = parsing.doTest(true)

  @Test
  fun testLoadWithoutComma() = parsing.doTest(true)

  @Test
  fun testLoadWithoutSymbols() = parsing.doTest(true)

  @Test
  fun testNamedArgumentAtEndOfFile() = parsing.doTest(true)

  @Test
  fun testNamedArgumentWithoutValue() = parsing.doTest(true)

  @Test
  fun testNotWithoutOperand() = parsing.doTest(true)

  @Test
  fun testParametersWithBracketedJunk() = parsing.doTest(true)

  @Test
  fun testParametersWithoutComma() = parsing.doTest(true)

  @Test
  fun testPythonStatements() = parsing.doTest(true)

  @Test
  fun testReferenceWithoutName() = parsing.doTest(true)

  @Test
  fun testReservedKeywordAsName() = parsing.doTest(true)

  @Test
  fun testReturnWithUnexpectedToken() = parsing.doTest(true)

  @Test
  fun testStarArgumentWithoutValue() = parsing.doTest(true)

  @Test
  fun testStrayKeywordBeforeUnclosedCall() = parsing.doTest(true)

  @Test
  fun testStrayTokenBeforeStatement() = parsing.doTest(true)

  @Test
  fun testTupleWithEmptyElement() = parsing.doTest(true)

  @Test
  fun testUnclosedArgumentList() = parsing.doTest(true)

  @Test
  fun testUnclosedBracketInHeaderBeforeBody() = parsing.doTest(true)

  @Test
  fun testUnclosedCallBeforeComment() = parsing.doTest(true)

  @Test
  fun testUnclosedCallBeforeDefAndStrayCloser() = parsing.doTest(true)

  @Test
  fun testUnclosedCallBeforeReturn() = parsing.doTest(true)

  @Test
  fun testUnclosedCallBeforeStatements() = parsing.doTest(true)

  @Test
  fun testUnclosedCallBeforeStrayCloser() = parsing.doTest(true)

  @Test
  fun testUnclosedCallInIfHeader() = parsing.doTest(true)

  @Test
  fun testUnclosedCallWithTrailingComma() = parsing.doTest(true)

  @Test
  fun testUnclosedDict() = parsing.doTest(true)

  @Test
  fun testUnclosedDictBeforeIf() = parsing.doTest(true)

  @Test
  fun testUnclosedList() = parsing.doTest(true)

  @Test
  fun testUnclosedListBeforeAssignment() = parsing.doTest(true)

  @Test
  fun testUnclosedListBeforeIf() = parsing.doTest(true)

  @Test
  fun testUnclosedListInCall() = parsing.doTest(true)

  @Test
  fun testUnclosedListInForHeader() = parsing.doTest(true)

  @Test
  fun testUnclosedLoad() = parsing.doTest(true)

  @Test
  fun testUnclosedParameters() = parsing.doTest(true)

  @Test
  fun testUnclosedParametersInDefHeader() = parsing.doTest(true)

  @Test
  fun testUnclosedParametersWithoutCommaBeforeColon() = parsing.doTest(true)

  @Test
  fun testUnclosedSlice() = parsing.doTest(true)

  @Test
  fun testUnclosedTuple() = parsing.doTest(true)

  @Test
  fun testUnexpectedIndent() = parsing.doTest(true)

  @Test
  fun testUnexpectedIndentBlock() = parsing.doTest(true)

  @Test
  fun testUnexpectedTokenAtStatementStart() = parsing.doTest(true)
}
