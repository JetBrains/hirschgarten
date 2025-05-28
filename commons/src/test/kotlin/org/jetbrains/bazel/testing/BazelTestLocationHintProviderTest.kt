package org.jetbrains.bazel.testing

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

class BazelTestLocationHintProviderTest {
  @Test
  fun `generate and parse a classname suite hint`() {
    val hint1 = BazelTestLocationHintProvider.testSuiteLocationHint(suiteName, normalClassName, javaSuites)
    val hint2 = BazelTestLocationHintProvider.testSuiteLocationHint(suiteName, nestedClassName, javaSuites)

    hint1.shouldStartWith(BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL)
    hint2.shouldStartWith(BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL)

    val parsedHint1 = BazelTestLocationHintProvider.parseLocationHint(hint1)
    val parsedHint2 = BazelTestLocationHintProvider.parseLocationHint(hint2)

    parsedHint1.classNameOrSuites.singleOrNull() shouldBe normalClassName
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.singleOrNull() shouldBe nestedClassName
    parsedHint2.methodName.shouldBeEmpty()
  }

  @Test
  fun `generate and parse a suite hint with unknown classname`() {
    val hint1 = BazelTestLocationHintProvider.testSuiteLocationHint(suiteName, parentSuites = javaSuites)
    val hint2 = BazelTestLocationHintProvider.testSuiteLocationHint(suiteName, parentSuites = unknownBazelSuite)

    hint1.shouldStartWith(BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL)
    hint2.shouldStartWith(BazelTestLocationHintProvider.TEST_SUITE_PROTOCOL)

    val parsedHint1 = BazelTestLocationHintProvider.parseLocationHint(hint1)
    val parsedHint2 = BazelTestLocationHintProvider.parseLocationHint(hint2)

    parsedHint1.classNameOrSuites.shouldContainExactly(javaSuites + suiteName)
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.shouldContainExactly(unknownBazelSuite + suiteName)
    parsedHint2.methodName.shouldBeEmpty()
  }

  @Test
  fun `generate and parse a test case hint`() {
    val hint1 = BazelTestLocationHintProvider.testCaseLocationHint(testName, nestedClassName, javaSuites)
    val hint2 = BazelTestLocationHintProvider.testCaseLocationHint(testName, parentSuites = javaSuites)

    hint1.shouldStartWith(BazelTestLocationHintProvider.TEST_CASE_PROTOCOL)
    hint2.shouldStartWith(BazelTestLocationHintProvider.TEST_CASE_PROTOCOL)

    val parsedHint1 = BazelTestLocationHintProvider.parseLocationHint(hint1)
    val parsedHint2 = BazelTestLocationHintProvider.parseLocationHint(hint2)

    parsedHint1.classNameOrSuites.singleOrNull() shouldBe nestedClassName
    parsedHint1.methodName shouldBe testName
    parsedHint2.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint2.methodName shouldBe testName
  }

  @Test
  fun `generate and parse hints with only names`() {
    val hint1 = BazelTestLocationHintProvider.testSuiteLocationHint(suiteName)
    val hint2 = BazelTestLocationHintProvider.testCaseLocationHint(testName)

    val parsedHint1 = BazelTestLocationHintProvider.parseLocationHint(hint1)
    val parsedHint2 = BazelTestLocationHintProvider.parseLocationHint(hint2)

    parsedHint1.classNameOrSuites.singleOrNull() shouldBe suiteName
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.shouldBeEmpty()
    parsedHint2.methodName shouldBe testName
  }

  @Test
  fun `generate and parse hints without names`() {
    val hint1 = BazelTestLocationHintProvider.testSuiteLocationHint("", parentSuites = javaSuites)
    val hint2 = BazelTestLocationHintProvider.testCaseLocationHint("", parentSuites = javaSuites)

    val parsedHint1 = BazelTestLocationHintProvider.parseLocationHint(hint1)
    val parsedHint2 = BazelTestLocationHintProvider.parseLocationHint(hint2)

    parsedHint1.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint2.methodName.shouldBeEmpty()
  }
}

private const val suiteName = "Suite123"
private const val testName = "Test123"

private const val normalClassName = "org.jetbrains.bazel.SomeClass"
private const val nestedClassName = "org.jetbrains.bazel.SomeClass\$NestedClass\$NestedNested"

private val javaSuites = listOf(
  "SomeClass",
  "NestedClass",
  "NestedNested",
)
private val unknownBazelSuite = listOf("//src/main:some_target")
