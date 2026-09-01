package org.jetbrains.bazel.commons

import com.intellij.testFramework.junit5.TestApplication
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldNotStartWith
import io.kotest.matchers.string.shouldStartWith
import org.jetbrains.bazel.sync.workspace.targetKind.TargetKindService
import org.jetbrains.bazel.testing.BazelTestDetails
import org.jetbrains.bazel.util.BazelTestLocationHintProvider
import org.junit.jupiter.api.Test

@TestApplication
class BazelTestLocationHintProviderTest {
  private val javaTargetKind = TargetKindService.getInstance().findPredefinedRule("java_test")

  @Test
  fun `generate a hint by file location`() {
    val testDetails1 =
      BazelTestDetails
        .testCase(testName)
        .withClassname(normalClassName)
        .withParentSuites(javaSuites)
        .withFileAndZeroBasedLine("src/java/package/file.java", 17)
        .build()
    val testDetails2 =
      BazelTestDetails
        .testCase(testName)
        .withClassname(normalClassName)
        .withParentSuites(javaSuites)
        .withFileAndZeroBasedLine("src/java/package/file.java", null)
        .build()
    val testDetails3 =
      BazelTestDetails
        .testCase(testName)
        .withClassname(normalClassName)
        .withParentSuites(javaSuites)
        .withFileAndZeroBasedLine("src\\java\\package\\file.java", 17)
        .build()
    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)
    val hint2 = BazelTestLocationHintProvider.getLocationHint(testDetails2, javaTargetKind)
    val hint3 = BazelTestLocationHintProvider.getLocationHint(testDetails3, javaTargetKind)

    hint1 shouldBe "file://src/java/package/file.java:18" // line number conversion from zero-based to one-based
    hint2.shouldNotStartWith("file://") // not enough information
    hint3 shouldBe "file://src/java/package/file.java:18"
  }

  @Test
  fun `generate a java suite hint with classname`() {
    val testDetails1 =
      BazelTestDetails.testSuite(suiteName).withClassname(normalClassName).withParentSuites(javaSuites).build()
    val testDetails2 =
      BazelTestDetails.testSuite(suiteName).withClassname(nestedClassName).withParentSuites(javaSuites).build()

    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)
    val hint2 = BazelTestLocationHintProvider.getLocationHint(testDetails2, javaTargetKind)

    hint1.shouldStartWith("java:suite://")
    hint2.shouldStartWith("java:suite://")

    val parsedHint1 = parseJavaLocationHint(hint1)
    val parsedHint2 = parseJavaLocationHint(hint2)

    parsedHint1.classNameOrSuites.singleOrNull() shouldBe normalClassName
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.joinToString("$") shouldBe nestedClassName
    parsedHint2.methodName.shouldBeEmpty()
  }

  @Test
  fun `generate a java suite hint with unknown classname`() {
    val testDetails1 =
      BazelTestDetails.testSuite(suiteName).withParentSuites(javaSuites).build()

    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)

    hint1.shouldStartWith("java:suite://")

    val parsedHint1 = parseJavaLocationHint(hint1)

    parsedHint1.classNameOrSuites.shouldContainExactly(javaSuites + suiteName)
    parsedHint1.methodName.shouldBeEmpty()
  }

  @Test
  fun `generate a java test case hint`() {
    val testDetails1 =
      BazelTestDetails.testCase(testName).withClassname(nestedClassName).withParentSuites(javaSuites).build()
    val testDetails2 =
      BazelTestDetails.testCase(testName).withParentSuites(javaSuites).build()

    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)
    val hint2 = BazelTestLocationHintProvider.getLocationHint(testDetails2, javaTargetKind)

    hint1.shouldStartWith("java:test://")
    hint2.shouldStartWith("java:test://")

    val parsedHint1 = parseJavaLocationHint(hint1)
    val parsedHint2 = parseJavaLocationHint(hint2)

    parsedHint1.classNameOrSuites.joinToString("$") shouldBe nestedClassName
    parsedHint1.methodName shouldBe testName
    parsedHint2.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint2.methodName shouldBe testName
  }

  @Test
  fun `generate java hints with only names`() {
    val testDetails1 =
      BazelTestDetails.testSuite(suiteName).build()
    val testDetails2 =
      BazelTestDetails.testCase(testName).build()

    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)
    val hint2 = BazelTestLocationHintProvider.getLocationHint(testDetails2, javaTargetKind)

    hint1.shouldStartWith("java:suite://")
    hint2.shouldStartWith("java:test://")

    val parsedHint1 = parseJavaLocationHint(hint1)
    val parsedHint2 = parseJavaLocationHint(hint2)

    parsedHint1.classNameOrSuites.singleOrNull() shouldBe suiteName
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.shouldBeEmpty()
    parsedHint2.methodName shouldBe testName
  }

  @Test
  fun `generate java hints without names`() {
    val testDetails1 =
      BazelTestDetails.testSuite("").withParentSuites(javaSuites).build()
    val testDetails2 =
      BazelTestDetails.testCase("").withParentSuites(javaSuites).build()

    val hint1 = BazelTestLocationHintProvider.getLocationHint(testDetails1, javaTargetKind)
    val hint2 = BazelTestLocationHintProvider.getLocationHint(testDetails2, javaTargetKind)

    hint1.shouldStartWith("java:suite://")
    hint2.shouldStartWith("java:test://")

    val parsedHint1 = parseJavaLocationHint(hint1)
    val parsedHint2 = parseJavaLocationHint(hint2)

    parsedHint1.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint1.methodName.shouldBeEmpty()
    parsedHint2.classNameOrSuites.shouldContainExactly(javaSuites)
    parsedHint2.methodName.shouldBeEmpty()
  }
}

private const val suiteName = "Suite123"
private const val testName = "Test123"

private const val normalClassName = "org.jetbrains.bazel.SomeClass"
private const val nestedClassName = $$"org.jetbrains.bazel.SomeClass$NestedClass$NestedNested"

private val javaSuites =
  listOf(
    "SomeClass",
    "NestedClass",
    "NestedNested",
  )

private fun parseJavaLocationHint(locationHint: String?): JavaLocationHintData {
  locationHint.shouldNotBeNull()
  val fragments = locationHint.substringAfter("://").split("/", limit = 2)
  val classNames =
    fragments.getOrNull(0)?.split("$")?.filter { it.isNotBlank() } ?: emptyList()
  val methodName = fragments.getOrNull(1) ?: ""
  return JavaLocationHintData(classNames, methodName)
}

private data class JavaLocationHintData(val classNameOrSuites: List<String>, val methodName: String)
