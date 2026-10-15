package org.jetbrains.bazel.clion

import com.jetbrains.cidr.execution.testing.CidrTestScopeElement
import com.jetbrains.cidr.execution.testing.google.CidrGoogleTestElements
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.bazel.clion.test.google.GoogleTestSupport
import org.junit.jupiter.api.Test

class GoogleTestSupportTest {
  @Test
  fun `plain test selects exactly itself`() = doTest(
    suite = "SampleSuite",
    test = "SampleTest",
    selects = listOf("SampleSuite.SampleTest"),
    excludes = listOf("SampleSuite.OtherTest", "OtherSuite.SampleTest"),
  )

  @Test
  fun `value parameterized test selects all instantiations`() = doTest(
    suite = "ParamSuite",
    test = "ParamTest",
    selects = listOf(
      "ParamSuite.ParamTest",
      "Instantiation/ParamSuite.ParamTest/0",
    ),
  )

  @Test
  fun `typed test selects all type instantiations`() = doTest(
    suite = "TypedTestSuite",
    test = "SimpleTest",
    selects = listOf(
      "TypedTestSuite/0.SimpleTest",
      "TypedTestSuite/1.SimpleTest",
    ),
    excludes = listOf("SampleSuite.SampleTest"),
  )

  @Test
  fun `test without name selects the whole suite`() = doTest(
    suite = "SampleSuite",
    test = null,
    selects = listOf("SampleSuite.SampleTest", "SampleSuite.AnotherTest"),
    excludes = listOf("FilterSuite.FilteredTest"),
  )

  @Test
  fun `multiple tests select each of them`() {
    val filter = requireNotNull(createTestFilter(listOf(gtest("SampleSuite", "SampleTest"), gtest("FilterSuite", "FilteredTest"))))

    assertThat(matchesGoogleTestFilter(filter, "SampleSuite.SampleTest")).isTrue()
    assertThat(matchesGoogleTestFilter(filter, "FilterSuite.FilteredTest")).isTrue()
    assertThat(matchesGoogleTestFilter(filter, "SampleSuite.AnotherTest")).isFalse()
  }

  @Test
  fun `no tests select nothing`() {
    assertThat(createTestFilter(emptyList())).isNull()
  }

  private fun doTest(
    suite: String,
    test: String?,
    selects: List<String> = emptyList(),
    excludes: List<String> = emptyList(),
  ) {
    val filter = requireNotNull(createTestFilter(listOf(gtest(suite, test))))

    for (name in selects) {
      assertThat(matchesGoogleTestFilter(filter, name))
        .withFailMessage("filter '$filter' for $suite.$test should select '$name'")
        .isTrue()
    }
    for (name in excludes) {
      assertThat(matchesGoogleTestFilter(filter, name))
        .withFailMessage("filter '$filter' for $suite.$test should not select '$name'")
        .isFalse()
    }
  }

  private fun createTestFilter(tests: List<CidrTestScopeElement>): String? {
    return requireNotNull(GoogleTestSupport()).createTestFilter(tests)
  }

  private fun gtest(suite: String?, test: String?) =
    CidrGoogleTestElements.createTestScopeElementForSuiteAndTest(suite, test)
}

internal fun matchesGoogleTestFilter(filter: String, testName: String): Boolean {
  val patterns = filter.split(':').filter { it.isNotEmpty() }
  return patterns.isEmpty() || patterns.any { filterMatches(it, testName) }
}

private fun filterMatches(pattern: String, name: String): Boolean {
  val regex = buildString {
    append('^')

    for (c in pattern) {
      when (c) {
        '*' -> append(".*")
        '?' -> append('.')
        else -> append(Regex.escape(c.toString()))
      }
    }

    append('$')
  }

  return Regex(regex).matches(name)
}
