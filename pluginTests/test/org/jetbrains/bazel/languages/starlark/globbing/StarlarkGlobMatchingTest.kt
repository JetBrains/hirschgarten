package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StarlarkGlobMatchingTest : BasePlatformTestCase() {

  @Test
  fun `should match star wildcard`() {
    assertTrue(StarlarkGlob.partMatches("*", "foo"))
    assertTrue(StarlarkGlob.partMatches("*", "foo.bar"))

    assertFalse(StarlarkGlob.partMatches("*", ""))
  }

  @Test
  fun `should match question wildcard`() {
    assertTrue(StarlarkGlob.partMatches("f?o", "foo"))
    assertTrue(StarlarkGlob.partMatches("?oo", "foo"))

    assertFalse(StarlarkGlob.partMatches("f?o", "fo"))
    assertFalse(StarlarkGlob.partMatches("f?o", "fxxo"))
  }

  @Test
  fun `should match suffix wildcard`() {
    assertTrue(StarlarkGlob.partMatches("*.java", "Foo.java"))
    assertTrue(StarlarkGlob.partMatches("*.java", "Foo.java"))

    assertFalse(StarlarkGlob.partMatches("*.java", "Foo.kt"))
  }

  @Test
  fun `should match prefix wildcard`() {
    assertTrue(StarlarkGlob.partMatches("Foo*", "Foo"))
    assertTrue(StarlarkGlob.partMatches("Foo*", "FooBar"))

    assertFalse(StarlarkGlob.partMatches("Foo*", "BarFoo"))
  }

  @Test
  fun `should match double star special case`() {
    assertTrue(StarlarkGlob.partMatches("**", "anything"))

    assertFalse(StarlarkGlob.partMatches("**", ""))
  }
}
