package org.jetbrains.bazel.languages.starlark.injection

import com.intellij.lang.Language
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.languages.starlark.fixtures.StarlarkPsiTestCase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

private const val SHELL_SCRIPT_LANGUAGE_ID = "Shell Script"

/**
 * BUILD file contents are written as Kotlin raw strings, which have no escape sequences of their own. Since `"""`
 * cannot appear in a Kotlin raw string, Starlark triple-quoted strings use `'''`.
 */
@RunWith(JUnit4::class)
class GenruleCommandShellInjectionTest : StarlarkPsiTestCase() {
  @Test
  fun `should have the shell script language available`() {
    Language.findLanguageByID(SHELL_SCRIPT_LANGUAGE_ID).shouldNotBeNull()
  }

  @Test
  fun `should inject shell script into a triple quoted genrule cmd`() {
    // given
    configureBuildFile(
      """
      genrule(
          name = "gen",
          srcs = ["in.txt"],
          outs = ["out.txt"],
          cmd = '''
      set -e
      cat $(location in.txt) > $@
      ''',
      )
      """,
    )

    // when
    val injected = injectedFiles("cmd")

    // then
    injected.size shouldBe 1
    injected.single().language.id shouldBe SHELL_SCRIPT_LANGUAGE_ID
    injected.single().text shouldBe "\nset -e\ncat $(location in.txt) > $@\n"
  }

  @Test
  fun `should inject decoded shell script into a single line genrule cmd_bash`() {
    // given
    configureBuildFile("""native.genrule(name = "gen", outs = ["out"], cmd_bash = "echo \"a\\tb\" > $@\n")""")

    // when
    val injected = injectedFiles("cmd_bash")

    // then
    injected.size shouldBe 1
    injected.single().language.id shouldBe SHELL_SCRIPT_LANGUAGE_ID
    injected.single().text shouldBe "echo \"a\\tb\" > $@\n"
  }

  @Test
  fun `should inject a single shell script into a parenthesized concatenation of string literals`() {
    // given
    configureBuildFile(
      """
      genrule(
          name = "gen",
          outs = ["out"],
          cmd = (
              "mkdir -p $(@D)/be && " +
              'echo a' +
              (" >> $(@D)/be/a") +
              ''' && zip -qj $@ $(@D)/be/*'''
          ),
      )
      """,
    )

    // when
    val literals = stringLiterals("cmd")

    // then
    literals.size shouldBe 4
    for (literal in literals) {
      val injected = injectedFiles(literal)
      injected.size shouldBe 1
      injected.single().language.id shouldBe SHELL_SCRIPT_LANGUAGE_ID
      injected.single().text shouldBe "mkdir -p $(@D)/be && echo a >> $(@D)/be/a && zip -qj $@ $(@D)/be/*"
    }
  }

  @Test
  fun `should inject placeholders for non-literal operands of a concatenation`() {
    // given
    configureBuildFile(
      """
      SRC = "in.txt"

      genrule(name = "gen", outs = ["out"], cmd = "cp " + SRC + " $@ && echo " + (SRC + "x") + " > " + SRC + SRC)
      """,
    )

    // when
    val injected = injectedFiles("cmd")

    // then
    injected.size shouldBe 1
    injected.single().text shouldBe "cp missing_value $@ && echo missing_valuex > missing_value"
  }

  @Test
  fun `should inject a single shell script into a joined list of string literals`() {
    // given
    configureBuildFile(
      """
      genrule(
          name = "gen",
          outs = ["out"],
          cmd = " && ".join([
              "mkdir -p $(@D)/out",
              # comment
              "echo a > " + "$(@D)/out/a",
              'cat $(@D)/out/a > $@',
          ]),
      )
      """,
    )

    // when
    val literals = stringLiterals("cmd")

    // then
    literals.size shouldBe 5
    literals.first().text shouldBe """" && """"
    injectedFiles(literals.first()).shouldBeEmpty()
    for (literal in literals.drop(1)) {
      val injected = injectedFiles(literal)
      injected.size shouldBe 1
      injected.single().text shouldBe "mkdir -p $(@D)/out && echo a > $(@D)/out/a && cat $(@D)/out/a > $@"
    }
  }

  @Test
  fun `should decode the join separator and inject placeholders for non-literal elements`() {
    // given
    configureBuildFile(
      """
      X = "echo x"

      genrule(name = "gen", outs = ["out"], cmd = "set -e\n" + "\n".join(["echo a", X, "echo b"]) + "\n")
      """,
    )

    // when
    val injected = injectedFiles("cmd")

    // then
    injected.size shouldBe 1
    injected.single().text shouldBe "set -e\necho a\nmissing_value\necho b\n"
  }

  @Test
  fun `should inject a placeholder for a join over a non-literal list`() {
    // given
    configureBuildFile(
      """
      CMDS = ["echo a"]

      genrule(name = "gen", outs = ["out"], cmd = "set -e && " + " && ".join(CMDS))
      """,
    )

    // when
    val injected = injectedFiles("cmd")

    // then
    injected.size shouldBe 1
    injected.single().text shouldBe "set -e && missing_value"
  }

  @Test
  fun `should not inject shell script into a concatenation without string literals`() {
    // given
    configureBuildFile(
      """
      A = "a"
      B = "b"

      genrule(name = "gen", outs = ["out"], cmd = (A + B))
      """,
    )

    // when & then
    PsiTreeUtil.findChildrenOfType(myFixture.file, StarlarkStringLiteralExpression::class.java).forEach {
      injectedFiles(it).shouldBeEmpty()
    }
  }

  @Test
  fun `should not inject shell script into non-shell genrule commands`() {
    // given
    configureBuildFile("""genrule(name = "gen", outs = ["out"], cmd_ps = "Write-Output a > $@", cmd_bat = "echo a > $@")""")

    // when & then
    injectedFiles("cmd_ps").shouldBeEmpty()
    injectedFiles("cmd_bat").shouldBeEmpty()
  }

  @Test
  fun `should not inject shell script into cmd arguments of other rules`() {
    // given
    configureBuildFile("""my_rule(name = "gen", cmd = "echo a")""")

    // when & then
    injectedFiles("cmd").shouldBeEmpty()
  }

  private fun configureBuildFile(contents: String) {
    myFixture.configureByText("BUILD", contents.trimIndent() + "\n")
  }

  private fun injectedFiles(argumentName: String): List<PsiFile> = injectedFiles(stringLiterals(argumentName).first())

  private fun injectedFiles(host: StarlarkStringLiteralExpression): List<PsiFile> =
    InjectedLanguageManager
      .getInstance(project)
      .getInjectedPsiFiles(host)
      .orEmpty()
      .map { it.first as PsiFile }

  /** All string literals in the value of the named argument, in source order. */
  private fun stringLiterals(argumentName: String): List<StarlarkStringLiteralExpression> {
    val argument =
      PsiTreeUtil
        .findChildrenOfType(myFixture.file, StarlarkNamedArgumentExpression::class.java)
        .single { it.name == argumentName }
    val literals = PsiTreeUtil.findChildrenOfType(argument, StarlarkStringLiteralExpression::class.java).toList()
    check(literals.isNotEmpty()) { "No string literal found for argument $argumentName" }
    return literals
  }
}
