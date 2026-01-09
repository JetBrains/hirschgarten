package org.jetbrains.bazel.languages.starlark.globbing

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class StarlarkGlobMatchingTest : BasePlatformTestCase() {

  @Test
  fun `should match star wildcard`() {
    assertTrue(StarlarkGlob.matches("*", "foo"))
    assertTrue(StarlarkGlob.matches("*", "foo.bar"))

    assertFalse(StarlarkGlob.matches("*", ""))
  }

  @Test
  fun `should match question wildcard`() {
    assertTrue(StarlarkGlob.matches("f?o", "foo"))
    assertTrue(StarlarkGlob.matches("?oo", "foo"))

    assertFalse(StarlarkGlob.matches("f?o", "fo"))
    assertFalse(StarlarkGlob.matches("f?o", "fxxo"))
  }

  @Test
  fun `should match suffix wildcard`() {
    assertTrue(StarlarkGlob.matches("*.java", "Foo.java"))
    assertTrue(StarlarkGlob.matches("*.java", "Foo.java"))

    assertFalse(StarlarkGlob.matches("*.java", "Foo.kt"))
  }

  @Test
  fun `should match prefix wildcard`() {
    assertTrue(StarlarkGlob.matches("Foo*", "Foo"))
    assertTrue(StarlarkGlob.matches("Foo*", "FooBar"))

    assertFalse(StarlarkGlob.matches("Foo*", "BarFoo"))
  }

  @Test
  fun `should match double star special case`() {
    assertTrue(StarlarkGlob.matches("**", "anything"))

    assertFalse(StarlarkGlob.matches("**", ""))
  }
}
