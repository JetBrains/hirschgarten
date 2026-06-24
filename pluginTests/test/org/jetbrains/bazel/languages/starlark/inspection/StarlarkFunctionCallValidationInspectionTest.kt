package org.jetbrains.bazel.languages.starlark.inspection

import com.intellij.openapi.project.Project
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.Environment
import org.jetbrains.bazel.languages.starlark.bazel.StarlarkGlobalFunctionProvider
import org.jetbrains.bazel.project.BazelProjectFixtures.initializeBazelProject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StarlarkFunctionCallValidationInspectionTest : BasePlatformTestCase() {
  val positionalAfterKeywordOrStar = StarlarkBundle.message("inspection.description.call.positional.after.keyword.or.star")
  val argumentAfterKwargs = StarlarkBundle.message("inspection.description.call.argument.after.kwargs")
  val multipleKwargsUnpacking = StarlarkBundle.message("inspection.description.call.multiple.kwargs.unpacking")
  val starAfterKwargs = StarlarkBundle.message("inspection.description.call.star.after.kwargs")
  val tooManyPositional = StarlarkBundle.message("inspection.description.call.too.many.positional")
  val duplicateNamedArgOne = StarlarkBundle.message("inspection.description.call.duplicate.named.argument", "argOne")
  val multipleValuesArgTwo = StarlarkBundle.message("inspection.description.call.multiple.values.for.parameter", "argTwo")
  val missingRequiredArgOne = StarlarkBundle.message("inspection.description.call.missing.required.argument", "argOne")
  val missingRequiredArgTwo = StarlarkBundle.message("inspection.description.call.missing.required.argument", "argTwo")
  val parameterNotFound = StarlarkBundle.message("inspection.description.call.named.parameter.not.found", "unknownParam")
  val positionalOnly = StarlarkBundle.message("inspection.description.call.unnamed.arg.with.name", "argOne")
  val wrongEnvironment = StarlarkBundle.message("inspection.description.call.not.available.in.environment", "simple", "BZL", "MODULE")
  val undefined = StarlarkBundle.message("inspection.description.call.undefined")

  @Before
  fun beforeEach() {
    myFixture.enableInspections(StarlarkFunctionCallValidationInspection())
    ExtensionTestUtil.maskExtensions(
      StarlarkGlobalFunctionProvider.extensionPoint,
      listOf(TestStarlarkGlobalFunctionProvider()),
      myFixture.testRootDisposable,
    )
  }

  @Test
  fun `positional after named should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      <error descr="$missingRequiredArgTwo">f</error>(argOne = 1, <error descr="$positionalAfterKeywordOrStar">2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `positional after star unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      xs = [1]
      f(*xs, <error descr="$positionalAfterKeywordOrStar">2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `positional after kwargs unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      kw = {}
      f(**kw, <error descr="$positionalAfterKeywordOrStar">2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `named argument after kwargs unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne): 
          pass
      kw = {}
      f(**kw, <error descr="$argumentAfterKwargs">argOne = 1</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `star unpacking after kwargs unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne): 
          pass
      kw = {}
      xs = [1]
      f(**kw, <error descr="$starAfterKwargs">*xs</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `multiple kwargs unpacking should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne): 
          pass
      kw1 = {}
      kw2 = {}
      f(1, **kw1, <error descr="$multipleKwargsUnpacking">**kw2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `too many positional arguments should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      f(1, 2, <error descr="$tooManyPositional">3</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `too many positional arguments with variadic parameter should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo, *args): 
          pass
      f(1, 2, 3, 4, 5)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate named argument should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne): 
          pass
      f(argOne = 1, <error descr="$duplicateNamedArgOne">argOne = 2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `multiple values for the same parameter should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      f(1, 1, <error descr="$multipleValuesArgTwo">argTwo = 2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unknown named argument should not be highlighted when function has kwargs parameter`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(**kwargs): 
          pass
      f(b = 1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `missing required argument should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      <error descr="$missingRequiredArgTwo">f</error>(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `missing required positional argument should not be highlighted when star unpacking is present`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, argTwo): 
          pass
      xs = [1]
      f(1, *xs)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `missing required keyword-only argument should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, *, argTwo): 
          pass
      <error descr="$missingRequiredArgTwo">f</error>(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `missing required keyword-only argument should not be highlighted when kwargs unpacking is present`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(argOne, *, argTwo): 
          pass
      kw = {}
      f(1, **kw)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `fully correct call should not be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      def f(a, b = 1, *args, c, d = 2, **kwargs): 
          pass
      f(1, 2, 3, 4, c = 5, d = 6, e = 7)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `unresolved callee should not highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$undefined">unknown(1, 2, a = 3)</error>
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `too many positional arguments in lambda call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda argOne, argTwo: argOne
      f(1, 2, <error descr="$tooManyPositional">3</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate named argument in lambda call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda argOne: argOne
      f(argOne = 1, <error descr="$duplicateNamedArgOne">argOne = 2</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `missing required argument in lambda call should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      f = lambda argOne, argTwo: argOne
      <error descr="$missingRequiredArgTwo">f</error>(1)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `immediately invoked lambda should be validated`() {
    myFixture.configureByText(
      "test.bzl",
      """
      x = (lambda k: 2 * k)(2, <error descr="$tooManyPositional">4</error>)
      """.trimIndent()
    )
    myFixture.checkHighlighting(true, false, false)
  }

  @Test
  fun `duplicate named argument in global function call should be highlighted`() {
    val errorMessage = StarlarkBundle.message("inspection.description.call.duplicate.named.argument", "argTwo")
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "someValue", 
        argTwo = "someOtherValue",
        <error descr="$errorMessage">argTwo = "someOtherValue"</error>
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `unresolved argument in global function call should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simpleWithoutKwArgs(
        "val2", 
        "val2",
        <error descr="$parameterNotFound">unknownParam</error> = "whatever"
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `kwargs in global function call should not be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "val1",
        "val2",
        kwArgOne = "val3",
        kwArgTwo = "val4",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `positional argument after keyword argument in global function call should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      <error descr="$missingRequiredArgOne">simple</error>(
        argTwo = "someValue",
        <error descr="$positionalAfterKeywordOrStar">"someValue"</error>,
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `keyword argument overriding positional in global function call should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      simple(
        "val1",
        "val2",
        <error descr="$multipleValuesArgTwo">argTwo = "someNewName"</error>,
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `unnamed argument with a name in global function call should be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      <error descr="$missingRequiredArgOne">simple</error>(
        <error descr="$positionalOnly">argOne = "val1"</error>,
        argTwo = "val2",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `positional args with default values in global function call should not be skipped`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      optionalAndVarArgs(
          "varArg1",
          "varArg2",
          kwArg1 = "kwArg1Value",
          kwArg2 = "kwArg2Value",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `positional argument not not as a first in global function call should not be highlighted`() {
    myFixture.configureByText(
      "MODULE.bazel",
      """
      positionalNotFirst(
          "varArg2",
          argOne = "valueOne",
      )
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `global function call in wrong environment should be highlighted`() {
    myFixture.configureByText(
      "test.bzl",
      """
      <error descr="$wrongEnvironment">simple</error>(1, 1)
      """.trimIndent(),
    )
    myFixture.checkHighlighting()
  }

  @Test
  fun `custom macro pseudo collision should not be highlighted`() {
    initializeBazelProject(project, myFixture.tempDirPath)

    myFixture.addFileToProject(
      "def.bzl",
      """
        load("@rules_java//java:defs.bzl", _alias = "java_library")

        def java_library(
                my_custom_param = False,
                 **kwargs):
            #     my awesome logic based on 'my_custom_param' param value
            _alias(**kwargs)        
      """.trimIndent(),
    )
    myFixture.addFileToProject(
      "MODULE.bazel",
      """
        module(name = "null", version = "0.1.0")
        bazel_dep(name = "rules_java", version = "8.10.0")
      """.trimIndent(),
    )
    myFixture.configureByText(
      "BUILD",
      """
        load("//:def.bzl", "java_library")

        java_library(
            name = "example",
            srcs = ["example.kt"],
            my_custom_param = True,
            deps = [],
        )
      """.trimIndent(),
    )

    myFixture.checkHighlighting(true, false, false)
  }
}


private class TestStarlarkGlobalFunctionProvider : StarlarkGlobalFunctionProvider {
  override fun functions(project: Project): List<BazelGlobalFunction> = listOf(
      BazelGlobalFunction(
        name = "simple",
        doc = null,
        environment = listOf(Environment.MODULE),
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "*args",
              positional = true,
              named = false,
              required = false,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "**kwargs",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "simpleWithoutKwArgs",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "positionalNotFirst",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = false,
              named = true,
              required = true,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = true,
              named = false,
              required = true,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
      BazelGlobalFunction(
        name = "optionalAndVarArgs",
        doc = null,
        environment = Environment.entries,
        params =
          listOf(
            BazelGlobalFunctionParameter(
              name = "argOne",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = "default",
            ),
            BazelGlobalFunctionParameter(
              name = "*args",
              positional = true,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
            BazelGlobalFunctionParameter(
              name = "argTwo",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = "default",
            ),
            BazelGlobalFunctionParameter(
              name = "**kwargs",
              positional = false,
              named = true,
              required = false,
              doc = null,
              defaultValue = null,
            ),
          ),
      ),
    )
}
