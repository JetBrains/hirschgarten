package org.jetbrains.bazel.languages.starlark.references

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModuleOverrideInsightTest : BasePlatformTestCase() {
  @Test
  fun `should resolve reference to original bazel_dep`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      bazel_dep(name = "dep", version="1.2.3")
      archive<caret>_override(module_name = "dep")
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    assertNotNull(reference)
    val resolved = reference!!.resolve()
    assertNotNull(resolved)
    assertEquals("bazel_dep(name = \"dep\", version=\"1.2.3\")", resolved!!.text)
  }

  @Test
  fun `should not resolve if bazel_dep is missing`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      archive<caret>_override(module_name = "dep")
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    assertNotNull(reference)
    val resolved = reference!!.resolve()
    assertNull(resolved)
  }

  @Test
  fun `should highlight missing matching bazel_dep()`() {
    val errorMsg = StarlarkBundle.message("annotator.override.missing.dep")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      bazel_dep(name = "dep", version="1.2.3")
      <error descr="$errorMsg">archive_override</error>(module_name = "dep_wrong")
      """.trimIndent(),
    )

    myFixture.checkHighlighting()
  }
}
