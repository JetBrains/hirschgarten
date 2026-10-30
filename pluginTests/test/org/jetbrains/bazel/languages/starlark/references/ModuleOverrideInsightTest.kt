package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.application.runReadActionBlocking
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.commons.BazelRelease
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.inspection.StarlarkFunctionCallValidationInspection
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class ModuleOverrideInsightTest {
  private val myFixture by bazelCodeInsightFixture(release = BazelRelease(9, 1))

  @Test
  fun `should resolve reference to original bazel_dep`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      bazel_dep(name = "dep", version="1.2.3")
      archive<caret>_override(module_name = "dep")
      """.trimIndent(),
    )

    val resolvedText = runReadActionBlocking {
      val reference = myFixture.getReferenceAtCaretPosition().shouldNotBeNull()
      reference.resolve().shouldNotBeNull().text
    }
    resolvedText shouldBe "bazel_dep(name = \"dep\", version=\"1.2.3\")"
  }

  @Test
  fun `should not resolve if bazel_dep is missing`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      archive<caret>_override(module_name = "dep")
      """.trimIndent(),
    )

    val resolved = runReadActionBlocking {
      val reference = myFixture.getReferenceAtCaretPosition().shouldNotBeNull()
      reference.resolve()
    }
    resolved.shouldBeNull()
  }

  @Test
  fun `should highlight missing matching bazel_dep()`() {
    myFixture.enableInspections(StarlarkFunctionCallValidationInspection())
    val errorMsg = StarlarkBundle.message("inspection.description.call.override.missing.dep")
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
