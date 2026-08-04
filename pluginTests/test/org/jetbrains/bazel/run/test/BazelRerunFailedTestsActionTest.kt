package org.jetbrains.bazel.run.test

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BazelRerunFailedTestsActionTest {
  private fun leafTest(className: String, methodName: String) =
    SMTestProxy(methodName, false, "java:test://$className/$methodName")

  private fun classSuite(className: String, vararg tests: SMTestProxy) =
    SMTestProxy(className.substringAfterLast('.'), true, "java:suite://$className")
      .apply { tests.forEach { addChild(it) } }

  @Test
  fun `builds an alternation filter from only the failed leaf tests`() {
    // getFailedTests returns the defective parent class alongside the failed leaf tests; only the
    // leaves should reach the filter, otherwise the bare class name would re-run the whole class.
    val foo = leafTest("com.example.MyTest", "foo")
    val bar = leafTest("com.example.MyTest", "bar")
    val suite = classSuite("com.example.MyTest", foo, bar)

    failedTestsToFilter(listOf(suite, foo, bar)) shouldBe "MyTest.foo$|MyTest.bar$"
  }

  @Test
  fun `returns null when only non-leaf containers failed`() {
    val suite = classSuite("com.example.MyTest", leafTest("com.example.MyTest", "foo"))

    failedTestsToFilter(listOf(suite)).shouldBeNull()
  }
}
