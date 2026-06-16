package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.lang.documentation.psi.psiDocumentationTargets
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.impl.computeDocumentationBlocking
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.ColorUtil
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.requireCallWithNameAttribute
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BazelRuleCallDocumentationTargetTest : BasePlatformTestCase() {
  private val functionColor by lazy { textAttributeColor("STARLARK_FUNCTION_DECLARATION") }
  private val namedArgColor by lazy { textAttributeColor("STARLARK_NAMED_ARGUMENT") }
  private val stringColor by lazy { textAttributeColor("STARLARK_STRING") }
  private val identifierColor by lazy { textAttributeColor("STARLARK_IDENTIFIER") }

  @Before
  fun setupBuildEnvironment() {
    initializeBazelProject(project, myFixture.tempDirPath)
  }

  private fun configureBuildFileWithDepAndLib() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "dep",
      )
      java_library(
          name = "lib",
          srcs = glob(["*.java"]),
          deps = [":dep"],
      )
      """.trimIndent(),
    )
  }

  @Test
  fun `should return proper documentation HTML for target name`() {
    configureBuildFileWithDepAndLib()

    val element = myFixture.requireCallWithNameAttribute("lib")
    val html = getDocumentationTargets(element).singleOrNull()?.computeDocumentationHtml()
    html shouldBe """
      <div class="definition"><pre style="white-space: pre; overflow-x: auto">
      <span style="color:$functionColor;">java_library</span><span style="">(</span>
      <br/>    <span style="color:$namedArgColor;">name</span> <span style="color:$namedArgColor;">=</span> <span style="color:$stringColor;font-weight:bold;">&quot;lib&quot;</span><span style="">,</span>
      <br/>    <span style="color:$namedArgColor;">srcs</span> <span style="color:$namedArgColor;">=</span> <span style="color:$identifierColor;">glob</span><span style="">(</span>[<span style="color:$stringColor;font-weight:bold;">&quot;*.java&quot;</span>]<span style="">)</span><span style="">,</span>
      <br/>    <span style="color:$namedArgColor;">deps</span> <span style="color:$namedArgColor;">=</span> [<a href="psi_element://:dep"><span style="color:$stringColor;font-weight:bold;">&quot;:dep&quot;</span></a>]<span style="">,</span>
      <br/><span style="">)</span>
      </pre></div>
    """.singleLineHtml()
  }

  @Test
  fun `should return proper documentation hint for target name`() {
    configureBuildFileWithDepAndLib()

    val element = myFixture.requireCallWithNameAttribute("lib")
    val hint = getDocumentationTargets(element).singleOrNull()?.computeDocumentationHint()
    hint shouldBe """
      <pre style="white-space: pre; overflow-x: auto">
      <span style="color:$functionColor;">java_library</span><span style="">(</span>
      <br/>    <span style="color:$namedArgColor;">name</span> <span style="color:$namedArgColor;">=</span> <span style="color:$stringColor;font-weight:bold;">&quot;lib&quot;</span><span style="">,</span>
      <br/>    <span style="color:$namedArgColor;">srcs</span> <span style="color:$namedArgColor;">=</span> <span style="color:$identifierColor;">glob</span><span style="">(</span>[<span style="color:$stringColor;font-weight:bold;">&quot;*.java&quot;</span>]<span style="">)</span><span style="">,</span>
      <br/>    <span style="color:$namedArgColor;">deps</span> <span style="color:$namedArgColor;">=</span> [<a href="psi_element://:dep"><span style="color:$stringColor;font-weight:bold;">&quot;:dep&quot;</span></a>]<span style="">,</span>
      <br/><span style="">)</span>
      </pre>
    """.singleLineHtml()
  }

  @Test
  fun `should return proper presentation for target name`() {
    configureBuildFileWithDepAndLib()

    val element = myFixture.requireCallWithNameAttribute("lib")
    val presentation = getDocumentationTargets(element).singleOrNull()
      .shouldNotBeNull()
      .computePresentation()
    presentation.presentableText shouldBe "lib"
    presentation.icon shouldBe BazelPluginIcons.bazel
    presentation.locationText shouldBe "/src/BUILD"
    presentation.locationIcon shouldBe BazelPluginIcons.bazel
  }

  @Test
  fun `should return proper documentation HTML for target label`() {
    configureBuildFileWithDepAndLib()

    val element = findLabelTarget(":dep")
    val html = getDocumentationTargets(element).singleOrNull()?.computeDocumentationHtml()
    html shouldBe """
      <div class="definition"><pre style="white-space: pre; overflow-x: auto">
      <span style="color:$functionColor;">java_library</span><span style="">(</span>
      <br/>    <span style="color:$namedArgColor;">name</span> <span style="color:$namedArgColor;">=</span> <span style="color:$stringColor;font-weight:bold;">&quot;dep&quot;</span><span style="">,</span>
      <br/><span style="">)</span>
      </pre></div>
    """.singleLineHtml()
  }

  @Test
  fun `should return proper documentation hint for target label`() {
    configureBuildFileWithDepAndLib()

    val element = findLabelTarget(":dep")
    val hint = getDocumentationTargets(element).singleOrNull()?.computeDocumentationHint()
    hint shouldBe """
      <pre style="white-space: pre; overflow-x: auto">
      <span style="color:$functionColor;">java_library</span><span style="">(</span>
      <br/>    <span style="color:$namedArgColor;">name</span> <span style="color:$namedArgColor;">=</span> <span style="color:$stringColor;font-weight:bold;">&quot;dep&quot;</span><span style="">,</span>
      <br/><span style="">)</span>
      </pre>
    """.singleLineHtml()
  }

  @Test
  fun `should return proper presentation for target label`() {
    configureBuildFileWithDepAndLib()

    val element = findLabelTarget(":dep")
    val presentation = getDocumentationTargets(element).singleOrNull()
      .shouldNotBeNull()
      .computePresentation()
    presentation.presentableText shouldBe "dep"
    presentation.icon shouldBe BazelPluginIcons.bazel
    presentation.locationText shouldBe "/src/BUILD"
    presentation.locationIcon shouldBe BazelPluginIcons.bazel
  }

  @Test
  fun `should not return documentation for non-target string`() {
    myFixture.configureByText(
      "BUILD",
      """
      java_library(
          name = "lib",
          srcs = ["Lib.java"],
      )
      """.trimIndent(),
    )

    val element = myFixture
      .file
      .descendantsOfType<StarlarkStringLiteralExpression>()
      .find { it.getStringContents() == "Lib.java" }
      .shouldNotBeNull()
    getDocumentationTargets(element)
      .singleOrNull()
      .shouldNotBeNull()
      .computeDocumentationHtml()
      .shouldBeNull()
  }

  @Test
  fun `should not return documentation for load`() {
    myFixture.configureByText(
      "BUILD",
      """
      load("@rules_java//java:defs.bzl", "java_library")
      """.trimIndent(),
    )

    val element = myFixture.file.descendantsOfType<StarlarkLoadStatement>()
      .firstOrNull()
      .shouldNotBeNull()
    getDocumentationTargets(element)
      .singleOrNull()
      .shouldNotBeNull()
      .computeDocumentationHtml()
      .shouldBeNull()
  }

  @Test
  fun `should not return documentation for variable`() {
    myFixture.configureByText(
      "BUILD",
      """
      x = 2
      """.trimIndent(),
    )

    val element = myFixture.file.descendantsOfType<StarlarkTargetExpression>()
      .singleOrNull()
      .shouldNotBeNull()
    getDocumentationTargets(element)
      .singleOrNull()
      .shouldNotBeNull()
      .computeDocumentationHtml()
      .shouldBeNull()
  }

  private fun getDocumentationTargets(element: PsiElement) = psiDocumentationTargets(element, element)

  private fun DocumentationTarget.computeDocumentationHtml(): String? {
    return computeDocumentationBlocking(createPointer())?.html
  }

  private fun findLabelTarget(label: String): PsiElement =
    myFixture.file
      .descendantsOfType<StarlarkStringLiteralExpression>()
      .first { it.getStringContents() == label }
      .reference
      ?.resolve()
      .shouldNotBeNull()

  private fun String.singleLineHtml(): String = trimIndent().replace("\n", "")

  private fun textAttributeColor(externalName: String): String {
    val key = TextAttributesKey.find(externalName)
    val color = EditorColorsManager
      .getInstance()
      .globalScheme
      .getAttributes(key)
      ?.foregroundColor
      ?: return ""
    return ColorUtil.toHtmlColor(color)
  }
}
