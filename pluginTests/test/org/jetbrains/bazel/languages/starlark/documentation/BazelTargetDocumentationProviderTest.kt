package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.lang.LanguageDocumentation
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelTargetDocumentationProviderTest : BasePlatformTestCase() {
  @Before
  fun setupBuildEnvironment() {
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  @Test
  fun `should return quick navigate info for target name`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )

    val element = myFixture.requireCallWithNameAttribute("lib")
    val quickNavInfo = getQuickNavigateInfo(element)

    quickNavInfo shouldBe "'java_library' target"
  }

  @Test
  fun `should not return quick navigate info for non-target string`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )

    val srcsStringLiteral = myFixture.file.descendantsOfType<StarlarkStringLiteralExpression>()
      .first { it.getStringContents() == "Lib.java" }

    val quickNavInfo = getQuickNavigateInfo(srcsStringLiteral)
    quickNavInfo.shouldBeNull()
  }

  private fun getQuickNavigateInfo(element: PsiElement): String? =
    LanguageDocumentation.INSTANCE.allForLanguage(StarlarkLanguage)
      .firstNotNullOfOrNull { it.getQuickNavigateInfo(element, element) }
}
