package org.jetbrains.bazel.jvm.run

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class JavaTestFilterProviderTest {
  private val provider = JavaTestFilterProvider()

  @Test
  fun `builds a filter for a single test method`() {
    provider.testFilterFromLocationUrl("java:test://com.example.MyTest/foo") shouldBe "MyTest.foo$"
  }

  @Test
  fun `anchors the method name so a prefix sibling is not also matched`() {
    // `foo` is a prefix of `fooBar`; the trailing '$' is what keeps `--test_filter` for `foo`
    // from also running `fooBar`, since bazel matches the filter as a regex.
    provider.testFilterFromLocationUrl("java:test://com.example.MyTest/foo") shouldBe "MyTest.foo$"
    provider.testFilterFromLocationUrl("java:test://com.example.MyTest/fooBar") shouldBe "MyTest.fooBar$"
  }

  @Test
  fun `uses the simple class name for a nested test method`() {
    provider.testFilterFromLocationUrl("java:test://com.example.Outer\$Inner/foo") shouldBe "Inner.foo$"
  }

  @Test
  fun `builds a filter for a whole suite`() {
    provider.testFilterFromLocationUrl("java:suite://com.example.MyTest") shouldBe "com.example.MyTest"
  }

  @Test
  fun `keeps the nested-class separator for a suite`() {
    provider.testFilterFromLocationUrl("java:suite://com.example.Outer\$Inner") shouldBe "com.example.Outer\$Inner"
  }

  @Test
  fun `does not handle a non-java location url`() {
    provider.testFilterFromLocationUrl("go:test://pkg/TestFoo").shouldBeNull()
  }
}
