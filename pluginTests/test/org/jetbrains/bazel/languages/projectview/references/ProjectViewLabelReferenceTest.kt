package org.jetbrains.bazel.languages.projectview.references

import com.intellij.openapi.application.runReadActionBlocking
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class ProjectViewLabelReferenceTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `targets should return a reference and resolve`() {
    myFixture.addFileToProject(
      "BUILD",
      """
      java_binary(name = "some_target")
      """.trimIndent(),
    )
    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //:some_<caret>target
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    reference.shouldBeInstanceOf<ProjectViewLabelReference>()
    runReadActionBlocking { reference.resolve() }.shouldNotBeNull()
  }

  @Test
  fun `should not resolve non-existing targets`() {
    myFixture.configureByText(
      ".bazelproject",
      """
      targets:
        //:some_<caret>target
      """.trimIndent(),
    )

    val reference = myFixture.getReferenceAtCaretPosition()
    reference.shouldBeInstanceOf<ProjectViewLabelReference>()
    runReadActionBlocking { reference.resolve() }.shouldBeNull()
  }
}
