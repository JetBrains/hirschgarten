package org.jetbrains.bazel.languages.starlark.globbing

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import org.junit.jupiter.api.Test

internal class StarlarkGlobMatchingTest {

  @Test
  fun `should match star wildcard`() {
    StarlarkGlob.partMatches("*", "foo").shouldBeTrue()
    StarlarkGlob.partMatches("*", "foo.bar").shouldBeTrue()

    StarlarkGlob.partMatches("*", "").shouldBeFalse()
  }

  @Test
  fun `should match question wildcard`() {
    StarlarkGlob.partMatches("f?o", "foo").shouldBeTrue()
    StarlarkGlob.partMatches("?oo", "foo").shouldBeTrue()

    StarlarkGlob.partMatches("f?o", "fo").shouldBeFalse()
    StarlarkGlob.partMatches("f?o", "fxxo").shouldBeFalse()
  }

  @Test
  fun `should match suffix wildcard`() {
    StarlarkGlob.partMatches("*.java", "Foo.java").shouldBeTrue()
    StarlarkGlob.partMatches("*.java", "Foo.java").shouldBeTrue()

    StarlarkGlob.partMatches("*.java", "Foo.kt").shouldBeFalse()
  }

  @Test
  fun `should match prefix wildcard`() {
    StarlarkGlob.partMatches("Foo*", "Foo").shouldBeTrue()
    StarlarkGlob.partMatches("Foo*", "FooBar").shouldBeTrue()

    StarlarkGlob.partMatches("Foo*", "BarFoo").shouldBeFalse()
  }

  @Test
  fun `should match double star special case`() {
    StarlarkGlob.partMatches("**", "anything").shouldBeTrue()

    StarlarkGlob.partMatches("**", "").shouldBeFalse()
  }
}
